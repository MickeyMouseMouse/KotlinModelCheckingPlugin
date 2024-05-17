package org.example.kotlinmodelcheckingplugin.verification

import NuXmvModelBuilder
import com.jetbrains.rd.generator.nova.PredefinedType
import org.example.kotlinmodelcheckingplugin.verification.variable.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine
import sootup.core.jimple.common.constant.BooleanConstant
import sootup.core.jimple.common.constant.IntConstant
import sootup.core.jimple.common.expr.Expr
import sootup.core.jimple.common.expr.JAddExpr
import sootup.core.jimple.common.ref.JInstanceFieldRef
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.jimple.common.stmt.JGotoStmt
import sootup.core.jimple.common.stmt.JIfStmt
import sootup.core.jimple.common.stmt.JInvokeStmt
import sootup.core.jimple.common.stmt.JReturnVoidStmt
import sootup.core.signatures.MethodSignature
import sootup.core.types.PrimitiveType
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.jimple.basic.JavaLocal
import sootup.java.core.views.JavaView
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class Analyzer(
    private var sourceCode: String,
    private var className: String,
    private var vars: MutableList<Variable>,
    private var ltlFormulas: List<String>,
    private var ctlFormulas: List<String>
) {
    /**
     * Execute the command in the OS command shell
     */
    private fun runCommand(cmd: Array<String>): Pair<Int, String> {
        val process = ProcessBuilder().redirectErrorStream(true).command(*cmd).start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val exitCode = process.exitValue()
        return Pair(exitCode, output)
    }

    /**
     * Check for dependencies in the system: Kotlin compiler, nuXmv model checker
     */
    fun checkDependencies(): String {
        val errors = mutableListOf<String>()
        try {
            runCommand(arrayOf("kotlinc.bat", "-version"))
        } catch (e: IOException) {
            errors.add("Kotlin compiler not found")
        }
        try {
            runCommand(arrayOf("nuxmv"))
        } catch (e: IOException) {
            errors.add("nuXmv not found")
        }

        if (errors.isEmpty()) {
            return ""
        } else {
            val msg = StringBuilder("ERROR: ")
            errors.forEach{
                msg.append(it).append("; ")
            }
            return msg.toString()
        }
    }

    /**
     * Compile the source code of the analyzed program for further processing with SootUp
     */
    private fun compileSourceCode(): Pair<Pair<Int, String>, File?> {
        val tmpDir = File(System.getProperty("java.io.tmpdir") + File.separator + "KotlinModelChecking")
        tmpDir.mkdir()

        val sourceCodeFile = File(tmpDir.absolutePath + File.separator + className + ".kt")
        sourceCodeFile.deleteOnExit()
        sourceCodeFile.printWriter().use { out -> out.print(sourceCode) }

        val jarFile = File(tmpDir.absolutePath + File.separator + "app.jar")

        val (exitCode: Int, output: String) = runCommand(
            arrayOf(
                "kotlinc.bat",  // Kotlin compiler
                sourceCodeFile.absolutePath,
                "-include-runtime", "-d", jarFile.absolutePath
            )
        )

        return if (exitCode == 0) {
            jarFile.deleteOnExit()
            Pair(Pair(exitCode, output), jarFile)
        } else {
            jarFile.delete()
            Pair(Pair(exitCode, output), null)
        }
    }

    /**
     * Run model checking tool (nuXmv)
     */
    private fun runModelChecker(modelCode: String): Pair<Int, String> {
        val tmpDir = File(System.getProperty("java.io.tmpdir") + File.separator + "KotlinModelChecking")
        tmpDir.mkdir()

        val modelCodeFile = File(tmpDir.absolutePath + File.separator + "model.smv")
        modelCodeFile.deleteOnExit()
        modelCodeFile.printWriter().use { out -> out.print(modelCode) }

        val (exitCode: Int, output: String) = runCommand(
            arrayOf("nuxmv", modelCodeFile.absolutePath)
        )

        return Pair(exitCode, output.substring(1138, output.length)) // first 1138 characters are the nuXmv preamble
    }

    /**
     *
     */
    private fun buildStateMachines(jarFile: File): Map<String, StateMachine> {
        val inputLocation = JavaClassPathAnalysisInputLocation(jarFile.absolutePath)
        val view = JavaView(inputLocation)
        val classType = view.identifierFactory.getClassType(className)
        val sootClass = view.getClass(classType).get()

        // get the initial values of variables from constructor
        for (method in sootClass.methods) {
            if (method.name == "<init>") {
                for (stmt in method.body.stmts) {
                    if (stmt is JAssignStmt) {
                        try {
                            val varName = stmt.fieldRef.fieldSignature.name // can throw RuntimeException
                            for (variable in vars) {
                                if (variable.name == varName) {
                                    when (variable.type) {
                                        VariableType.INT -> {
                                            variable.initValue.intValue = stmt.uses[1].toString().toInt()
                                        }
                                        VariableType.BOOL -> {
                                            variable.initValue.boolValue = stmt.uses[1].toString().toBoolean()
                                        }
                                        VariableType.UNKNOWN -> {}
                                    }
                                    break
                                }
                            }
                        } catch (_: RuntimeException) {
                        }
                    }
                }
            }
        }

        //
        val stateMachines = mutableMapOf<String, StateMachine>()
        for (variable in vars) {
            stateMachines[variable.name] = StateMachine(variable.initValue)
        }

        //
        fun calculateExpression(expr: Expr): Variable {
            val value = VariableValue()
            when (expr) {
                is JAddExpr -> {
                    expr.op1
                }
            }
            return Variable("result", VariableType.INT, value, value, false)
        }

        //
        fun analyseMethod(methodSignature: MethodSignature) {
            val method = view.getMethod(methodSignature).get()
            val localVars = mutableListOf<Variable>()
            var stmt = method.body.stmts[0]
            while (stmt !is JReturnVoidStmt) {
                when (stmt) {
                    is JAssignStmt -> {
                        val newValue = VariableValue()
                        when (stmt.rightOp) {
                            is IntConstant -> {
                                newValue.intValue = stmt.rightOp.toString().toInt()
                            }
                            is BooleanConstant -> {
                                newValue.boolValue = stmt.rightOp.toString().toBoolean()
                            }
                            is JInstanceFieldRef -> {
                                val varName = (stmt.rightOp as JInstanceFieldRef).fieldSignature.name
                                for (variable in vars) {
                                    if (variable.name == varName) {
                                        when (variable.type) {
                                            VariableType.INT -> newValue.intValue = variable.value.intValue
                                            VariableType.BOOL -> newValue.boolValue = variable.value.boolValue
                                            VariableType.UNKNOWN -> {}
                                        }
                                        break
                                    }
                                }
                            }
                            is JavaLocal -> {
                                val varName = (stmt.rightOp as JavaLocal).name
                                for (variable in localVars) {
                                    if (variable.name == varName) {
                                        when (variable.type) {
                                            VariableType.INT -> newValue.intValue = variable.value.intValue
                                            VariableType.BOOL -> newValue.boolValue = variable.value.boolValue
                                            VariableType.UNKNOWN -> {}
                                        }
                                        break
                                    }
                                }
                            }
                            is Expr -> {
                                val variable = calculateExpression(stmt.rightOp as Expr)
                                when (variable.type) {
                                    VariableType.INT -> newValue.intValue = variable.value.intValue
                                    VariableType.BOOL -> newValue.boolValue = variable.value.boolValue
                                    VariableType.UNKNOWN -> {}
                                }
                            }
                        }

                        when (stmt.leftOp) {
                            is JInstanceFieldRef -> {
                                val varSignature = (stmt.leftOp as JInstanceFieldRef).fieldSignature
                                for (variable in vars) {
                                    if (variable.name == varSignature.name) {
                                        when (varSignature.type) {
                                            PrimitiveType.getInt() -> {
                                                variable.value.intValue = newValue.intValue
                                                stateMachines[variable.name]?.add(
                                                    VariableValue(intValue = variable.value.intValue)
                                                )
                                            }
                                            PrimitiveType.getBoolean() -> {
                                                variable.value.boolValue = newValue.boolValue
                                                stateMachines[variable.name]?.add(
                                                    VariableValue(boolValue = variable.value.boolValue)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is JavaLocal -> {
                                val type = when (stmt.rightOp.type) {
                                    PrimitiveType.getInt() -> VariableType.INT
                                    PrimitiveType.getBoolean() -> VariableType.BOOL
                                    else -> VariableType.UNKNOWN
                                }
                                val value = when (stmt.rightOp.type) {
                                    PrimitiveType.getInt() -> VariableValue(intValue=newValue.intValue)
                                    PrimitiveType.getBoolean() -> VariableValue(boolValue = newValue.boolValue)
                                    else -> VariableValue()
                                }
                                val varName = (stmt.leftOp as JavaLocal).name
                                var exists = false
                                for (variable in localVars) {
                                    if (variable.name == varName) {
                                        exists = true
                                        variable.value = value
                                        break
                                    }
                                }
                                if (!exists) localVars.add(
                                    Variable(varName, type, value, value, false)
                                )
                            }
                        }
                    }
                    is JInvokeStmt -> {
                        analyseMethod(stmt.invokeExpr.methodSignature)
                    }
                }

                stmt = when (stmt) {
                    is JIfStmt -> {
                        method.body.stmts[method.body.stmts.indexOf(stmt) + 1] //
                        //stmt.getTargetStmts(method.body)[0]
                    }
                    is JGotoStmt -> {
                        stmt.getTargetStmts(method.body)[0]
                    }
                    else -> { // JAssignStmt, JInvokeStmt and so on
                        method.body.stmts[method.body.stmts.indexOf(stmt) + 1]
                    }
                }
            }
        }

        // start the analysis with the 'main()' function (entry point)
        analyseMethod(view.identifierFactory.getMethodSignature(classType, "main", "void", listOf()))

        return stateMachines
    }

    /**
     *
     */
    fun start(): String {
        val (result: Pair<Int, String>, jarFile: File?) = compileSourceCode()
        if (jarFile == null) {
            return "Compilation failed: " + result.second
        }

        val stateMachines = buildStateMachines(jarFile)

        return NuXmvModelBuilder(
            vars, stateMachines, ltlFormulas, ctlFormulas
        ).getModel()

        //return runModelChecker(model)

        //return "mock"
    }
}
package org.example.kotlinmodelcheckingplugin.verification

import NuXmvModelBuilder
import com.googlecode.dex2jar.ir.expr.CastExpr
import com.jetbrains.rd.generator.nova.PredefinedType
import org.example.kotlinmodelcheckingplugin.verification.variable.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.constant.BooleanConstant
import sootup.core.jimple.common.constant.DoubleConstant
import sootup.core.jimple.common.constant.IntConstant
import sootup.core.jimple.common.expr.*
import sootup.core.jimple.common.ref.JInstanceFieldRef
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.jimple.common.stmt.JGotoStmt
import sootup.core.jimple.common.stmt.JIfStmt
import sootup.core.jimple.common.stmt.JInvokeStmt
import sootup.core.jimple.common.stmt.JReturnVoidStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt
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
     * Analyse Jimple IR to retrieve finite state machines for variables
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
                                        VariableType.DOUBLE -> {
                                            variable.initValue.doubleValue = stmt.uses[1].toString().toDouble()
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

        // create finite state machines with initial states
        val stateMachines = mutableMapOf<String, StateMachine>()
        for (variable in vars) {
            stateMachines[variable.name] = StateMachine(variable.initValue)
        }

        //
        fun calculate(value: Value, localVars: List<Variable>): Variable {
            lateinit var result : Variable
            when (value) {
                is IntConstant -> {
                    result = Variable(
                        "result",
                        VariableType.INT,
                        VariableValue(),
                        VariableValue(intValue = value.toString().toInt()),
                        false
                    )
                }
                is DoubleConstant -> {
                    result = Variable(
                        "result",
                        VariableType.DOUBLE,
                        VariableValue(),
                        VariableValue(doubleValue = value.toString().toDouble()),
                        false
                    )
                }
                is BooleanConstant -> {
                    result = Variable(
                        "result",
                        VariableType.BOOL,
                        VariableValue(),
                        VariableValue(boolValue = value.toString().toBoolean()),
                        false
                    )
                }
                is JInstanceFieldRef -> {
                    for (variable in vars) {
                        if (variable.name == value.fieldSignature.name) {
                            when (variable.type) {
                                VariableType.INT -> {
                                    result = Variable(
                                        "result",
                                        VariableType.INT,
                                        VariableValue(),
                                        VariableValue(intValue = variable.value.intValue),
                                        false
                                    )
                                }
                                VariableType.DOUBLE -> {
                                    result = Variable(
                                        "result",
                                        VariableType.DOUBLE,
                                        VariableValue(),
                                        VariableValue(doubleValue = variable.value.doubleValue),
                                        false
                                    )
                                }
                                VariableType.BOOL -> {
                                    result = Variable(
                                        "result",
                                        VariableType.BOOL,
                                        VariableValue(),
                                        VariableValue(boolValue = variable.value.boolValue),
                                        false
                                    )
                                }
                                VariableType.UNKNOWN -> {}
                            }
                            break
                        }
                    }
                }
                is JavaLocal -> {
                    for (variable in localVars) {
                        if (variable.name == value.name) {
                            when (variable.type) {
                                VariableType.INT -> {
                                    result = Variable(
                                        "result",
                                        VariableType.INT,
                                        VariableValue(),
                                        VariableValue(intValue = variable.value.intValue),
                                        false
                                    )
                                }
                                VariableType.DOUBLE -> {
                                    result = Variable(
                                        "result",
                                        VariableType.DOUBLE,
                                        VariableValue(),
                                        VariableValue(doubleValue = variable.value.doubleValue),
                                        false
                                    )
                                }
                                VariableType.BOOL -> {
                                    result = Variable(
                                        "result",
                                        VariableType.BOOL,
                                        VariableValue(),
                                        VariableValue(boolValue = variable.value.boolValue),
                                        false
                                    )
                                }
                                VariableType.UNKNOWN -> {}
                            }
                            break
                        }
                    }
                }
                is JAddExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.INT,
                            VariableValue(),
                            VariableValue(intValue = (op1.getValue() as Int) + (op2.getValue() as Int)),
                            false
                        )
                    }
                }
                is JMulExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.INT,
                            VariableValue(),
                            VariableValue(intValue = (op1.getValue() as Int) * (op2.getValue() as Int)),
                            false
                        )
                    }
                }
                is JDivExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.INT,
                            VariableValue(),
                            VariableValue(intValue = (op1.getValue() as Int) / (op2.getValue() as Int)),
                            false
                        )
                    }
                    if (op1.type == VariableType.DOUBLE || op2.type == VariableType.DOUBLE) {
                        result = Variable(
                            "result",
                            VariableType.DOUBLE,
                            VariableValue(),
                            VariableValue(doubleValue = (op1.getValue() as Double) / (op2.getValue() as Double)),
                            false
                        )
                    }
                }
                is JRemExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.INT,
                            VariableValue(),
                            VariableValue(intValue = (op1.getValue() as Int) / (op2.getValue() as Int)),
                            false
                        )
                    }
                }
                is JGeExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.BOOL,
                            VariableValue(),
                            VariableValue(boolValue = (op1.getValue() as Int) >= (op2.getValue() as Int)),
                            false
                        )
                    }
                }
                is JEqExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.BOOL,
                            VariableValue(),
                            VariableValue(boolValue = (op1.getValue() as Int) == (op2.getValue() as Int)),
                            false
                        )
                    }
                    if (op1.type == VariableType.BOOL && op2.type == VariableType.BOOL) {
                        result = Variable(
                            "result",
                            VariableType.BOOL,
                            VariableValue(),
                            VariableValue(boolValue = (op1.getValue() as Boolean) == (op2.getValue() as Boolean)),
                            false
                        )
                    }
                }
                is JNeExpr -> {
                    val op1 = calculate(value.op1, localVars)
                    val op2 = calculate(value.op2, localVars)
                    if (op1.type == VariableType.INT && op2.type == VariableType.INT) {
                        result = Variable(
                            "result",
                            VariableType.BOOL,
                            VariableValue(),
                            VariableValue(boolValue = (op1.getValue() as Int) != (op2.getValue() as Int)),
                            false
                        )
                    }
                    if (op1.type == VariableType.BOOL && op2.type == VariableType.BOOL) {
                        result = Variable(
                            "result",
                            VariableType.BOOL,
                            VariableValue(),
                            VariableValue(boolValue = (op1.getValue() as Boolean) != (op2.getValue() as Boolean)),
                            false
                        )
                    }
                }
                is JCastExpr -> {
                    val op = calculate(value.op, localVars)
                    when (value.type) {
                        PrimitiveType.getDouble() -> {
                            when (op.type) {
                                VariableType.INT -> {
                                    result = Variable(
                                        "result",
                                        VariableType.DOUBLE,
                                        VariableValue(),
                                        VariableValue(doubleValue = (op.getValue() as Int).toDouble()),
                                        false
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
            return result
        }

        //
        fun analyseMethod(methodSignature: MethodSignature) {
            val method = view.getMethod(methodSignature).get()
            val localVars = mutableListOf<Variable>()
            var stmt = method.body.stmts[0]
            while (stmt !is JReturnVoidStmt) {
                when (stmt) {
                    is JAssignStmt -> {
                        val result = calculate(stmt.rightOp, localVars)
                        when (stmt.leftOp) {
                            is JInstanceFieldRef -> {
                                val varSignature = (stmt.leftOp as JInstanceFieldRef).fieldSignature
                                for (variable in vars) {
                                    if (variable.name == varSignature.name) {
                                        when (varSignature.type) {
                                            PrimitiveType.getInt() -> {
                                                variable.value.intValue = result.getValue() as Int
                                                stateMachines[variable.name]?.add(
                                                    VariableValue(intValue = variable.value.intValue)
                                                )
                                            }
                                            PrimitiveType.getBoolean() -> {
                                                variable.value.boolValue = (result.getValue() as Int) == 1
                                                stateMachines[variable.name]?.add(
                                                    VariableValue(boolValue = variable.value.boolValue)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is JavaLocal -> {
                                var exists = false
                                for (variable in localVars) {
                                    if (variable.name == (stmt.leftOp as JavaLocal).name) {
                                        exists = true
                                        variable.value = result.value
                                        break
                                    }
                                }
                                if (!exists) localVars.add(
                                    Variable(
                                        (stmt.leftOp as JavaLocal).name,
                                        result.type,
                                        VariableValue(),
                                        result.value,
                                        false
                                    )
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
                        if  (calculate(stmt.condition, localVars).getValue() as Boolean)
                            stmt.getTargetStmts(method.body)[0]
                        else
                            method.body.stmts[method.body.stmts.indexOf(stmt) + 1]
                    }
                    is JGotoStmt -> {
                        stmt.getTargetStmts(method.body)[0]
                    }
                    is JSwitchStmt -> {
                        lateinit var nextStmt: Stmt
                        for (variable in localVars) {
                            if (variable.name == (stmt.key as JavaLocal).name) {
                                when (variable.type) {
                                    VariableType.INT -> {
                                        for (i in 0..<stmt.values.size) {
                                            if ((variable.getValue() as Int) == stmt.values[i].value) {
                                                nextStmt = stmt.getTargetStmts(method.body)[i]
                                                break
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                        nextStmt
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
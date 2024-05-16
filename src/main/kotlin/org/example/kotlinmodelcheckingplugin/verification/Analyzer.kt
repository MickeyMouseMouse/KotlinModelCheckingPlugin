package org.example.kotlinmodelcheckingplugin.verification

import NuXmvModelBuilder
import org.example.kotlinmodelcheckingplugin.verification.dataclasses.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.jimple.common.stmt.JInvokeStmt
import sootup.core.signatures.MethodSignature
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.JavaSootMethod
import sootup.java.core.views.JavaView
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class Analyzer(
    private var sourceCode: String,
    private var className: String,
    private var entryPoint: String,
    private var vars: MutableList<VarInfo>,
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

        //
        val constructor = view.getMethod(
            view.identifierFactory.getMethodSignature(
                classType, "<init>", "void", listOf()
            )
        ).get()
        for (stmt in constructor.body.stmts) {
            if (stmt is JAssignStmt) { // get the initial values of variables
                try {
                    val varName = stmt.fieldRef.fieldSignature.name // can throw RuntimeException
                    for (variable in vars) {
                        if (variable.name == varName) {
                            variable.initValue = stmt.uses[1].toString()
                            break
                        }
                    }
                } catch (_: RuntimeException) {}
            }
        }

        //
        val stateMachines = mutableMapOf<String, StateMachine>()
        for (variable in vars) {
            stateMachines[variable.name] = StateMachine(variable.initValue)
        }

        //
        fun analyse(methodSignature: MethodSignature) {
            val method = view.getMethod(methodSignature).get()
            for (stmt in method.body.stmts) {
                if (stmt is JInvokeStmt) {
                    analyse(stmt.invokeExpr.methodSignature)
                }
            }
        }

        // start the analysis with the 'main()' function (entry point)
        analyse(view.identifierFactory.getMethodSignature(classType, "main", "void", listOf()))

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
            className, vars, stateMachines, ltlFormulas, ctlFormulas
        ).getModel()

        //return runModelChecker(model)
    }
}
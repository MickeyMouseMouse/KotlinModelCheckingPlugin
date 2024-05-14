package org.example.kotlinmodelcheckingplugin.verification

import NuXmvModelBuilder
import org.example.kotlinmodelcheckingplugin.verification.annotation_dataclasses.*
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class Analyzer(
    private val className: String?,
    private val sourceCode: String,
    private val stateVars: List<StateVarInfo>,
    private val ltlFormulas: List<LTLFormula>,
    private val ctlFormulas: List<CTLFormula>
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
    fun checkDependencies(): Pair<Boolean,String> {
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
            return Pair(true, "")
        } else {
            val msg = StringBuilder("ERROR: ")
            errors.forEach{
                msg.append(it).append("; ")
            }
            return Pair(false, msg.toString())
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
    private fun analyse(jarFile: File) {
        val inputLocation = JavaClassPathAnalysisInputLocation(jarFile.absolutePath)
        val view = JavaView(inputLocation)
        val classType = view.identifierFactory.getClassType(className)
        val sootClass = view.getClass(classType).get()

        for (method in sootClass.methods) {
            if (method.name == "<init>") { // constructor
                for (stmt in method.body.stmts) {
                    if (stmt is JAssignStmt) { // get the initial values of state variables
                        try {
                            val varName = stmt.fieldRef.fieldSignature.name  // can throw RuntimeException
                            for (stateVar in stateVars) {
                                if (stateVar.name == varName) {
                                    stateVar.initValue = stmt.uses[1].toString()
                                }
                            }
                        } catch (_: RuntimeException) {}
                    }
                }
            }

            // TODO
        }

        print("")
        /*
        val methodSignature = view.identifierFactory.getMethodSignature(
            classType,
            "<init>", // main
            "void", // void
            listOf() // listOf("java.lang.String[]")) listOf()
        )
        val opt = view.getMethod(methodSignature)
        val method = opt.get()

        val stmtGraph = method.body.stmts
         */
    }

    /**
     * Perform model checking
     */
    fun start(): String {
        val (result: Pair<Int, String>, jarFile: File?) = compileSourceCode()
        if (jarFile == null) {
            return "Compilation failed: " + result.second
        }

        analyse(jarFile)

        return NuXmvModelBuilder(
            stateVars, ltlFormulas, ctlFormulas
        ).getModel()

        //return runModelChecker(model)

        //return "mock"
    }
}
package org.example.kotlinmodelcheckingplugin.model

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class Tools {
    companion object {
        /**
         * Execute the command in the OS command shell
         */
        private fun exec(cmd: Array<String>): Pair<Int, String> {
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
                exec(arrayOf("kotlinc.bat", "-version"))
            } catch (e: IOException) {
                errors.add("Kotlin compiler not found")
            }
            try {
                exec(arrayOf("nuxmv"))
            } catch (e: IOException) {
                errors.add("nuXmv not found")
            }

            if (errors.isEmpty()) {
                return ""
            } else {
                val msg = StringBuilder("ERROR: ")
                errors.forEach {
                    msg.append(it).append("; ")
                }
                return msg.toString()
            }
        }

        /**
         * Compile the source code of the analyzed program for further processing with SootUp
         */
        fun compile(className: String, sourceCode: String): Pair<Pair<Int, String>, File?> {
            val tmpDir = File(System.getProperty("java.io.tmpdir") + File.separator + "KotlinModelChecking")
            tmpDir.mkdir()

            val sourceCodeFile = File(tmpDir.absolutePath + File.separator + className + ".kt")
            sourceCodeFile.deleteOnExit()
            sourceCodeFile.printWriter().use { out -> out.print(sourceCode) }

            val jarFile = File(tmpDir.absolutePath + File.separator + "${className}.jar")

            val (exitCode: Int, output: String) = exec(
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
        fun runModelChecker(className: String, modelCode: String): Pair<Int, String> {
            val tmpDir = File(System.getProperty("java.io.tmpdir") + File.separator + "KotlinModelChecking")
            tmpDir.mkdir()

            val modelCodeFile = File(tmpDir.absolutePath + File.separator + "${className}_model.smv")
            modelCodeFile.deleteOnExit()
            modelCodeFile.printWriter().use { out -> out.print(modelCode) }

            val (exitCode: Int, output: String) = exec(
                arrayOf("nuxmv", modelCodeFile.absolutePath)
            )

            return Pair(
                exitCode,
                output.substring( // skip the first few lines with the nuXmv preamble
                    1138, // output.indexOf("-- specification")
                    output.length
                )
            )
        }
    }
}

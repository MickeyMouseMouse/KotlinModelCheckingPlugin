package org.example.kotlinmodelcheckingplugin.controller

import com.intellij.openapi.application.ApplicationManager
import org.example.kotlinmodelcheckingplugin.model.Analyzer
import org.example.kotlinmodelcheckingplugin.model.NuXmvModelBuilder
import org.example.kotlinmodelcheckingplugin.model.Tools
import org.example.kotlinmodelcheckingplugin.model.stmt_value.Constant
import org.example.kotlinmodelcheckingplugin.model.stmt_value.Variable
import javax.swing.JTextArea
import javax.swing.JProgressBar

class Controller(
    val sourceCode: String,
    val className: String,
    val stateVariables: List<Variable>,
    val variables: List<Variable>,
    val constants: List<Constant>,
    val ltlFormulas: List<String>,
    val ctlFormulas: List<String>
) {
    val resultTextArea = JTextArea("Click Start to perform model checking")

    val progressBar = JProgressBar()

    fun start() {
        /*
        // check the necessary system dependencies (additional)
        val check = Tools.checkDependencies()
        if (check.isNotEmpty()) {
            resultTextArea.text = check
            return
        }
        */

        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                resultTextArea.text = "Compiling the application... "
                progressBar.value = 0
                val (result: Pair<Int, String>, jarFile: java.io.File?) = Tools.compile(className, sourceCode)
                if (jarFile == null) {
                    resultTextArea.text = "Compilation failed: " + result.second
                } else {
                    resultTextArea.text += "Done\nAnalyzing... "
                    progressBar.value = 10

                    val allVariables = stateVariables + variables
                    val stateMachines = Analyzer(jarFile, className, allVariables, constants).buildStateMachines()

                    resultTextArea.text += "Done\nBuilding a program model... "
                    progressBar.value = 60

                    val model = NuXmvModelBuilder(
                        allVariables,
                        stateMachines,
                        ltlFormulas,
                        ctlFormulas
                    ).getModel()

                    resultTextArea.text += "Done\nRunning a model checking... "
                    progressBar.value = 80

                    resultTextArea.text = model + Tools.runModelChecker(className, model).second
                    progressBar.value = 100
                }
            }
        }
    }
}

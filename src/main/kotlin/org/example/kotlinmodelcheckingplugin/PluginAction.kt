package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.verification.Analyzer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import org.example.kotlinmodelcheckingplugin.verification.stmt_value.*
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.psi.KtClass
import javax.swing.Action
import javax.swing.JComponent


/*
Guides:
    https://plugins.jetbrains.com/docs/intellij/creating-plugin-project.html
    https://www.baeldung.com/intellij-idea-plugins-gradle

Plugin config file: src/resources/META-INF/plugin.xml
    Action:
        text: "Model Checking..."
        group-id: "AnalyzeActions"
        anchor: "first"

Build plugin:
    Gradle menu->Tasks->intellij->buildPlugin
    Result: build/distributions/plugin.zip
*/

class PluginAction: DumbAwareAction() {
    @ApiStatus.Internal
    data class ModelData(
        var stateVariables: String,
        var ltlFormulas: String,
        var ctlFormulas: String,
        var output: String
    )

    private lateinit var modelData: ModelData

    private lateinit var analyzer: Analyzer

    /**
     * Action on the pop-up menu event
     */
    override fun actionPerformed(e: AnActionEvent) {
        e.project ?: return

        val selectedFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        if (selectedFile.isDirectory || !selectedFile.name.matches(Regex("(.)+(.kt)$"))) {
            getErrorDialog("Select a Kotlin source file").show()
            return
        }

        val document = FileDocumentManager.getInstance().getDocument(selectedFile) ?: return
        FileDocumentManager.getInstance().saveDocument(document)
        if (document.text.isEmpty()) {
            getErrorDialog("Empty source file").show()
            return
        }

        val ktClass = e.getData(PSI_ELEMENT) as KtClass

        // retrieve variables and constants from source code
        val variables = mutableListOf<Variable>()
        val constants = mutableListOf<Constant>()
        for (property in ktClass.getProperties()) {
            val propertyType = (property.getReturnTypeReference()?.getTypeText() ?: "unknown").lowercase()
            val initValueString = property.initializer?.text
            if (property.isVar) {
                lateinit var newVar: Variable
                when (propertyType) {
                    "int" -> newVar = Variable(
                        property.nameAsSafeName.asString(),
                        StmtValue(type = StmtType.INT, intValue = initValueString?.toInt()),
                        StmtValue(type = StmtType.INT, intValue = initValueString?.toInt()),
                        false
                    )

                    "boolean" -> newVar = Variable(
                        property.nameAsSafeName.asString(),
                        StmtValue(type = StmtType.BOOL, boolValue = initValueString?.toBoolean()),
                        StmtValue(type = StmtType.BOOL, boolValue = initValueString?.toBoolean()),
                        false
                    )
                }

                for (annotation in property.annotationEntries) {
                    if (annotation.shortName?.asString() == "StateVar") {
                        newVar.isState = true
                        break
                    }
                }

                variables.add(newVar)
            } else { // const
                lateinit var newConst: Constant
                when (propertyType) {
                    "int" -> newConst = Constant(
                        property.nameAsSafeName.asString(),
                        StmtValue(type = StmtType.INT, intValue = initValueString?.toInt())
                    )

                    "boolean" -> newConst = Constant(
                        property.nameAsSafeName.asString(),
                        StmtValue(type = StmtType.BOOL, boolValue = initValueString?.toBoolean())
                    )
                }
                constants.add(newConst)
            }
        }

        // retrieve LTL and CTL formulas from annotations in the source code
        val ltlFormulas = mutableListOf<String>()
        val ctlFormulas = mutableListOf<String>()
        for (annotation in ktClass.annotationEntries) {
            when (annotation.shortName?.asString()) {
                "LTL" -> {
                    val args = annotation.valueArgumentList?.arguments
                    if (args != null) {
                        val arg = args[0].text
                        ltlFormulas.add(arg.substring(1, arg.length - 1))
                    }
                }
                "CTL" -> {
                    val args = annotation.valueArgumentList?.arguments
                    if (args != null) {
                        val arg = args[0].text
                        ctlFormulas.add(arg.substring(1, arg.length - 1))
                    }
                }
            }
        }

        modelData = ModelData(
            variables.filter { it.isState }.joinToString("\n") { it.name },
            ltlFormulas.joinToString("\n") { it },
            ctlFormulas.joinToString("\n") { it },
            ""
        )

        // remove the plugin annotations from the source code
        val sourceCode = ktClass.text  // Full code: document.text
            .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
            .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

        analyzer = Analyzer(
            sourceCode,
            ktClass.name.toString(),
            variables,
            constants,
            ltlFormulas,
            ctlFormulas
        )

        /*
        // check the necessary system dependencies (additional)
        val check = analyzer.checkDependencies()
        if (check.isNotEmpty()) {
            getErrorDialog(check).show()
            return
        }
        */

        getModelCheckingDialog().show()
    }

    private fun getErrorDialog(msg: String): DialogWrapper {
        return object: DialogWrapper(true) {
            init {
                title = "Error"
                init()
            }

            override fun createActions(): Array<Action> { // bottom buttons
                return arrayOf(okAction)
            }

            override fun createCenterPanel(): JComponent { // gui
                return panel {
                    row {
                        label(msg)
                    }
                }
            }
        }
    }

    private fun getModelCheckingDialog(): DialogWrapper {
        return object: DialogWrapper(true) {
            init {
                title = "Model Checking"
                init()
            }

            override fun createActions(): Array<Action> { // bottom buttons
                return arrayOf() // (okAction, cancelAction)
            }

            override fun createCenterPanel(): JComponent { // gui
                lateinit var panel: DialogPanel
                panel = panel {
                    row("State variables") {
                        textArea().bindText(modelData::stateVariables).rows(3).columns(50)
                    }
                    row("LTL formulas") {
                        textArea().bindText(modelData::ltlFormulas).rows(3).columns(50)
                    }
                    row("CTL formulas") {
                        textArea().bindText(modelData::ctlFormulas).rows(3).columns(50)
                    }
                    row("Result") {
                        textArea().bindText(modelData::output).rows(12).columns(50)
                    }
                    row {
                        button("Start") {
                            if (analyzer.start()) {
                                modelData.output = analyzer.modelCheckingResult
                            } else {
                                modelData.output = analyzer.error
                            }
                            panel.reset()

                            // https://stackoverflow.com/questions/18725340/create-a-background-task-in-intellij-plugin
                            /*
                            ApplicationManager.getApplication().executeOnPooledThread {
                                ApplicationManager.getApplication().runReadAction {
                                    modelData.output = analyzer.start()
                                    panel.reset()
                                }
                            }
                            */

                            /*
                            ProgressManager.getInstance().run(object: Task.Backgroundable(modelData.project, "Model checking...") {
                                override fun run(@NotNull progressIndicator: ProgressIndicator) {
                                    modelData.output = analyzer.start()
                                    panel.reset()
                                }
                            })
                            */
                        }.enabled(
                            modelData.stateVariables.isNotEmpty() and
                                    (modelData.ltlFormulas.isNotEmpty() or modelData.ctlFormulas.isNotEmpty())
                        )
                        button("Cancel") {
                            doCancelAction()
                        }
                    }
                }
                return panel
            }
        }
    }
}
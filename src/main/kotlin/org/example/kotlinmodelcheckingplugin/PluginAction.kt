package org.example.kotlinmodelcheckingplugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import org.example.kotlinmodelcheckingplugin.controller.Controller
import org.example.kotlinmodelcheckingplugin.model.stmt_value.*
import org.example.kotlinmodelcheckingplugin.view.View
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
        val stateVariables = mutableListOf<Variable>()
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
                    "string" -> {
                        val stringValue = initValueString!!.substring(1, initValueString.length - 1)
                        newVar = Variable(
                            property.nameAsSafeName.asString(),
                            StmtValue(type = StmtType.STRING, stringValue = stringValue),
                            StmtValue(type = StmtType.STRING, stringValue = stringValue),
                            false
                        )
                    }
                }

                for (annotation in property.annotationEntries) {
                    if (annotation.shortName?.asString() == "StateVar") {
                        newVar.isState = true
                        break
                    }
                }

                if (newVar.isState) {
                    stateVariables.add(newVar)
                } else {
                    variables.add(newVar)
                }
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
                    "string" -> {
                        val stringValue = initValueString!!.substring(1, initValueString.length - 1)
                        newConst = Constant(
                            property.nameAsSafeName.asString(),
                            StmtValue(type = StmtType.STRING, stringValue = stringValue)
                        )
                    }
                }
                constants.add(newConst)
            }
        }

        if (stateVariables.isEmpty()) {
            getErrorDialog("No state variables are specified (use @StateVar annotation)").show()
            return
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

        if (ltlFormulas.isEmpty() && ctlFormulas.isEmpty()) {
            getErrorDialog("No temporal formulas are specified (use @LTL and @CTL annotations)").show()
            return
        }

        // remove the plugin annotations from the source code
        val sourceCode = ktClass.text  // Full code: document.text
            .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
            .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

        val ctrl = Controller(
            sourceCode,
            ktClass.name.toString(),
            stateVariables,
            variables,
            constants,
            ltlFormulas,
            ctlFormulas
        )

        val gui = View(ctrl)
        gui.isVisible = true
    }

    private fun getErrorDialog(msg: String): DialogWrapper {
        return object: DialogWrapper(true) {
            init {
                title = "Error"
                init()
            }

            override fun createActions(): Array<Action> { // bottom action buttons
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
}

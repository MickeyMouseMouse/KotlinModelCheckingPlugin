package org.example.kotlinmodelcheckingplugin.view

import com.intellij.util.ui.JBUI
import javax.swing.JFrame
import javax.swing.JPanel
import com.intellij.ui.table.JBTable
import com.intellij.ui.components.JBScrollPane
import org.example.kotlinmodelcheckingplugin.controller.Controller
import java.awt.*
import javax.swing.JButton
import javax.swing.table.DefaultTableModel

class View(
    private val ctrl: Controller
): JFrame() {
    init {
        createUI(
            ctrl.stateVariables.map { it.name },
            ctrl.variables.map { it.name },
            ctrl.constants.map { it.name },
            ctrl.ltlFormulas,
            ctrl.ctlFormulas
        )
    }

    private fun createUI(
        stateVariables: List<String>,
        variables: List<String>,
        constants: List<String>,
        ltlFormulas: List<String>,
        ctlFormulas: List<String>
    ) {
        this.title = "${ctrl.className} - Model Checking"
        this.setSize(650, 600)
        setLocationRelativeTo(null)

        val rootPanel = JPanel()
        rootPanel.border = JBUI.Borders.empty(10, 20)
        val rootLayout = GridBagLayout()
        rootLayout.columnWeights = doubleArrayOf(1.0)
        rootLayout.rowWeights = doubleArrayOf(
            1.0, // vars
            1.0, // ltl
            1.0, // ctl
            3.0, // model checking result
            0.5, // progress bar
            1.0 // action buttons
        )
        val constraints = GridBagConstraints()
        constraints.insets = JBUI.insetsBottom(10)
        constraints.fill = GridBagConstraints.BOTH
        rootPanel.layout = rootLayout

        class CustomTableModel: DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }

        val varsTableModel = CustomTableModel()
        varsTableModel.setColumnIdentifiers(arrayOf("State variables", "Variables", "Constants"))
        val rowNumber = listOf(stateVariables.size, variables.size, constants.size).maxOrNull() ?: 0
        for (i in 0..<rowNumber) {
            varsTableModel.addRow(arrayOf(
                stateVariables.getOrNull(i) ?: "",
                variables.getOrNull(i) ?: "",
                constants.getOrNull(i) ?: ""
            ))
        }
        val varsTable = JBTable(varsTableModel)
        val varsScrollPane = JBScrollPane(varsTable)
        constraints.gridx = 0
        constraints.gridy = 0
        rootPanel.add(varsScrollPane, constraints)

        val ltlTableModel = CustomTableModel()
        ltlTableModel.setColumnIdentifiers(arrayOf("LTL"))
        for (i in ltlFormulas.indices) {
            ltlTableModel.addRow(arrayOf(ltlFormulas.getOrNull(i) ?: ""))
        }
        val ltlTable = JBTable(ltlTableModel)
        val ltlScrollPane = JBScrollPane(ltlTable)
        constraints.gridx = 0
        constraints.gridy = 1
        rootPanel.add(ltlScrollPane, constraints)

        val ctlTableModel = CustomTableModel()
        ctlTableModel.setColumnIdentifiers(arrayOf("CTL"))
        for (i in ctlFormulas.indices) {
            ctlTableModel.addRow(arrayOf(ctlFormulas.getOrNull(i) ?: ""))
        }
        val ctlTable = JBTable(ctlTableModel)
        val ctlScrollPane = JBScrollPane(ctlTable)
        constraints.gridx = 0
        constraints.gridy = 2
        rootPanel.add(ctlScrollPane, constraints)

        val resultScrollPane = JBScrollPane(ctrl.resultTextArea)
        constraints.gridx = 0
        constraints.gridy = 3
        rootPanel.add(resultScrollPane, constraints)

        constraints.gridx = 0
        constraints.gridy = 4
        rootPanel.add(ctrl.progressBar, constraints)

        val actionBtnPanel = JPanel()
        val startBtn = JButton("Start")
        startBtn.addActionListener {ctrl.start() }
        actionBtnPanel.add(startBtn)
        val cancelBtn = JButton("Cancel")
        cancelBtn.addActionListener { cancel() }
        actionBtnPanel.add(cancelBtn)
        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        bottomPanel.add(actionBtnPanel)
        constraints.gridx = 0
        constraints.gridy = 5
        constraints.insets = JBUI.emptyInsets()
        rootPanel.add(bottomPanel, constraints)

        contentPane.add(rootPanel)
    }

    private fun cancel() {
        isVisible = false
        dispose()
    }
}
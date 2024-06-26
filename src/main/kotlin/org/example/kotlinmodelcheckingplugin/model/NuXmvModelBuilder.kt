package org.example.kotlinmodelcheckingplugin.model

import org.example.kotlinmodelcheckingplugin.model.stmt_value.*
import org.example.kotlinmodelcheckingplugin.model.state_machine.StateMachine

/**
 * Class for building a program model in the nuXmv language
 */
class NuXmvModelBuilder(
    private val variables: List<Variable>,
    private val stateMachines: List<StateMachine>,
    private val ltlFormulas: List<String>,
    private val ctlFormulas: List<String>
) {
    private val tab = "    " // 4 spaces

    /**
     * Create model from its component parts
     */
    fun getModel(): String {
        val model = StringBuilder()
        model.append("MODULE main\n")
            .append(getVarBlock())
            .append("ASSIGN\n")
                .append(getInitBlock())
                .append("\n")
                .append(getStateMachineBlock())
                .append("\n")
            .append(getSpecsBlock())

        return model.toString()
    }

    /**
     * Declaring variables
     */
    private fun getVarBlock(): StringBuilder {
        val result = StringBuilder("VAR\n")
        for (variable in variables) {
            when (variable.value.type) {
                StmtType.INT -> {
                    var min = Int.MAX_VALUE
                    var max = Int.MIN_VALUE
                    for (vertex in stateMachines.filter { it.varName == variable.name }[0].vertices) {
                        if (vertex.intValue!! < min) min = vertex.intValue!!
                        if (vertex.intValue!! > max) max = vertex.intValue!!
                    }
                    result.append("${tab}${variable.name}: ${min}..${max};\n")
                }
                StmtType.BOOL -> {
                    result.append("${tab}${variable.name}: boolean;\n")
                }
                else -> {}
            }
        }
        return result
    }

    /**
     * Assigning variables their initial values
     */
    private fun getInitBlock(): StringBuilder {
        val result = StringBuilder()
        for (variable in variables) {
            when (variable.value.type) {
                StmtType.INT -> {
                    result.append("${tab}init(${variable.name}) := ${variable.initValue.intValue};\n")
                }
                StmtType.DOUBLE -> {
                    result.append("${tab}init(${variable.name}) := ${variable.initValue.doubleValue};\n")
                }
                StmtType.BOOL -> {
                    result.append("${tab}init(${variable.name}) := ${variable.initValue.boolValue.toString().uppercase()};\n")
                }
                else -> {}
            }
        }
        return result
    }

    /**
     * Description of finite state machines (transitions between states under appropriate conditions)
     */
    private fun getStateMachineBlock(): StringBuilder {
        val result = StringBuilder()
        for (stateMachine in stateMachines) {
            result.append("${tab}next(${stateMachine.varName}) := case\n")

            for (transition in stateMachine.getTransitions()) {
                val transitionDescription = StringBuilder("${tab}${tab}")
                for (i in transition.allConditions.indices) {
                    val conditionList = transition.allConditions[i]
                    val conditionDescription = getConditionDescription(conditionList)
                    if (transition.allConditions.indices.last == 0) {
                        transitionDescription.append(conditionDescription)
                    } else {
                        transitionDescription.append("(${conditionDescription})")
                        if (i != transition.allConditions.indices.last) {
                            transitionDescription.append(" | ")
                        }
                    }
                }
                transitionDescription.append(" : ")
                if (transition.targetIds.size > 1) transitionDescription.append("{")
                for (i in transition.targetIds.indices) {
                    val vertex = stateMachine.vertices[transition.targetIds[i]]
                    when (vertex.type) {
                        StmtType.INT -> transitionDescription.append(vertex.intValue)
                        StmtType.DOUBLE -> transitionDescription.append(vertex.doubleValue)
                        StmtType.BOOL -> transitionDescription.append(vertex.boolValue.toString().uppercase())
                        else -> {}
                    }

                    if (i != transition.targetIds.indices.last) transitionDescription.append(", ")
                }
                if (transition.targetIds.size > 1) transitionDescription.append("}")
                transitionDescription.append(";\n")

                result.append(transitionDescription)
            }

            result.append("${tab}${tab}TRUE : ${stateMachine.varName};\n")

            result.append("${tab}esac;\n\n")
        }
        return result
    }

    private fun getConditionDescription(conditionList: List<Variable>): StringBuilder {
        val conditionDescription = StringBuilder()
        for (i in conditionList.indices) {
            val variable = conditionList[i]
            when (variable.value.type) {
                StmtType.INT -> {
                    conditionDescription.append("${variable.name} = ${variable.value.intValue}")
                }
                StmtType.BOOL -> {
                    conditionDescription.append("${variable.name} = ${variable.value.boolValue.toString().uppercase()}")
                }
                else -> {}
            }
            if (i != conditionList.indices.last) conditionDescription.append(" & ")
        }
        return conditionDescription
    }

    /**
     * Block with LTL and CTL specifications
     */
    private fun getSpecsBlock(): StringBuilder {
        val result = StringBuilder()
        ltlFormulas.forEach{ result.append("LTLSPEC\n${it}\n\n") }
        ctlFormulas.forEach{ result.append("CTLSPEC\n${it}\n\n") }
        return result
    }
}
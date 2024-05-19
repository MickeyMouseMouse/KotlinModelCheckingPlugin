package org.example.kotlinmodelcheckingplugin.verification

import org.example.kotlinmodelcheckingplugin.verification.variable.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine
import org.example.kotlinmodelcheckingplugin.verification.variable.Variable

class NuXmvModelBuilder(
    private val vars: List<Variable>,
    private val consts: List<Constant>,
    private val stateMachines: Map<String, StateMachine>,
    private val ltlFormulas: List<String>,
    private val ctlFormulas: List<String>
) {
    private val tab = "    " // 4 spaces

    /**
     *
     */
    fun getModel(): String {
        val model = StringBuilder()
        model.append("MODULE main\n")
            .append(getDefineBlock())
            .append(getVarBlock())
            .append("ASSIGN\n")
                .append(getInitBlock())
                .append("\n")
                .append(getStateMachineBlock())
                .append("\n")
            .append(getSpecsBlock())

        return model.toString()
    }

    private fun getDefineBlock(): StringBuilder {
        if (consts.isEmpty()) return StringBuilder()

        val result = StringBuilder("DEFINE\n")
        for (const in consts) {
            result.append("${tab}${const.name} := ${const.value.getValue()};\n")
        }
        return result
    }

    private fun getVarBlock(): StringBuilder {
        val result = StringBuilder("VAR\n")
        for (variable in vars) {
            result.append("${tab}${variable.name}: ")
            when (variable.value.type) {
                VariableType.INT -> {
                    val vertices = stateMachines[variable.name]?.vertices
                    if (vertices != null) {
                        var min = Int.MAX_VALUE
                        var max = Int.MIN_VALUE
                        for (vertex in vertices) {
                            if (vertex.intValue!! < min) min = vertex.intValue!!
                            if (vertex.intValue!! > max) max = vertex.intValue!!
                        }
                        result.append("${min}..${max};\n")
                    }
                }
                VariableType.DOUBLE -> {
                    // TODO
                }
                VariableType.BOOL -> {
                    result.append("boolean;\n")
                }
                VariableType.UNKNOWN -> {}
            }
        }
        return result
    }

    private fun getInitBlock(): StringBuilder {
        val result = StringBuilder()
        for (variable in vars) {
            result.append("${tab}init(${variable.name}) := ")
            when (variable.value.type) {
                VariableType.INT -> {
                    result.append("${variable.initValue.intValue};\n")
                }
                VariableType.DOUBLE -> {
                    result.append("${variable.initValue.doubleValue};\n")
                }
                VariableType.BOOL -> {
                    result.append("${variable.initValue.boolValue.toString().uppercase()};\n")
                }
                VariableType.UNKNOWN -> {}
            }
        }
        return result
    }

    private fun getStateMachineBlock(): StringBuilder {
        val result = StringBuilder()
        for ((varName, stateMachine) in stateMachines) {
            result.append("${tab}next(${varName}) := case\n")

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
                        VariableType.INT -> transitionDescription.append(vertex.intValue)
                        VariableType.DOUBLE -> transitionDescription.append(vertex.doubleValue)
                        VariableType.BOOL -> transitionDescription.append(vertex.boolValue.toString().uppercase())
                        VariableType.UNKNOWN -> {}
                    }

                    if (i != transition.targetIds.indices.last) transitionDescription.append(", ")
                }
                if (transition.targetIds.size > 1) transitionDescription.append("}")
                transitionDescription.append(";\n")

                result.append(transitionDescription)
            }

            result.append("${tab}${tab}TRUE : ${varName};\n")

            result.append("${tab}esac;\n\n")
        }
        return result
    }

    private fun getConditionDescription(conditionList: List<Variable>): StringBuilder {
        val conditionDescription = StringBuilder()
        for (i in conditionList.indices) {
            val variable = conditionList[i]
            when (variable.value.type) {
                VariableType.INT -> {
                    conditionDescription.append("${variable.name} = ${variable.value.intValue}")
                }
                VariableType.BOOL -> {
                    conditionDescription.append("${variable.name} = ${variable.value.boolValue.toString().uppercase()}")
                }
                VariableType.DOUBLE -> {}
                VariableType.UNKNOWN -> {}
            }
            if (i != conditionList.indices.last) conditionDescription.append(" & ")
        }
        return conditionDescription
    }

    private fun getSpecsBlock(): StringBuilder {
        val result = StringBuilder()
        ltlFormulas.forEach{ result.append("LTLSPEC\n${it}\n\n") }
        ctlFormulas.forEach{ result.append("CTLSPEC\n${it}\n\n") }
        return result
    }
}
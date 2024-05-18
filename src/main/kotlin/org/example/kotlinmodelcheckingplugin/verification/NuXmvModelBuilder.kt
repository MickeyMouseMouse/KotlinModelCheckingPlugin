package org.example.kotlinmodelcheckingplugin.verification

import org.example.kotlinmodelcheckingplugin.verification.variable.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine
import org.example.kotlinmodelcheckingplugin.verification.variable.Variable

class NuXmvModelBuilder(
    private val vars: List<Variable>,
    private val stateMachines: Map<String, StateMachine>,
    private val ltlFormulas: List<String>,
    private val ctlFormulas: List<String>
) {
    private val tab = "    " // 4 spaces

    private fun getVarBlock(): StringBuilder {
        val result = StringBuilder()
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
            // TODO
            result.append("${tab}esac;\n\n")
        }
        return result
    }

    private fun getSpecsBlock(): StringBuilder {
        val result = StringBuilder()
        ltlFormulas.forEach{ result.append("LTLSPEC\n${it}\n\n") }
        ctlFormulas.forEach{ result.append("CTLSPEC\n${it}\n\n") }
        return result
    }


    /**
     *
     */
    fun getModel(): String {
        val model = StringBuilder()
        model.append("MODULE main\n")
            .append("VAR\n").append(getVarBlock())
            .append("ASSIGN\n").append(getInitBlock()).append("\n").append(getStateMachineBlock()).append("\n")
            .append(getSpecsBlock())

        return model.toString()
    }
}
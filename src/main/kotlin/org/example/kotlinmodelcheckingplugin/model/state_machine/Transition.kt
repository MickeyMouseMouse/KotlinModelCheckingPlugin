package org.example.kotlinmodelcheckingplugin.model.state_machine

import org.example.kotlinmodelcheckingplugin.model.stmt_value.Variable

data class Transition(val allConditions: List<List<Variable>>, val targetIds: MutableList<Int>)

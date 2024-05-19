package org.example.kotlinmodelcheckingplugin.verification.state_machine

import org.example.kotlinmodelcheckingplugin.verification.stmt_value.Variable

data class Transition(val allConditions: List<List<Variable>>, val targetIds: MutableList<Int>)

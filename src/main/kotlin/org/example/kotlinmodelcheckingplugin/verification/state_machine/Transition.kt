package org.example.kotlinmodelcheckingplugin.verification.state_machine

import org.example.kotlinmodelcheckingplugin.verification.variable.Variable

data class Transition(val targetIds: MutableList<Int>, val allConditions: List<List<Variable>>)
package org.example.kotlinmodelcheckingplugin.verification.state_machine

import org.example.kotlinmodelcheckingplugin.verification.stmt_value.Variable

data class Edge(val fromIndex: Int, val toIndex: Int, val conditions: List<Variable>)
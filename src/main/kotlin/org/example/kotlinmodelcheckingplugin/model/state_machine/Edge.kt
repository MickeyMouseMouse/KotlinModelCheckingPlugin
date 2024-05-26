package org.example.kotlinmodelcheckingplugin.model.state_machine

import org.example.kotlinmodelcheckingplugin.model.stmt_value.Variable

data class Edge(val fromIndex: Int, val toIndex: Int, val conditions: List<Variable>)
package org.example.kotlinmodelcheckingplugin.verification.state_machine

import org.example.kotlinmodelcheckingplugin.verification.variable.Variable

data class Edge(val fromIndex: Int, val toIndex: Int, val conditions: List<Variable>)
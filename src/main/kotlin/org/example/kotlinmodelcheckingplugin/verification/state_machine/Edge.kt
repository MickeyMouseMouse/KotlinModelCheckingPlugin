package org.example.kotlinmodelcheckingplugin.verification.state_machine

data class Edge(val fromIndex: Int, val toIndex: Int, val conditions: List<Pair<String, String>>)
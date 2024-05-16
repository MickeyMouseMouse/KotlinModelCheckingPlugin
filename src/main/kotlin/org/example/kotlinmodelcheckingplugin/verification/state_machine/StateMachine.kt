package org.example.kotlinmodelcheckingplugin.verification.state_machine

class StateMachine(initValue: String) {
    val vertices = mutableListOf<String>()
    val edges = mutableListOf<Edge>()
    private var headIndex: Int

    init {
        vertices.add(initValue)
        headIndex = 0
    }

    fun add(value: String, conditions: List<Pair<String, String>> = listOf()) {
        var exists = false
        for (i in 0..< vertices.size) {
            if (vertices[i] == value) {
                exists = true
                val edge = Edge(headIndex, i, conditions)
                if (edge !in edges) edges.add(edge)
                headIndex = i
                break
            }
        }
        if (!exists) {
            vertices.add(value)
            edges.add(Edge(headIndex, vertices.size - 1, conditions))
            headIndex = vertices.size - 1
        }
    }
}

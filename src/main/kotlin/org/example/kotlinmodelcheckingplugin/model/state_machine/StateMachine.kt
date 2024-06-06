package org.example.kotlinmodelcheckingplugin.model.state_machine

import org.example.kotlinmodelcheckingplugin.model.stmt_value.*

class StateMachine(variable: Variable) {
    val varName: String = variable.name
    val vertices = mutableListOf<StmtValue>()
    val edges = mutableListOf<Edge>()
    private var headIndex: Int // last visited vertex

    init {
        vertices.add(variable.initValue)
        headIndex = 0
    }

    /**
     * Add a new vertex (the next state) in the state machine
     */
    fun addNextState(value: StmtValue, conditions: List<Variable>) {
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

    /**
     * Retrieve information about transitions between states of a finite state machine
     */
    fun getTransitions(): List<Transition> {
        val transitions = mutableListOf<Transition>()
        for (i in 0..<vertices.size) {
            val allConditions = mutableListOf<List<Variable>>()
            for (edge in edges) {
                if (edge.toIndex == i) {
                    allConditions.add(edge.conditions)
                }
            }
            if (allConditions.isNotEmpty()) transitions.add(Transition(allConditions, mutableListOf(i)))
        }

        // merge transitions with the same conditions
        var i = 0
        while (i < transitions.size) {
            var j = i + 1
            while (j < transitions.size) {
                if (transitions[i].allConditions == transitions[j].allConditions) {
                    transitions[i].targetIds.addAll(transitions[j].targetIds)
                    transitions.removeAt(j)
                } else {
                    j++
                }
            }
            i++
        }

        return transitions
    }
}

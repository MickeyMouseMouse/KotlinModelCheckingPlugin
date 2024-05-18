package org.example.kotlinmodelcheckingplugin.verification.state_machine

import org.example.kotlinmodelcheckingplugin.verification.variable.Variable
import org.example.kotlinmodelcheckingplugin.verification.variable.VariableValue

class StateMachine(initValue: VariableValue) {
    val vertices = mutableListOf<VariableValue>()
    val edges = mutableListOf<Edge>()
    private var headIndex: Int

    init {
        vertices.add(initValue)
        headIndex = 0
    }

    fun add(value: VariableValue, conditions: List<Variable>) {
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

    fun getTransitions(): MutableList<Transition> { // // Pair<target_ids, all_transition_conditions>
        val transitions = mutableListOf<Transition>()
        for (i in 0..<vertices.size) {
            val allConditions = mutableListOf<List<Variable>>()
            for (edge in edges) {
                if (edge.toIndex == i) {
                    allConditions.add(edge.conditions)
                }
            }
            transitions.add(Transition(mutableListOf(i), allConditions))
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

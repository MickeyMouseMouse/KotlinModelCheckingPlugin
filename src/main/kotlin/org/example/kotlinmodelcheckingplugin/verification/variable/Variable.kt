package org.example.kotlinmodelcheckingplugin.verification.variable

data class Variable(
    var name: String,
    var initValue: VariableValue,
    var value: VariableValue,
    var isState: Boolean
) {
    fun copy(): Variable {
        return Variable(name, initValue.copy(), value.copy(), isState)
    }
}


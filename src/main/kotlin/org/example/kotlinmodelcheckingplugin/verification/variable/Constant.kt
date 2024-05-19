package org.example.kotlinmodelcheckingplugin.verification.variable

data class Constant(
    var name: String,
    var value: VariableValue
) {
    fun copy(): Constant {
        return Constant(name, value.copy())
    }
}


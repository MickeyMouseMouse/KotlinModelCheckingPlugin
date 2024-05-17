package org.example.kotlinmodelcheckingplugin.verification.variable

data class Variable(
    var name: String,
    var type: VariableType,
    var initValue: VariableValue,
    var value: VariableValue,
    var isState: Boolean
) {
    fun getInitValue(): Comparable<*>? {
        return when (type) {
            VariableType.INT -> initValue.intValue
            VariableType.BOOL -> initValue.boolValue
            VariableType.UNKNOWN -> null
        }
    }

    fun getValue(): Comparable<*>? {
        return when (type) {
            VariableType.INT -> value.intValue
            VariableType.BOOL -> value.boolValue
            VariableType.UNKNOWN -> null
        }
    }
}


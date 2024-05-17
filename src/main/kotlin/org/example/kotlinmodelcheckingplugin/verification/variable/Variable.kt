package org.example.kotlinmodelcheckingplugin.verification.variable

data class Variable(
    var name: String,
    var type: VariableType,
    var initValue: VariableValue,
    var value: VariableValue,
    var isState: Boolean
) {
    fun getInitValue(): Any? {
        return when (type) {
            VariableType.INT -> initValue.intValue
            VariableType.DOUBLE -> initValue.doubleValue
            VariableType.BOOL -> initValue.boolValue
            VariableType.UNKNOWN -> null
        }
    }

    fun getValue(): Any? {
        return when (type) {
            VariableType.INT -> value.intValue
            VariableType.DOUBLE -> value.doubleValue
            VariableType.BOOL -> value.boolValue
            VariableType.UNKNOWN -> null
        }
    }
}


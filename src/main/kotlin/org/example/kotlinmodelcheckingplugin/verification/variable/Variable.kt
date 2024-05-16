package org.example.kotlinmodelcheckingplugin.verification.variable

data class Variable(
    var name: String,
    var type: VariableType,
    var initValue: VariableValue,
    var isState: Boolean
)


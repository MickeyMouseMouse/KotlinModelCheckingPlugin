package org.example.kotlinmodelcheckingplugin.verification.stmt_value

data class Variable(
    var name: String,
    var initValue: StmtValue,
    var value: StmtValue,
    var isState: Boolean
) {
    fun copy(): Variable {
        return Variable(name, initValue.copy(), value.copy(), isState)
    }
}


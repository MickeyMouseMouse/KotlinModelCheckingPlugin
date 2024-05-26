package org.example.kotlinmodelcheckingplugin.model.stmt_value

data class Constant(
    var name: String,
    var value: StmtValue
) {
    fun copy(): Constant {
        return Constant(name, value.copy())
    }
}


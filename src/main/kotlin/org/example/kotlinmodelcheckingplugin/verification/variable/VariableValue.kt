package org.example.kotlinmodelcheckingplugin.verification.variable

data class VariableValue(
    val type: VariableType,
    var intValue: Int?,
    var doubleValue: Double?,
    var boolValue: Boolean?
) {
    constructor() : this(
        VariableType.UNKNOWN,
        intValue = null,
        doubleValue = null,
        boolValue = null
    )

    constructor(intValue: Int?) : this(
        type = if (intValue == null) VariableType.UNKNOWN else VariableType.INT,
        intValue = intValue,
        doubleValue = null,
        boolValue = null
    )

    constructor(doubleValue: Double?) : this(
        type = if (doubleValue == null) VariableType.UNKNOWN else VariableType.DOUBLE,
        intValue = null,
        doubleValue = doubleValue,
        boolValue = null
    )

    constructor(boolValue: Boolean?) : this(
        type = if (boolValue == null) VariableType.UNKNOWN else VariableType.BOOL,
        intValue = null,
        doubleValue = null,
        boolValue = boolValue
    )

    fun getValue(): Any? {
        return when (type) {
            VariableType.INT -> intValue
            VariableType.DOUBLE -> doubleValue
            VariableType.BOOL -> boolValue
            VariableType.UNKNOWN -> null
        }
    }
}
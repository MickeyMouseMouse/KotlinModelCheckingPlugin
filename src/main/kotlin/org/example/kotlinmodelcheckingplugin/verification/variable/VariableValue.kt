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

    constructor(type: VariableType? = null, intValue: Int?) : this(
        type = type ?: if (intValue == null) VariableType.UNKNOWN else VariableType.INT,
        intValue = intValue,
        doubleValue = null,
        boolValue = null
    )

    constructor(type: VariableType? = null, doubleValue: Double?) : this(
        type = type ?: if (doubleValue == null) VariableType.UNKNOWN else VariableType.DOUBLE,
        intValue = null,
        doubleValue = doubleValue,
        boolValue = null
    )

    constructor(type: VariableType? = null, boolValue: Boolean?) : this(
        type = type ?: if (boolValue == null) VariableType.UNKNOWN else VariableType.BOOL,
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
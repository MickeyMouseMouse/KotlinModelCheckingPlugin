package org.example.kotlinmodelcheckingplugin.model.stmt_value

data class StmtValue(
    val type: StmtType,
    var intValue: Int?,
    var doubleValue: Double?,
    var boolValue: Boolean?
) {
    constructor() : this(
        StmtType.UNKNOWN,
        intValue = null,
        doubleValue = null,
        boolValue = null
    )

    constructor(type: StmtType? = null, intValue: Int?) : this(
        type = type ?: if (intValue == null) StmtType.UNKNOWN else StmtType.INT,
        intValue = intValue,
        doubleValue = null,
        boolValue = null
    )

    constructor(type: StmtType? = null, doubleValue: Double?) : this(
        type = type ?: if (doubleValue == null) StmtType.UNKNOWN else StmtType.DOUBLE,
        intValue = null,
        doubleValue = doubleValue,
        boolValue = null
    )

    constructor(type: StmtType? = null, boolValue: Boolean?) : this(
        type = type ?: if (boolValue == null) StmtType.UNKNOWN else StmtType.BOOL,
        intValue = null,
        doubleValue = null,
        boolValue = boolValue
    )

    fun getValue(): Any? {
        return when (type) {
            StmtType.INT -> intValue
            StmtType.DOUBLE -> doubleValue
            StmtType.BOOL -> boolValue
            StmtType.UNKNOWN -> null
        }
    }
}
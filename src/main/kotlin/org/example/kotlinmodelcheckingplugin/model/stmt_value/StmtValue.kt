package org.example.kotlinmodelcheckingplugin.model.stmt_value

data class StmtValue(
    val type: StmtType,
    var intValue: Int?,
    var doubleValue: Double?,
    var boolValue: Boolean?
) {
    constructor(type: StmtType) : this(
        type = type,
        intValue = null,
        doubleValue = null,
        boolValue = null
    )

    constructor(type: StmtType? = null, intValue: Int?) : this(
        type = type ?: if (intValue == null) StmtType.VOID else StmtType.INT,
        intValue = intValue,
        doubleValue = null,
        boolValue = null
    )

    constructor(type: StmtType? = null, doubleValue: Double?) : this(
        type = type ?: if (doubleValue == null) StmtType.VOID else StmtType.DOUBLE,
        intValue = null,
        doubleValue = doubleValue,
        boolValue = null
    )

    constructor(type: StmtType? = null, boolValue: Boolean?) : this(
        type = type ?: if (boolValue == null) StmtType.VOID else StmtType.BOOL,
        intValue = null,
        doubleValue = null,
        boolValue = boolValue
    )

    fun isInitialized(): Boolean {
        return intValue != null || doubleValue != null || boolValue != null
    }

    fun getValue(): Any? {
        return when (type) {
            StmtType.INT -> intValue
            StmtType.DOUBLE -> doubleValue
            StmtType.BOOL -> boolValue
            StmtType.VOID -> null
        }
    }
}
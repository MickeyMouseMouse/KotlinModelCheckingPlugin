package org.example.kotlinmodelcheckingplugin.model.stmt_value

data class StmtValue(
    val type: StmtType,
    var intValue: Int?,
    var doubleValue: Double?,
    var boolValue: Boolean?,
    var stringValue: String?
) {
    constructor(type: StmtType) : this(
        type = type,
        intValue = null,
        doubleValue = null,
        boolValue = null,
        stringValue = null
    )

    constructor(type: StmtType? = null, intValue: Int?) : this(
        type = type ?: if (intValue == null) StmtType.VOID else StmtType.INT,
        intValue = intValue,
        doubleValue = null,
        boolValue = null,
        stringValue = null
    )

    constructor(type: StmtType? = null, doubleValue: Double?) : this(
        type = type ?: if (doubleValue == null) StmtType.VOID else StmtType.DOUBLE,
        intValue = null,
        doubleValue = doubleValue,
        boolValue = null,
        stringValue = null
    )

    constructor(type: StmtType? = null, boolValue: Boolean?) : this(
        type = type ?: if (boolValue == null) StmtType.VOID else StmtType.BOOL,
        intValue = null,
        doubleValue = null,
        boolValue = boolValue,
        stringValue = null
    )

    constructor(type: StmtType? = null, stringValue: String?) : this(
        type = type ?: if (stringValue == null) StmtType.VOID else StmtType.STRING,
        intValue = null,
        doubleValue = null,
        boolValue = null,
        stringValue = stringValue
    )

    fun isInitialized(): Boolean {
        return intValue != null || doubleValue != null || boolValue != null || stringValue != null
    }

    fun getValue(): Any? {
        return when (type) {
            StmtType.INT -> intValue
            StmtType.DOUBLE -> doubleValue
            StmtType.BOOL -> boolValue
            StmtType.STRING -> stringValue
            StmtType.VOID -> null
        }
    }
}
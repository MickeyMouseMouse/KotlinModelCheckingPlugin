package org.example.kotlinmodelcheckingplugin.verification.dataclasses

data class VarInfo(
    val name: String,
    val type: String?,
    var initValue: String?,
    var isState: Boolean
)

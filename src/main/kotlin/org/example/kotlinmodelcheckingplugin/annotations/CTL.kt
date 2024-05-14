package org.example.kotlinmodelcheckingplugin.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class CTL(val formula: String)

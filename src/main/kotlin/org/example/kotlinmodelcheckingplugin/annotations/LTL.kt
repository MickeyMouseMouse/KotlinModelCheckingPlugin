package org.example.kotlinmodelcheckingplugin.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class LTL(val formula: String)

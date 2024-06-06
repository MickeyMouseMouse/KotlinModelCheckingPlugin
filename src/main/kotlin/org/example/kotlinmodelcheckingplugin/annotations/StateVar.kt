// kotlinc.bat StateVar.kt LTL.kt CTL.kt -d ../../../../../resources/modelcheckingannotations.jar
package org.example.kotlinmodelcheckingplugin.annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class StateVar

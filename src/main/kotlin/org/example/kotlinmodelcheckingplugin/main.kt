package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.verification.Analyzer
import org.example.kotlinmodelcheckingplugin.verification.variable.*
import java.io.File

// for debug
fun main() {
    val sourceCode = File("src/test/kotlin/TrafficLight.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        sourceCode,
        "TrafficLight",
        mutableListOf(
            Variable("mode", VariableType.INT, VariableValue(intValue=0), VariableValue(intValue=0),true),
            Variable("red", VariableType.BOOL, VariableValue(boolValue=false), VariableValue(boolValue=false), false),
            Variable("green", VariableType.BOOL, VariableValue(boolValue=false), VariableValue(boolValue=false), false),
            Variable("yellow", VariableType.BOOL, VariableValue(boolValue=false), VariableValue(boolValue=false), false)
        ),
        listOf(
            "G (!(red = TRUE & yellow = TRUE & green = TRUE))"
        ),
        listOf(

        )
    )
    print(analyzer.start())
}
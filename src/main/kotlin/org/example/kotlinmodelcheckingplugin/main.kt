package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.verification.Analyzer
import org.example.kotlinmodelcheckingplugin.verification.annotation_dataclasses.*
import java.io.File

// for debug
fun main() {
    val sourceCode = File("src/test/kotlin/TrafficLight.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        "TrafficLight",
        sourceCode,
        listOf(
            StateVarInfo("mode", "int", "0"),
            StateVarInfo("red", "Boolean", "false"),
            StateVarInfo("yellow", "Boolean", "false"),
            StateVarInfo("green", "Boolean", "false"),
        ),
        listOf(
            LTLFormula("G (!(red = TRUE & yellow = TRUE & green = TRUE))")
        ),
        listOf()
    )
    print(analyzer.start())
}
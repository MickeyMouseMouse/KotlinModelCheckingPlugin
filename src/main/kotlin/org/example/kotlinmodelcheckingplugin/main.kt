package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.verification.Analyzer
import org.example.kotlinmodelcheckingplugin.verification.variable.*
import java.io.File

// for debug
fun main() {
    //trafficLightTest()
    atmTest()
}

fun trafficLightTest() {
    val sourceCode = File("src/test/kotlin/TrafficLight.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        sourceCode,
        "TrafficLight",
        mutableListOf(
            Variable(
                "mode",
                VariableValue(intValue=0),
                VariableValue(intValue=0),
                true
            ),
            Variable(
                "red",
                VariableValue(boolValue=false),
                VariableValue(boolValue=false),
                false
            ),
            Variable(
                "green",
                VariableValue(boolValue=false),
                VariableValue(boolValue=false),
                false
            ),
            Variable(
                "yellow",
                VariableValue(boolValue=false),
                VariableValue(boolValue=false),
                false
            )
        ),
        listOf(
            "G (!(red = TRUE & yellow = TRUE & green = TRUE))"
        ),
        listOf(
            "AG mode < 5"
        )
    )

    analyzer.start()
    print(analyzer.model)
    print(analyzer.modelCheckingResult)
}

fun atmTest() {
    val sourceCode = File("src/test/kotlin/ATM.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        sourceCode,
        "ATM",
        mutableListOf(
            Variable(
                "mode",
                VariableValue(intValue=0),
                VariableValue(intValue=0),
                true
            )
        ),
        listOf(
            "G ((mode=0) -> X(mode!=5))"
        ),
        listOf(

        )
    )

    analyzer.start()
    print(analyzer.model)
    print(analyzer.modelCheckingResult)
}
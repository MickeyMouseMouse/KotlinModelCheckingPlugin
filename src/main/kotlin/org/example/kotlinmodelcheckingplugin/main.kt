package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.verification.Analyzer
import org.example.kotlinmodelcheckingplugin.verification.variable.*
import java.io.File

// for debug
fun main() {
    trafficLight()
    atm()
    lock()
}

fun trafficLight() {
    val sourceCode = File("src/test/kotlin/TrafficLight.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        sourceCode,
        "TrafficLight",
        listOf(
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
        listOf(),
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

fun atm() {
    val sourceCode = File("src/test/kotlin/ATM.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        sourceCode,
        "ATM",
        listOf(
            Variable(
                "mode",
                VariableValue(intValue=0),
                VariableValue(intValue=0),
                true
            )
        ),
        listOf(),
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

fun lock() {
    val sourceCode = File("src/test/kotlin/Lock.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")

    val analyzer = Analyzer(
        sourceCode,
        "Lock",
        listOf(
            Variable(
                "value1",
                VariableValue(intValue=0),
                VariableValue(intValue=0),
                true
            ),
            Variable(
                "value2",
                VariableValue(intValue=0),
                VariableValue(intValue=0),
                true
            ),
            Variable(
                "value3",
                VariableValue(intValue=0),
                VariableValue(intValue=0),
                true
            ),
            Variable(
                "isOpen",
                VariableValue(boolValue=false),
                VariableValue(boolValue=false),
                false
            )
        ),
        listOf(
            Constant(
                "secret1",
                VariableValue(intValue=1)
            ),
            Constant(
                "secret2",
                VariableValue(intValue=2)
            ),
            Constant(
                "secret3",
                VariableValue(intValue=3)
            )
        ),
        listOf(
            "G ((value1 != 1 & value1 != 2 & value3 != 3) -> X(isOpen = FALSE))"
        ),
        listOf(

        )
    )

    analyzer.start()
    print(analyzer.model)
    print(analyzer.modelCheckingResult)
}
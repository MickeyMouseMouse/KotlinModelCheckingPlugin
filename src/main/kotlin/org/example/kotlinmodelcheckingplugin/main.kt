package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.verification.Analyzer
import org.example.kotlinmodelcheckingplugin.verification.stmt_value.*
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
                StmtValue(intValue=0),
                StmtValue(intValue=0),
                true
            ),
            Variable(
                "red",
                StmtValue(boolValue=false),
                StmtValue(boolValue=false),
                false
            ),
            Variable(
                "green",
                StmtValue(boolValue=false),
                StmtValue(boolValue=false),
                false
            ),
            Variable(
                "yellow",
                StmtValue(boolValue=false),
                StmtValue(boolValue=false),
                false
            )
        ),
        listOf(), // constants
        listOf( // ltl
            "G (!(red = TRUE & yellow = TRUE & green = TRUE))"
        ),
        listOf( // ctl
            "AG mode < 5"
        )
    )

    analyzer.start()
    print(analyzer.model)
    println(analyzer.modelCheckingResult)
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
                StmtValue(intValue=0),
                StmtValue(intValue=0),
                true
            )
        ),
        listOf(), // constants
        listOf( // ltl
            "G ((mode=0) -> X(mode!=5))"
        ),
        listOf( // ctl

        )
    )

    analyzer.start()
    print(analyzer.model)
    println(analyzer.modelCheckingResult)
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
                StmtValue(intValue=0),
                StmtValue(intValue=0),
                true
            ),
            Variable(
                "value2",
                StmtValue(intValue=0),
                StmtValue(intValue=0),
                true
            ),
            Variable(
                "value3",
                StmtValue(intValue=0),
                StmtValue(intValue=0),
                true
            ),
            Variable(
                "isOpen",
                StmtValue(boolValue=false),
                StmtValue(boolValue=false),
                false
            )
        ),
        listOf(
            Constant(
                "secret1",
                StmtValue(intValue=1)
            ),
            Constant(
                "secret2",
                StmtValue(intValue=2)
            ),
            Constant(
                "secret3",
                StmtValue(intValue=3)
            )
        ),
        listOf( // ltl
            "G ((value1 != 1 & value1 != 2 & value3 != 3) -> X(isOpen = FALSE))"
        ),
        listOf( // ctl

        )
    )

    analyzer.start()
    print(analyzer.model)
    println(analyzer.modelCheckingResult)
}
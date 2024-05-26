package org.example.kotlinmodelcheckingplugin

import org.example.kotlinmodelcheckingplugin.model.Analyzer
import org.example.kotlinmodelcheckingplugin.model.NuXmvModelBuilder
import org.example.kotlinmodelcheckingplugin.model.Tools
import org.example.kotlinmodelcheckingplugin.model.stmt_value.*
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
    val className = "TrafficLight"
    val (result: Pair<Int, String>, jarFile: File?) = Tools.compile(className, sourceCode)
    if (jarFile == null) {
        println(result.second)
        return
    }

    val variables = listOf(
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
    )
    val constants = listOf<Constant>()
    val stateMachines = Analyzer(variables, constants).buildStateMachines(className, jarFile)

    val model = NuXmvModelBuilder(
        variables,
        constants,
        stateMachines,
        listOf( // ltl
            "G (!(red = TRUE & yellow = TRUE & green = TRUE))"
        ),
        listOf( // ctl
            "AG mode < 10"
        )
    ).getModel()
    print(Tools.runModelChecker(className, model).second)
}


fun atm() {
    val sourceCode = File("src/test/kotlin/ATM.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")
    val className = "ATM"
    val (result: Pair<Int, String>, jarFile: File?) = Tools.compile(className, sourceCode)
    if (jarFile == null) {
        println(result.second)
        return
    }

    val variables = listOf(
        Variable(
            "mode",
            StmtValue(intValue=0),
            StmtValue(intValue=0),
            true
        )
    )
    val constants = listOf<Constant>()
    val stateMachines = Analyzer(variables, constants).buildStateMachines(className, jarFile)

    val model = NuXmvModelBuilder(
        variables,
        constants,
        stateMachines,
        listOf( // ltl
            "G ((mode=0) -> X(mode!=5))"
        ),
        listOf( // ctl

        )
    ).getModel()
    print(Tools.runModelChecker(className, model).second)
}


fun lock() {
    val sourceCode = File("src/test/kotlin/Lock.kt").readText()
        .replace("import org\\.example\\.kotlinmodelcheckingplugin\\.annotations\\.(\\*|StateVar|LTL|CTL)".toRegex(), "")
        .replace("@(StateVar|LTL|CTL)(\\((.)+\\)|)".toRegex(), "")
    val className = "Lock"
    val (result: Pair<Int, String>, jarFile: File?) = Tools.compile(className, sourceCode)
    if (jarFile == null) {
        println(result.second)
        return
    }

    val variables = listOf(
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
    )
    val constants = listOf (
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
    )
    val stateMachines = Analyzer(variables, constants).buildStateMachines(className, jarFile)

    val model = NuXmvModelBuilder(
        variables,
        constants,
        stateMachines,
        listOf( // ltl
            "G ((value1 != 1 & value1 != 2 & value3 != 3) -> X(isOpen = FALSE))"
        ),
        listOf( // ctl

        )
    ).getModel()
    print(Tools.runModelChecker(className, model).second)
}

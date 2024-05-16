import org.example.kotlinmodelcheckingplugin.verification.dataclasses.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine

class NuXmvModelBuilder(
    private val vars: List<VarInfo>,
    private val stateMachines: Map<String, StateMachine>,
    private val ltlFormulas: List<String>,
    private val ctlFormulas: List<String>
) {
    private val tab = "    " // 4 spaces

    private fun getBoolean(value: String): String {
        return if (arrayOf("true", "1").contains(value.lowercase())) "TRUE" else "FALSE"
    }

    /**
     *
     */
    fun getModel(): String {
        val model = StringBuilder("MODULE main\n")

        // VAR block
        model.append("VAR\n")
        for (variable in vars) {
            model.append("${tab}${variable.name}: ")
            when (variable.type) {
                "int" -> {
                    // ???
                }
                "boolean" -> {
                    model.append("boolean;\n")
                }
            }
        }

        // ASSIGN block
        model.append("ASSIGN\n")
        // initial values
        for (variable in vars) {
            model.append("${tab}init(${variable.name}) := ")
            when (variable.type) {
                "int" -> {
                    model.append("${variable.initValue};\n")
                }
                "boolean" -> {
                    model.append("${getBoolean(variable.initValue)};\n")
                }
            }
        }
        model.append("\n")

        model.append("TODO\n")

        model.append("\n")

        // specs
        ltlFormulas.forEach{ model.append("LTLSPEC\n${it}\n\n") }
        ctlFormulas.forEach{ model.append("CTLSPEC\n${it}\n\n") }

        return model.toString()
    }
}
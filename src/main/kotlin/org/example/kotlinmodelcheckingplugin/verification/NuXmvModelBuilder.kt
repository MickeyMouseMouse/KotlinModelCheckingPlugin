import org.example.kotlinmodelcheckingplugin.verification.variable.*
import org.example.kotlinmodelcheckingplugin.verification.state_machine.StateMachine
import org.example.kotlinmodelcheckingplugin.verification.variable.Variable

class NuXmvModelBuilder(
    private val vars: List<Variable>,
    private val stateMachines: Map<String, StateMachine>,
    private val ltlFormulas: List<String>,
    private val ctlFormulas: List<String>
) {
    private val tab = "    " // 4 spaces

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
                VariableType.INT -> {
                    // ???
                }
                VariableType.BOOL -> {
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
                VariableType.INT -> {
                    model.append("${variable.initValue.intValue};\n")
                }
                VariableType.BOOL -> {
                    model.append("${variable.initValue.boolValue.toString()};\n")
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
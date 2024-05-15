import org.example.kotlinmodelcheckingplugin.verification.dataclasses.*

class NuXmvModelBuilder(
    private val vars: List<VarInfo>,
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
            model.append("${tab}${variable.name}:")
            when (variable.type) {
                "int" -> {

                }
                "boolean" -> {

                }
            }
            model.append(";\n")
        }

        // ASSIGN block
        model.append("ASSIGN\n")
        // initial values
        for (variable in vars) {
            model.append("${tab}init(${variable.name}) := ${variable.initValue};\n")
        }
        model.append("\n")



        model.append("\n")

        // specs
        ltlFormulas.forEach{ model.append("LTLSPEC\n${it}\n\n") }
        ctlFormulas.forEach{ model.append("CTLSPEC\n${it}\n\n") }

        return model.toString()
    }
}
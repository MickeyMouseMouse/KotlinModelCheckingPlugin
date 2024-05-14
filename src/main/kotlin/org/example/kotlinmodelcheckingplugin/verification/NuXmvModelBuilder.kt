import org.example.kotlinmodelcheckingplugin.verification.annotation_dataclasses.CTLFormula
import org.example.kotlinmodelcheckingplugin.verification.annotation_dataclasses.LTLFormula
import org.example.kotlinmodelcheckingplugin.verification.annotation_dataclasses.StateVarInfo

class NuXmvModelBuilder(
    private val stateVars: List<StateVarInfo>,
    private val ltlFormulas: List<LTLFormula>,
    private val ctlFormulas: List<CTLFormula>
) {
    private val tab = "    " // 4 spaces

    /**
     *
     */
    fun getModel(): String {
        val model = StringBuilder("MODULE main\n")

        // VAR block
        model.append("VAR\n")
        for (stateVar in stateVars) {
            model.append("${tab}${stateVar.name}:")
            when (stateVar.type) {
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
        for (stateVar in stateVars) {
            model.append("${tab}init(${stateVar.name}) := ${stateVar.initValue};\n")
        }
        model.append("\n")



        model.append("\n")

        // specs
        ltlFormulas.forEach{ model.append("LTLSPEC\n${it.formula}\n\n") }
        ctlFormulas.forEach{ model.append("CTLSPEC\n${it.formula}\n\n") }

        return model.toString()
    }
}
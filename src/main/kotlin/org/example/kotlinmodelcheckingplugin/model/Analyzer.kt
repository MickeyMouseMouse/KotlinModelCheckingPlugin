package org.example.kotlinmodelcheckingplugin.model

import org.example.kotlinmodelcheckingplugin.model.stmt_value.*
import org.example.kotlinmodelcheckingplugin.model.state_machine.StateMachine
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.constant.BooleanConstant
import sootup.core.jimple.common.constant.DoubleConstant
import sootup.core.jimple.common.constant.IntConstant
import sootup.core.jimple.common.constant.StringConstant
import sootup.core.jimple.common.expr.*
import sootup.core.jimple.common.ref.JInstanceFieldRef
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.jimple.common.stmt.JGotoStmt
import sootup.core.jimple.common.stmt.JIdentityStmt
import sootup.core.jimple.common.stmt.JIfStmt
import sootup.core.jimple.common.stmt.JInvokeStmt
import sootup.core.jimple.common.stmt.JReturnStmt
import sootup.core.jimple.common.stmt.JReturnVoidStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt
import sootup.core.signatures.MethodSignature
import sootup.core.types.PrimitiveType
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.jimple.basic.JavaLocal
import sootup.java.core.views.JavaView
import java.io.File

/**
 * Class for analyzing the intermediate representation (Jimple) of a program
 */
class Analyzer(
    private val jarFile: File,
    private val className: String,
    private val variables: List<Variable>,
    private val constants: List<Constant>
) {
    private lateinit var view: JavaView
    private val stateMachines = mutableListOf<StateMachine>()

    /**
     * The method retrieves finite state machines for variables
     */
    fun buildStateMachines(): List<StateMachine> {
        view = JavaView(JavaClassPathAnalysisInputLocation(jarFile.absolutePath))
        val classType = view.identifierFactory.getClassType(className)

        // class constructor
        analyzeMethod(
            view.identifierFactory.getMethodSignature(
                classType, "<init>", "void", listOf()
            ),
            listOf()
        )

        // main() function
        analyzeMethod(
            view.identifierFactory.getMethodSignature(
                classType, "main", "void", listOf()
            ),
            listOf()
        )
        return stateMachines
    }

    /**
     * This function parses Jimple IR of the program and generate finite state machines for the variables.
     */
    private fun analyzeMethod(methodSignature: MethodSignature, args: List<Variable>): StmtValue {
        val methodOptional = view.getMethod(methodSignature)
        if (!methodOptional.isPresent) return StmtValue(type=StmtType.VOID)

        val method = methodOptional.get()
        val localVariables = mutableListOf<Variable>()
        var stmt = method.body.stmts[0]
        while (true) {
            when (stmt) {
                is JIdentityStmt -> {  // parsing the passed arguments
                    val (argName, argType) = stmt.rightOp.toString().split(": ")
                    for (arg in args) {
                        if (arg.name == argName) {
                            when (argType) {
                                "int" -> {
                                    localVariables.add(
                                        Variable(
                                            stmt.leftOp.name,
                                            StmtValue(type = StmtType.INT),
                                            StmtValue(intValue = arg.value.intValue),
                                            false
                                        )
                                    )
                                }

                                "double" -> {
                                    localVariables.add(
                                        Variable(
                                            stmt.leftOp.name,
                                            StmtValue(type = StmtType.DOUBLE),
                                            StmtValue(doubleValue = arg.value.doubleValue),
                                            false
                                        )
                                    )
                                }

                                "boolean" -> {
                                    localVariables.add(
                                        Variable(
                                            stmt.leftOp.name,
                                            StmtValue(type = StmtType.BOOL),
                                            StmtValue(boolValue = arg.value.intValue != 0),
                                            false
                                        )
                                    )
                                }

                                "java.lang.String" -> {
                                    localVariables.add(
                                        Variable(
                                            stmt.leftOp.name,
                                            StmtValue(type = StmtType.STRING),
                                            StmtValue(stringValue = arg.value.stringValue),
                                            false
                                        )
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }

                is JAssignStmt -> {
                    val result = calculate(stmt.rightOp, localVariables)
                    when (stmt.leftOp) {
                        is JInstanceFieldRef -> { // class field
                            val varSignature = (stmt.leftOp as JInstanceFieldRef).fieldSignature
                            for (variable in variables) {
                                if (variable.name == varSignature.name) {
                                    if (!StateMachine.appropriateTypes.contains(variable.value.type)) {
                                        // we form state machines only for appropriate data types
                                        continue
                                    }

                                    if (variable.initValue.isInitialized()) {
                                        if (stateMachines.none { it.varName == variable.name }) {
                                            stateMachines.add(StateMachine(variable))
                                        }
                                    } else {  // not initialized
                                        when (variable.value.type) {
                                            StmtType.INT -> {
                                                variable.initValue.intValue = result.intValue
                                                variable.value.intValue = variable.initValue.intValue
                                            }

                                            StmtType.DOUBLE -> {
                                                variable.initValue.doubleValue = result.doubleValue
                                                variable.value.doubleValue = variable.initValue.doubleValue
                                            }

                                            StmtType.BOOL -> {
                                                variable.initValue.boolValue = result.intValue != 0
                                                variable.value.boolValue = variable.initValue.boolValue
                                            }

                                            else -> {}
                                        }
                                        stateMachines.add(StateMachine(variable))
                                        break
                                    }

                                    when (varSignature.type) {
                                        PrimitiveType.getInt() -> {
                                            val newValue = result.getValue() as Int
                                            if (newValue == variable.value.intValue) break

                                            stateMachines.filter { it.varName == variable.name }[0].addNextState(
                                                StmtValue(intValue = newValue),
                                                getStateVariables()
                                            )
                                            variable.value.intValue = newValue
                                        }

                                        PrimitiveType.getBoolean() -> {
                                            val newValue = (result.getValue() as Int) == 1
                                            if (newValue == variable.value.boolValue) break

                                            stateMachines.filter { it.varName == variable.name }[0].addNextState(
                                                StmtValue(boolValue = newValue),
                                                getStateVariables()
                                            )
                                            variable.value.boolValue = newValue
                                        }
                                    }
                                    break
                                }
                            }
                        }

                        is JavaLocal -> { // method local variable
                            var exists = false
                            for (variable in localVariables) {
                                if (variable.name == (stmt.leftOp as JavaLocal).name) {
                                    exists = true
                                    variable.value = result
                                    break
                                }
                            }
                            if (!exists) localVariables.add(
                                Variable(
                                    (stmt.leftOp as JavaLocal).name,
                                    StmtValue(type = result.type),
                                    result,
                                    false
                                )
                            )
                        }
                    }
                }

                is JInvokeStmt -> { // call another method
                    // prepare arguments
                    val funArgs = mutableListOf<Variable>()
                    for (i in stmt.invokeExpr.args.indices) {
                        val argValue = calculate(stmt.invokeExpr.args[i], localVariables)
                        funArgs.add(Variable("@parameter${i}", StmtValue(type = argValue.type), argValue, false))
                    }

                    analyzeMethod(stmt.invokeExpr.methodSignature, funArgs)
                }

                is JReturnStmt -> {
                    return calculate(stmt.op, localVariables)
                }

                is JReturnVoidStmt -> {
                    return StmtValue(type=StmtType.VOID)
                }
            }

            stmt = when (stmt) { // get the next stmt
                is JIfStmt -> {
                    if (calculate(stmt.condition, localVariables).getValue() as Boolean)
                        stmt.getTargetStmts(method.body)[0]
                    else
                        method.body.stmts[method.body.stmts.indexOf(stmt) + 1]
                }

                is JSwitchStmt -> {
                    lateinit var nextStmt: Stmt
                    for (variable in localVariables) {
                        if (variable.name == (stmt.key as JavaLocal).name) {
                            when (variable.value.type) {
                                StmtType.INT -> {
                                    for (i in 0..<stmt.values.size) {
                                        if ((variable.value.getValue() as Int) == stmt.values[i].value) {
                                            nextStmt = stmt.getTargetStmts(method.body)[i]
                                            break
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                    nextStmt
                }

                is JGotoStmt -> {
                    stmt.getTargetStmts(method.body)[0]
                }

                else -> { // JAssignStmt, JInvokeStmt and so on
                    method.body.stmts[method.body.stmts.indexOf(stmt) + 1]
                }
            }
        }
    }

    /**
     * This function calculates the result (StmtValue) of various values, for example:
     * 1) 2 + 3
     * 2) some_var % 5
     * 3) some_fun() / 5.3 < some_var
     */
    private fun calculate(value: Value, localVariables: List<Variable>): StmtValue {
        var result : StmtValue? = null
        when (value) {
            is IntConstant -> {
                result = StmtValue(intValue = value.value)
            }
            is DoubleConstant -> {
                result = StmtValue(doubleValue = value.value)
            }
            is BooleanConstant -> {
                result = StmtValue(boolValue = value.toString().toBoolean())
            }
            is StringConstant -> {
                result = StmtValue(stringValue = value.value)
            }
            is JInstanceFieldRef -> {
                for (variable in variables) {
                    if (variable.name == value.fieldSignature.name) {
                        when (variable.value.type) {
                            StmtType.INT -> {
                                result = StmtValue(intValue=variable.value.intValue)
                            }
                            StmtType.DOUBLE -> {
                                result = StmtValue(doubleValue=variable.value.doubleValue)
                            }
                            StmtType.BOOL -> {
                                result = StmtValue(boolValue=variable.value.boolValue)
                            }
                            StmtType.STRING -> {
                                result = StmtValue(stringValue=variable.value.stringValue)
                            }
                            else -> {}
                        }
                        break
                    }
                }
                if (result == null)  { // the desired variable was not found => check constants
                    for (constant in constants) {
                        if (constant.name == value.fieldSignature.name) {
                            when (constant.value.type) {
                                StmtType.INT -> {
                                    result = StmtValue(intValue=constant.value.intValue)
                                }
                                StmtType.DOUBLE -> {
                                    result = StmtValue(doubleValue=constant.value.doubleValue)
                                }
                                StmtType.BOOL -> {
                                    result = StmtValue(boolValue=constant.value.boolValue)
                                }
                                StmtType.STRING -> {
                                    result = StmtValue(stringValue=constant.value.stringValue)
                                }
                                else -> {}
                            }
                            break
                        }
                    }
                }
            }
            is JavaLocal -> {
                for (variable in localVariables) {
                    if (variable.name == value.name) {
                        when (variable.value.type) {
                            StmtType.INT -> {
                                result = StmtValue(intValue = variable.value.intValue)
                            }
                            StmtType.DOUBLE -> {
                                result = StmtValue(doubleValue = variable.value.doubleValue)
                            }
                            StmtType.BOOL -> {
                                result = StmtValue(boolValue = variable.value.boolValue)
                            }
                            StmtType.STRING -> {
                                result = StmtValue(stringValue = variable.value.stringValue)
                            }
                            else -> {}
                        }
                        break
                    }
                }
            }
            is JAddExpr -> {
                val op1 = calculate(value.op1, localVariables)
                val op2 = calculate(value.op2, localVariables)
                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(intValue = (op1.getValue() as Int) + (op2.getValue() as Int))
                }
            }
            is JMulExpr -> {
                val op1 = calculate(value.op1, localVariables)
                val op2 = calculate(value.op2, localVariables)
                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(intValue = (op1.getValue() as Int) * (op2.getValue() as Int))
                }
            }
            is JDivExpr -> {
                val op1 = calculate(value.op1, localVariables)
                val op2 = calculate(value.op2, localVariables)
                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(intValue = (op1.getValue() as Int) / (op2.getValue() as Int))
                }
                if (op1.type == StmtType.DOUBLE || op2.type == StmtType.DOUBLE) {
                    result = StmtValue(doubleValue = (op1.getValue() as Double) / (op2.getValue() as Double))
                }
            }
            is JRemExpr -> {
                val op1 = calculate(value.op1, localVariables)
                val op2 = calculate(value.op2, localVariables)
                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(intValue = (op1.getValue() as Int) % (op2.getValue() as Int))
                }
            }
            is JGeExpr -> {
                val op1 = calculate(value.op1, localVariables)
                val op2 = calculate(value.op2, localVariables)
                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(boolValue = (op1.getValue() as Int) >= (op2.getValue() as Int))
                }
            }
            is JEqExpr -> {
                var op1 = calculate(value.op1, localVariables)
                if (op1.type == StmtType.BOOL) {
                    op1 = StmtValue(type = StmtType.INT, intValue = if (op1.boolValue == true) 1 else 0)
                }

                var op2 = calculate(value.op2, localVariables)
                if (op2.type == StmtType.BOOL) {
                    op2 = StmtValue(type = StmtType.INT, intValue = if (op2.boolValue == true) 1 else 0)
                }

                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(boolValue = (op1.getValue() as Int) == (op2.getValue() as Int))
                }

                if (op1.type == StmtType.STRING && op2.type == StmtType.STRING) {
                    result = StmtValue(boolValue = (op1.getValue() as String) == (op2.getValue() as String))
                }
            }
            is JNeExpr -> {
                var op1 = calculate(value.op1, localVariables)
                if (op1.type == StmtType.BOOL) {
                    op1 = StmtValue(type = StmtType.INT, intValue = if (op1.boolValue == true) 1 else 0)
                }

                var op2 = calculate(value.op2, localVariables)
                if (op2.type == StmtType.BOOL) {
                    op2 = StmtValue(type = StmtType.INT, intValue = if (op2.boolValue == true) 1 else 0)
                }

                if (op1.type == StmtType.INT && op2.type == StmtType.INT) {
                    result = StmtValue(boolValue = (op1.getValue() as Int) != (op2.getValue() as Int))
                }

                if (op1.type == StmtType.STRING && op2.type == StmtType.STRING) {
                    result = StmtValue(boolValue = (op1.getValue() as String) != (op2.getValue() as String))
                }
            }
            is JCmpgExpr -> {
                // TODO
            }
            is JCastExpr -> {
                val op = calculate(value.op, localVariables)
                when (value.type) {
                    PrimitiveType.getDouble() -> {
                        when (op.type) {
                            StmtType.INT -> {
                                result = StmtValue(doubleValue = (op.getValue() as Int).toDouble())
                            }
                            else -> {}
                        }
                    }
                }
            }
            is JVirtualInvokeExpr -> { // call another function
                // prepare arguments
                val funArgs = mutableListOf<Variable>()
                for (i in value.args.indices) {
                    val argValue = calculate(value.args[i], localVariables)
                    funArgs.add(Variable("@parameter${i}", StmtValue(argValue.type), argValue, false))
                }
                result = analyzeMethod(value.methodSignature, funArgs)
            }
        }

        if (result == null) {throw Exception("Method calculate error: the result variable has not been initiated")}

        return result
    }

    /**
     * Retrieve the current values of state variables (transition condition of a state machine)
     */
    private fun getStateVariables(): List<Variable> {
        val condition = mutableListOf<Variable>()
        for (variable in variables) {
            if (variable.isState) {
                condition.add(variable.copy())
            }
        }
        return condition
    }
}
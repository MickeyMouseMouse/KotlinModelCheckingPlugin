package org.example.kotlinmodelcheckingplugin.model

import org.example.kotlinmodelcheckingplugin.model.stmt_value.*
import org.example.kotlinmodelcheckingplugin.model.state_machine.StateMachine
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.constant.BooleanConstant
import sootup.core.jimple.common.constant.DoubleConstant
import sootup.core.jimple.common.constant.IntConstant
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
    private val variables: List<Variable>,
    private val constants: List<Constant>
) {
    private lateinit var view: JavaView
    val stateMachines = mutableListOf<StateMachine>()

    /**
     * Analyse Jimple IR to retrieve finite state machines for variables
     */
    fun buildStateMachines(className: String, jarFile: File): List<StateMachine> {
        val inputLocation = JavaClassPathAnalysisInputLocation(jarFile.absolutePath)
        view = JavaView(inputLocation)
        val classType = view.identifierFactory.getClassType(className)
        val sootClass = view.getClass(classType).get()

        // get the initial values of variables from constructor
        for (method in sootClass.methods) {
            if (method.name == "<init>") {
                for (stmt in method.body.stmts) {
                    if (stmt is JAssignStmt) {
                        try {
                            val varName = stmt.fieldRef.fieldSignature.name // can throw RuntimeException
                            for (variable in variables) {
                                if (variable.name == varName) {
                                    when (variable.value.type) {
                                        StmtType.INT -> {
                                            variable.initValue.intValue = stmt.uses[1].toString().toInt()
                                            variable.value.intValue = stmt.uses[1].toString().toInt()
                                        }
                                        StmtType.DOUBLE -> {
                                            variable.initValue.doubleValue = stmt.uses[1].toString().toDouble()
                                            variable.value.doubleValue = stmt.uses[1].toString().toDouble()
                                        }
                                        StmtType.BOOL -> {
                                            variable.initValue.boolValue = stmt.uses[1].toString().toBoolean()
                                            variable.value.boolValue = stmt.uses[1].toString().toBoolean()
                                        }
                                        StmtType.UNKNOWN -> {}
                                    }
                                    break
                                }
                            }
                        } catch (_: RuntimeException) {
                        }
                    }
                }
            }
        }

        // create finite state machines
        for (variable in variables) {
            stateMachines.add(StateMachine(variable))
        }

        // start the analysis with the 'main()' function (entry point)
        analyseMethod(
            view.identifierFactory.getMethodSignature(classType, "main", "void", listOf()),
            listOf()
        )
        return stateMachines
    }

    /**
     *
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

    /**
     *
     */
    private fun calculate(value: Value, localVariables: List<Variable>): StmtValue {
        var result : StmtValue? = null
        when (value) {
            is IntConstant -> {
                result = StmtValue(intValue = value.toString().toInt())
            }
            is DoubleConstant -> {
                result = StmtValue(doubleValue = value.toString().toDouble())
            }
            is BooleanConstant -> {
                result = StmtValue(boolValue = value.toString().toBoolean())
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
                            StmtType.UNKNOWN -> {}
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
                                StmtType.UNKNOWN -> {}
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
                            StmtType.UNKNOWN -> {}
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
                    funArgs.add(Variable("@parameter${i}", StmtValue(), argValue, false))
                }
                result = analyseMethod(value.methodSignature, funArgs)
            }
        }

        if (result == null) {throw Exception("Method calculate error: the result variable has not been initiated")}

        return result
    }

    /**
     *
     */
    private fun analyseMethod(methodSignature: MethodSignature, args: List<Variable>): StmtValue {
        val method = view.getMethod(methodSignature).get()
        val localVariables = mutableListOf<Variable>()
        var stmt = method.body.stmts[0]
        while (stmt !is JReturnVoidStmt) {
            when (stmt) {
                is JIdentityStmt -> {  // parsing arguments
                    val (argName, argType) = stmt.rightOp.toString().split(": ")
                    for (arg in args) {
                        if (arg.name == argName) {
                            when (argType) {
                                "int" -> {
                                    localVariables.add(Variable(
                                        stmt.leftOp.name,
                                        StmtValue(),
                                        StmtValue(intValue = arg.value.intValue),
                                        false
                                    ))
                                }
                                "double" -> {
                                    localVariables.add(Variable(
                                        stmt.leftOp.name,
                                        StmtValue(),
                                        StmtValue(doubleValue = arg.value.doubleValue),
                                        false
                                    ))
                                }
                                "boolean" -> {
                                    localVariables.add(Variable(
                                        stmt.leftOp.name,
                                        StmtValue(),
                                        StmtValue(boolValue = arg.value.intValue != 0),
                                        false
                                    ))
                                }
                            }
                        }
                    }
                }
                is JAssignStmt -> {
                    val result = calculate(stmt.rightOp, localVariables)
                    when (stmt.leftOp) {
                        is JInstanceFieldRef -> {
                            val varSignature = (stmt.leftOp as JInstanceFieldRef).fieldSignature
                            for (variable in variables) {
                                if (variable.name == varSignature.name) {
                                    when (varSignature.type) {
                                        PrimitiveType.getInt() -> {
                                            val newValue = result.getValue() as Int
                                            stateMachines.filter { it.varName == variable.name }[0].addNextState(
                                                StmtValue(intValue = newValue),
                                                getStateVariables()
                                            )
                                            variable.value.intValue = newValue
                                        }
                                        PrimitiveType.getBoolean() -> {
                                            val newValue = (result.getValue() as Int) == 1
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
                        is JavaLocal -> {
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
                                    StmtValue(),
                                    result,
                                    false
                                )
                            )
                        }
                    }
                }
                is JInvokeStmt -> { // call another function
                    // prepare arguments
                    val funArgs = mutableListOf<Variable>()
                    for (i in stmt.invokeExpr.args.indices) {
                        val argValue = calculate(stmt.invokeExpr.args[i], localVariables)
                        funArgs.add(Variable("@parameter${i}", StmtValue(), argValue, false))
                    }

                    analyseMethod(stmt.invokeExpr.methodSignature, funArgs)
                }
                is JReturnStmt -> {
                    return calculate(stmt.op, localVariables)
                }
            }

            stmt = when (stmt) { // get next stmt
                is JIfStmt -> {
                    if  (calculate(stmt.condition, localVariables).getValue() as Boolean)
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
        return StmtValue()
    }
}
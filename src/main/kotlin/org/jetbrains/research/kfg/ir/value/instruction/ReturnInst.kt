package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.UndefinedName
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.type.Type

class ReturnInst : TerminateInst {
    constructor(id: Int, type: Type) : super(id, UndefinedName, type, arrayOf(), arrayOf())
    constructor(id: Int, retval: Value) : super(id, UndefinedName, retval.type, arrayOf(retval), arrayOf())

    val hasReturnValue: Boolean
        get() = ops.isNotEmpty()

    val returnType: Type
        get() = ops[0].type

    val returnValue: Value
        get() = ops[0]

    override val isTerminate get() = true
    override fun print(): String {
        val sb = StringBuilder()
        sb.append("return")
        if (hasReturnValue) sb.append(" $returnValue")
        return sb.toString()
    }

    override fun clone(): Instruction = when {
        hasReturnValue -> ReturnInst(id, returnValue)
        else -> ReturnInst(id, type)
    }
}

package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.InvalidAccessError
import org.jetbrains.research.kfg.ir.Class
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.UndefinedName
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.ir.value.Name

class CallInst : Instruction {
    val opcode: CallOpcode
    val method: Method
    val `class`: Class
    val isStatic: Boolean

    val callee: Value
        get() = when {
            !isStatic -> ops[0]
            else -> throw InvalidAccessError("Trying to get callee of static call")
        }

    val args: List<Value>
        get() = when {
            isStatic -> ops.toList()
            else -> ops.drop(1)
        }

    constructor(opcode: CallOpcode, method: Method, `class`: Class, args: Array<Value>)
            : super(UndefinedName, method.desc.retval, args) {
        this.opcode = opcode
        this.method = method
        this.`class` = `class`
        this.isStatic = true
    }

    constructor(opcode: CallOpcode, method: Method, `class`: Class, obj: Value, args: Array<Value>)
            : super(UndefinedName, method.desc.retval, arrayOf(obj).plus(args)) {
        this.opcode = opcode
        this.method = method
        this.`class` = `class`
        this.isStatic = false
    }

    constructor(opcode: CallOpcode, name: Name, method: Method, `class`: Class, args: Array<Value>)
            : super(name, method.desc.retval, args) {
        this.opcode = opcode
        this.method = method
        this.`class` = `class`
        this.isStatic = true
    }

    constructor(opcode: CallOpcode, name: Name, method: Method, `class`: Class, obj: Value, args: Array<Value>)
            : super(name, method.desc.retval, arrayOf(obj).plus(args)) {
        this.opcode = opcode
        this.method = method
        this.`class` = `class`
        this.isStatic = false
    }

    override fun print(): String {
        val sb = StringBuilder()
        if (name !== UndefinedName) sb.append("$name = ")

        sb.append("$opcode ")
        if (isStatic) sb.append(`class`.name)
        else sb.append(callee.name)
        sb.append(".${method.name}(")
        args.dropLast(1).forEach { sb.append("$it, ") }
        args.takeLast(1).forEach { sb.append("$it") }
        sb.append(")")
        return sb.toString()
    }

    override fun clone(): Instruction = when {
        isStatic -> CallInst(opcode, name.clone(), method, `class`, args.toTypedArray())
        else -> CallInst(opcode, name.clone(), method, `class`, callee, args.toTypedArray())
    }
}
package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.UndefinedName
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.type.Type

class ExitMonitorInst(id: Int, type: Type, owner: Value)
    : Instruction(id, UndefinedName, type, arrayOf(owner)) {

    val owner: Value
        get() = ops[0]

    override fun print() = "exit monitor $owner"
    override fun clone(): Instruction = ExitMonitorInst(id, type, owner)
}

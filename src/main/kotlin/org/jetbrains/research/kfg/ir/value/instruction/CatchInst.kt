package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.type.Type

class CatchInst(id: Int, name: Name, type: Type) : Instruction(id, name, type, arrayOf()) {
    override fun print() = "$name = catch ${type.name}"
    override fun clone(): Instruction = CatchInst(id, name.clone(), type)
}

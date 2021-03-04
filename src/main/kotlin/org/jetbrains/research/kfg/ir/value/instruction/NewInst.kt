package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.type.Type

class NewInst(id: Int, name: Name, type: Type) : Instruction(id, name, type, arrayOf()) {
    override fun print() = "$name = new ${type.name}"
    override fun clone(): Instruction = NewInst(id, name.clone(), type)
}

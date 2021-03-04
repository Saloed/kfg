package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.type.Type

class InstanceOfInst(id: Int, name: Name, type: Type, val targetType: Type, obj: Value)
    : Instruction(id, name, type, arrayOf(obj)) {

    val operand: Value
        get() = ops[0]

    override fun print() = "$name = $operand instanceOf ${targetType.name}"
    override fun clone(): Instruction = InstanceOfInst(id, name.clone(), type, targetType, operand)
}

package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.type.Type
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.ir.value.Name

class CmpInst(id: Int, name: Name, type: Type, val opcode: CmpOpcode, lhv: Value, rhv: Value)
    : Instruction(id, name, type, arrayOf(lhv, rhv)) {

    val lhv: Value
        get() = ops[0]

    val rhv: Value
        get() = ops[1]

    override fun print() = "$name = ($lhv $opcode $rhv)"
    override fun clone(): Instruction = CmpInst(id, name.clone(), type, opcode, lhv, rhv)
}

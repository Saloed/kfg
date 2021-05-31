package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kthelper.assert.ktassert
import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.type.ArrayType
import org.jetbrains.research.kfg.type.Type

class NewArrayInst(id: Int, name: Name, type: Type, dimensions: Array<Value>): Instruction(id, name, type, dimensions) {
    val component: Type

    val dimensions: List<Value>
        get() = ops.toList()

    val numDimensions: Int
        get() = ops.size

    init {
        var current = type
        repeat(numDimensions) {
            ktassert(current is ArrayType)
            current = (current as ArrayType).component
        }
        this.component = current
    }

    override fun print(): String {
        val sb = StringBuilder()
        sb.append("$name = new ${component.name}")
        dimensions.forEach {
            sb.append("[$it]")
        }
        return sb.toString()
    }
    override fun clone(): Instruction = NewArrayInst(id, name.clone(), type, ops)
}

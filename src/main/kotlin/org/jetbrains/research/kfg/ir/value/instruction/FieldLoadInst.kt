package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.Field
import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kthelper.assert.asserted

class FieldLoadInst : Instruction {
    val field: Field
    val isStatic: Boolean

    val hasOwner: Boolean
        get() = !isStatic

    val owner: Value
        get() = asserted(hasOwner) { ops[0] }

    constructor(id: Int, name: Name, field: Field) : super(id, name, field.type, arrayOf()) {
        this.field = field
        isStatic = true
    }

    constructor(id: Int, name: Name, owner: Value, field: Field) : super(id, name, field.type, arrayOf(owner)) {
        this.field = field
        isStatic = false
    }

    override fun print(): String {
        val sb = StringBuilder()
        sb.append("$name = ")
        if (hasOwner) sb.append("$owner.")
        else sb.append("${field.klass.name}.")
        sb.append(field.name)
        return sb.toString()
    }

    override fun clone(): Instruction = when {
        isStatic -> FieldLoadInst(id, name.clone(), field)
        else -> FieldLoadInst(id, name.clone(), owner, field)
    }
}

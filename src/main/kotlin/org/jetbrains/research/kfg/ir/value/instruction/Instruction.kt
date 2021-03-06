package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Location
import org.jetbrains.research.kfg.ir.value.*
import org.jetbrains.research.kfg.type.Type
import org.jetbrains.research.kthelper.assert.asserted

abstract class Instruction(
    val id: Int,
    name: Name,
    type: Type,
    protected val ops: Array<Value>
) : Value(name, type), ValueUser, Iterable<Value> {

    internal var parentUnsafe: BasicBlock? = null
    var location = Location()
        internal set

    val parent get() = asserted(hasParent) { parentUnsafe!! }
    val hasParent get() = parentUnsafe != null

    open val isTerminate = false

    val operands: List<Value>
        get() = ops.toList()

    init {
        ops.forEach { it.addUser(this) }
    }

    abstract fun print(): String
    override fun iterator(): Iterator<Value> = ops.iterator()

    override fun replaceUsesOf(from: UsableValue, to: UsableValue) {
        ops.indices
            .filter { ops[it] == from }
            .forEach {
                ops[it].removeUser(this)
                ops[it] = to.get()
                to.addUser(this)
            }
    }

    protected abstract fun clone(): Instruction

    open fun update(remapping: Map<Value, Value> = mapOf(), loc: Location = location): Instruction {
        val new = clone()
        remapping.forEach { (from, to) -> new.replaceUsesOf(from, to) }
        new.location = loc
        return new
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Instruction
        return id == other.id
    }

    override fun hashCode(): Int = id
}

abstract class TerminateInst(
    id: Int,
    name: Name,
    type: Type,
    operands: Array<Value>,
    protected val succs: Array<BasicBlock>
) : Instruction(id, name, type, operands), BlockUser {

    val successors: List<BasicBlock>
        get() = succs.toList()

    override val isTerminate = true

    init {
        succs.forEach { it.addUser(this) }
    }

    override fun replaceUsesOf(from: UsableBlock, to: UsableBlock) {
        succs.indices
            .filter { succs[it] == from }
            .forEach {
                succs[it].removeUser(this)
                succs[it] = to.get()
                to.addUser(this)
            }
    }
}

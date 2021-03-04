package org.jetbrains.research.kfg.ir.value.instruction

import com.abdullin.kthelper.assert.asserted
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Location
import org.jetbrains.research.kfg.ir.value.*
import org.jetbrains.research.kfg.type.Type

abstract class Instruction(name: Name, type: Type, protected val ops: Array<Value>)
    : Value(name, type), ValueUser, Iterable<Value> {

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

    private val instructionId = InstructionId()
    internal fun setId(id: Int) {
        instructionId.set(id)
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
        val new = clone().also { it.instructionId.setCopyOf(instructionId) }
        remapping.forEach { (from, to) -> new.replaceUsesOf(from, to) }
        new.location = loc
        return new
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Instruction
        val thisId = instructionId.get()
        val otherId = other.instructionId.get()
        if (thisId != null && otherId != null) return thisId == otherId
        return super.equals(other)
    }

    override fun hashCode(): Int = instructionId.get() ?: super.hashCode()
}

abstract class TerminateInst(name: Name, type: Type, operands: Array<Value>, protected val succs: Array<BasicBlock>) :
        Instruction(name, type, operands), BlockUser {

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

class InstructionId {
    private var id: Int = 0
    private var isInitialized: Boolean = false
    private var isAccessed: Boolean = false

    fun get(): Int? {
        isAccessed = true
        if (isInitialized) return id
        return null
    }

    fun set(id: Int) {
        if (isAccessed) error("Try update instruction id which is already used")
        if (isInitialized) error("Try update instruction id which is already set")
        this.id = id
        isInitialized = true
    }

    fun setCopyOf(other: InstructionId) {
        id = other.id
        isAccessed = other.isAccessed
        isInitialized = other.isInitialized
    }
}

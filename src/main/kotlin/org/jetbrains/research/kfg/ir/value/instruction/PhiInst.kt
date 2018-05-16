package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.value.BlockUser
import org.jetbrains.research.kfg.type.Type
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.ir.value.UsableBlock

class PhiInst(name: Name, type: Type, incomings: Map<BasicBlock, Value>)
    : Instruction(name, type, incomings.values.toTypedArray()), BlockUser {
    private val predecessors = incomings.keys.toTypedArray()

    fun getPredecessors() = predecessors.toList()
    fun getIncomingValues() = operands.toList()
    fun getIncomings() = predecessors.zip(operands).toMap()

    override fun print(): String {
        val sb = StringBuilder()
        sb.append("$name = phi {")
        val incomings = getIncomings().map { it.key to it.value }
        incomings.take(1).forEach { sb.append("${it.first.name} -> ${it.second}") }
        incomings.drop(1).forEach { sb.append("; ${it.first.name} -> ${it.second}") }
        sb.append("}")
        return sb.toString()
    }

    override fun clone(): Instruction = PhiInst(name.clone(), type, getIncomings())

    override fun replaceUsesOf(from: UsableBlock, to: UsableBlock) {
        (0 until predecessors.size)
                .filter { predecessors[it] == from }
                .forEach {
                    predecessors[it].removeUser(this)
                    predecessors[it] = to.get()
                    to.addUser(this)
                }
    }
}
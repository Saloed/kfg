package org.jetbrains.research.kfg.builder.asm

import org.jetbrains.research.kfg.*
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.CatchBlock
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.*
import org.jetbrains.research.kfg.ir.value.instruction.*
import org.jetbrains.research.kfg.type.*
import org.jetbrains.research.kfg.visitor.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

fun typeToFullInt(type: Type) = when (type) {
    is BoolType -> 0
    is ByteType -> 5
    is ShortType -> 7
    is CharType -> 6
    is IntType -> 0
    is LongType -> 1
    is FloatType -> 2
    is DoubleType -> 3
    is Reference -> 4
    else -> throw UnexpectedException("Unexpected type for conversion: ${type.name}")
}

fun typeToInt(type: Type) = when (type) {
    is LongType -> 1
    is Integral -> 0
    is FloatType -> 2
    is DoubleType -> 3
    is Reference -> 4
    else -> throw UnexpectedException("Unexpected type for conversion: ${type.name}")
}

class AsmBuilder(method: Method) : MethodVisitor(method) {
    private val insnLists = mutableMapOf<BasicBlock, InsnList>()
    private val terminateInsns = mutableMapOf<BasicBlock, InsnList>()
    private val labels = method.basicBlocks.map { Pair(it, LabelNode()) }.toMap()
    private val stack = mutableListOf<Value>()
    private val locals = mutableMapOf<Value, Int>()

    var currentInsnList = InsnList()
    var maxLocals = 0
    var maxStack = 0

    init {
        if (!method.isStatic()) {
            val `this` = VF.getThis(TF.getRefType(method.`class`))
            locals[`this`] = getLocalFor(`this`)
        }
        for ((indx, type) in method.argTypes.withIndex()) {
            val arg = VF.getArgument("arg$$indx", method, type)
            locals[arg] = getLocalFor(arg)
        }
    }

    private fun getInsnList(bb: BasicBlock) = insnLists.getOrPut(bb, { InsnList() })
    private fun getTerminateInsnList(bb: BasicBlock) = terminateInsns.getOrPut(bb, { InsnList() })
    private fun stackPop() = stack.removeAt(stack.size - 1)
    private fun stackPush(value: Value): Boolean {
        val res = stack.add(value)
        if (stack.size > maxStack) maxStack = stack.size
        return res
    }
    private fun stackSave(): Unit {
        while (stack.isNotEmpty()) {
            val operand = stackPop()
            val local = getLocalFor(operand)
            val opcode = ISTORE + typeToInt(operand.type)
            val insn = VarInsnNode(opcode, local)
            currentInsnList.add(insn)
        }
    }

    private fun getLocalFor(value: Value) = locals.getOrPut(value, {
        val old = maxLocals
        maxLocals += if (value.type.isDWord()) 2 else 1
        old
    })

    private fun getLabel(bb: BasicBlock) = labels[bb] ?: throw UnexpectedException("Unknown basic block ${bb.name}")

    private fun convertConstantToInsn(`const`: Constant) = when (`const`) {
        is BoolConstant -> InsnNode(if (`const`.value) ICONST_1 else ICONST_0)
        is ByteConstant -> IntInsnNode(BIPUSH, `const`.value.toInt())
        is ShortConstant -> IntInsnNode(SIPUSH, `const`.value.toInt())
        is IntConstant -> when (`const`.value) {
            in -1..5 -> InsnNode(ICONST_0 + `const`.value)
            in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(BIPUSH, `const`.value)
            in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(SIPUSH, `const`.value)
            else -> LdcInsnNode(`const`.value)
        }
        is LongConstant -> when (`const`.value) {
            in 0..1 -> InsnNode(LCONST_0 + `const`.value.toInt())
            else -> LdcInsnNode(`const`.value)
        }
        is FloatConstant -> when (`const`.value) {
            0.0F -> InsnNode(FCONST_0)
            1.0F -> InsnNode(FCONST_1)
            2.0F -> InsnNode(FCONST_2)
            else -> LdcInsnNode(`const`.value)
        }
        is DoubleConstant -> when (`const`.value) {
            0.0 -> InsnNode(DCONST_0)
            1.0 -> InsnNode(DCONST_1)
            else -> LdcInsnNode(`const`.value)
        }
        is NullConstant -> InsnNode(ACONST_NULL)
        is StringConstant -> LdcInsnNode(`const`.value)
        is ClassConstant -> LdcInsnNode(org.objectweb.asm.Type.getType(`const`.type.getAsmDesc()))
        is MethodConstant -> TODO()
        else -> throw UnexpectedException("Unknown constant type $`const`")
    }

    // add all instructions for loading required arguments to stack
    private fun addOperandsToStack(operands: Array<Value>) {
        stackSave()
        for (operand in operands) {
            val insn = when (operand) {
                is Constant -> convertConstantToInsn(operand)
                else -> {
                    val local = getLocalFor(operand)
                    val opcode = ILOAD + typeToInt(operand.type)
                    VarInsnNode(opcode, local)
                }
            }
            currentInsnList.add(insn)
            stackPush(operand)
        }
    }

    override fun visitBasicBlock(bb: BasicBlock) {
        stack.clear()
        currentInsnList = getInsnList(bb)
        super.visitBasicBlock(bb)
    }

    override fun visitArrayLoadInst(inst: ArrayLoadInst) {
        addOperandsToStack(inst.operands)
        val opcode = IALOAD + typeToFullInt(inst.type)
        val insn = InsnNode(opcode)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitArrayStoreInst(inst: ArrayStoreInst) {
        addOperandsToStack(inst.operands)
        val opcode = IASTORE + typeToFullInt(inst.getValue().type)
        val insn = InsnNode(opcode)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
    }

    override fun visitBinaryInst(inst: BinaryInst) {
        addOperandsToStack(inst.operands)
        val opcode = inst.opcode.toAsmOpcode() + typeToInt(inst.type)
        val insn = InsnNode(opcode)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitJumpInst(inst: JumpInst) {
        stackSave()
        currentInsnList = getTerminateInsnList(inst.parent!!)
        val successor = getLabel(inst.successor)
        val insn = JumpInsnNode(GOTO, successor)
        currentInsnList.add(insn)
    }

    override fun visitNewInst(inst: NewInst) {
        val insn = TypeInsnNode(NEW, inst.type.toInternalDesc())
        currentInsnList.add(insn)
        stackPush(inst)
    }

    override fun visitReturnInst(inst: ReturnInst) {
        stackSave()
        currentInsnList = getTerminateInsnList(inst.parent!!)
        addOperandsToStack(inst.operands)
        val opcode = if (inst.hasReturnValue()) IRETURN + typeToInt(inst.getReturnType()) else RETURN
        val insn = InsnNode(opcode)
        currentInsnList.add(insn)
    }

    override fun visitBranchInst(inst: BranchInst) {
        stackSave()
        currentInsnList = getTerminateInsnList(inst.parent!!)
        val jump = JumpInsnNode(GOTO, getLabel(inst.falseSuccessor))
        currentInsnList.add(jump)
    }

    override fun visitCastInst(inst: CastInst) {
        addOperandsToStack(inst.operands)
        val originalType = inst.getOperand().type
        val targetType = inst.type
        val insn = if (originalType.isPrimary() and targetType.isPrimary()) {
            val opcode = when (originalType) {
                is IntType -> when (targetType) {
                    is LongType -> I2L
                    is FloatType -> I2F
                    is DoubleType -> I2D
                    is ByteType -> I2B
                    is CharType -> I2C
                    is ShortType -> I2S
                    else -> throw InvalidOperandException("Invalid cast from ${originalType.name} to ${targetType.name}")
                }
                is LongType -> when (targetType) {
                    is IntType -> L2I
                    is FloatType -> L2F
                    is DoubleType -> L2D
                    else -> throw InvalidOperandException("Invalid cast from ${originalType.name} to ${targetType.name}")
                }
                is FloatType -> when (targetType) {
                    is IntType -> F2I
                    is LongType -> F2L
                    is DoubleType -> F2D
                    else -> throw InvalidOperandException("Invalid cast from ${originalType.name} to ${targetType.name}")
                }
                is DoubleType -> when (targetType) {
                    is IntType -> D2I
                    is LongType -> D2L
                    is FloatType -> D2F
                    else -> throw InvalidOperandException("Invalid cast from ${originalType.name} to ${targetType.name}")
                }
                else -> throw InvalidOperandException("Invalid cast from ${originalType.name} to ${targetType.name}")
            }
            InsnNode(opcode)
        } else {
            TypeInsnNode(CHECKCAST, targetType.toInternalDesc())
        }
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitEnterMonitorInst(inst: EnterMonitorInst) {
        val insn = InsnNode(MONITORENTER)
        currentInsnList.add(insn)
    }

    override fun visitExitMonitorInst(inst: ExitMonitorInst) {
        val insn = InsnNode(MONITOREXIT)
        currentInsnList.add(insn)
    }

    override fun visitNewArrayInst(inst: NewArrayInst) {
        addOperandsToStack(inst.operands)
        val component = inst.compType
        val insn = if (component.isPrimary()) {
            IntInsnNode(NEWARRAY, primaryTypeToInt(component))
        } else {
            TypeInsnNode(ANEWARRAY, component.toInternalDesc())
        }
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitMultiNewArrayInst(inst: MultiNewArrayInst) {
        val insn = MultiANewArrayInsnNode(inst.type.getAsmDesc(), inst.dims)
        currentInsnList.add(insn)
        stackPush(inst)
    }

    override fun visitUnaryInst(inst: UnaryInst) {
        addOperandsToStack(inst.operands)
        val opcode = if (inst.opcode == UnaryOpcode.LENGTH) {
            ARRAYLENGTH
        } else {
            INEG + typeToInt(inst.getOperand().type)
        }
        val insn = InsnNode(opcode)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitThrowInst(inst: ThrowInst) {
        stackSave()
        currentInsnList = getTerminateInsnList(inst.parent!!)
        addOperandsToStack(inst.operands)
        val insn = InsnNode(ATHROW)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
    }

    override fun visitSwitchInst(inst: SwitchInst) {
        stackSave()
        currentInsnList = getTerminateInsnList(inst.parent!!)
        addOperandsToStack(inst.operands)
        val default = getLabel(inst.default)
        val keys = inst.branches.keys.map { (it as IntConstant).value }.toIntArray()
        val labels = inst.branches.values.map { getLabel(it) }.toTypedArray()
        val insn = LookupSwitchInsnNode(default, keys, labels)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
    }

    override fun visitFieldLoadInst(inst: FieldLoadInst) {
        addOperandsToStack(inst.operands)
        val opcode = if (inst.isStatic) GETSTATIC else GETFIELD
        val insn = FieldInsnNode(opcode, inst.field.`class`.getFullname(), inst.field.name, inst.type.getAsmDesc())
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitFieldStoreInst(inst: FieldStoreInst) {
        addOperandsToStack(inst.operands)
        val opcode = if (inst.isStatic) PUTSTATIC else PUTFIELD
        val insn = FieldInsnNode(opcode, inst.field.`class`.getFullname(), inst.field.name, inst.type.getAsmDesc())
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
    }

    override fun visitInstanceOfInst(inst: InstanceOfInst) {
        addOperandsToStack(inst.operands)
        val insn = TypeInsnNode(INSTANCEOF, inst.targetType.toInternalDesc())
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        stackPush(inst)
    }

    override fun visitTableSwitchInst(inst: TableSwitchInst) {
        stackSave()
        currentInsnList = getTerminateInsnList(inst.parent!!)
        addOperandsToStack(arrayOf(inst.getIndex()))
        val min = (inst.getMin() as IntConstant).value
        val max = (inst.getMax() as IntConstant).value
        val default = getLabel(inst.default)
        val labels = inst.branches.map { getLabel(it) }.toTypedArray()
        val insn = TableSwitchInsnNode(min, max, default, *labels)
        currentInsnList.add(insn)
        stackPop()
    }

    override fun visitCallInst(inst: CallInst) {
        addOperandsToStack(inst.operands)
        val opcode = inst.opcode.toAsmOpcode()
        val insn = MethodInsnNode(opcode, inst.`class`.getFullname(), inst.method.name, inst.method.getAsmDesc(), opcode == INVOKEINTERFACE)
        currentInsnList.add(insn)
        inst.operands.forEach { stackPop() }
        if (!inst.type.isVoid()) stackPush(inst)
    }

    override fun visitCmpInst(inst: CmpInst) {
        val isBranch = !((inst.opcode is CmpOpcode.Cmpg || inst.opcode is CmpOpcode.Cmpl)
                || (inst.opcode is CmpOpcode.Eq && inst.getLhv().type is LongType))
        if (isBranch) {
            stackSave()
            currentInsnList = getTerminateInsnList(inst.parent!!)
            val opcode = if (inst.getLhv().type is Reference) {
                when (inst.opcode) {
                    is CmpOpcode.Eq -> IF_ACMPEQ
                    is CmpOpcode.Neq -> IF_ACMPNE
                    else -> throw UnexpectedOpcodeException("Branch cmp opcode ${inst.opcode}")
                }
            } else {
                when (inst.opcode) {
                    is CmpOpcode.Eq -> IF_ICMPEQ
                    is CmpOpcode.Neq -> IF_ICMPNE
                    is CmpOpcode.Lt -> IF_ICMPLT
                    is CmpOpcode.Gt -> IF_ICMPGT
                    is CmpOpcode.Le -> IF_ICMPLE
                    is CmpOpcode.Ge -> IF_ICMPGE
                    else -> throw UnexpectedOpcodeException("Branch cmp opcode ${inst.opcode}")
                }
            }
            addOperandsToStack(inst.operands)
            require(inst.getUsers().size == 1, { "Unsupported usage of cmp inst" })
            require(inst.getUsers().first() is BranchInst, { "Unsupported usage of cmp inst" })
            val branch = inst.getUsers().first() as BranchInst
            val insn = JumpInsnNode(opcode, getLabel(branch.trueSuccessor))
            currentInsnList.add(insn)
            inst.operands.forEach { stackPop() }
        } else {
            addOperandsToStack(inst.operands)
            val opcode = when (inst.opcode) {
                is CmpOpcode.Eq -> LCMP
                is CmpOpcode.Cmpg -> when (inst.getLhv().type) {
                    is FloatType -> FCMPG
                    is DoubleType -> DCMPG
                    else -> throw InvalidOperandException("Non-real operands of CMPG inst")
                }
                is CmpOpcode.Cmpl -> when (inst.getLhv().type) {
                    is FloatType -> FCMPL
                    is DoubleType -> DCMPL
                    else -> throw InvalidOperandException("Non-real operands of CMPL inst")
                }
                else -> throw UnexpectedException("Unknown non-branch cmp inst ${inst.print()}")
            }
            val insn = InsnNode(opcode)
            currentInsnList.add(insn)
            inst.operands.forEach { stackPop() }
            stackPush(inst)
        }
    }

    override fun visitCatchInst(inst: CatchInst) {
        val local = getLocalFor(inst)
        val insn = VarInsnNode(ASTORE, local)
        currentInsnList.add(insn)
    }

    private fun buildPhiInst(inst: PhiInst) {
        val storeOpcode = ISTORE + typeToInt(inst.type)
        val local = getLocalFor(inst)
        for ((bb, value) in inst.getIncomings()) {
            val bbInsns = getInsnList(bb)
            val loadIncoming = when (value) {
                is Constant -> convertConstantToInsn(value)
                else -> {
                    val lcl = getLocalFor(value)
                    val opcode = ILOAD + typeToInt(value.type)
                    VarInsnNode(opcode, lcl)
                }
            }
            bbInsns.add(loadIncoming)
            val insn = VarInsnNode(storeOpcode, local)
            bbInsns.add(insn)
        }
    }

    fun buildTryCatchBlocks() = method.filter { it is CatchBlock }.map {
        it as CatchBlock
        val from = it.throwers.minBy { method.basicBlocks.indexOf(it) }
                ?: throw UnexpectedException("Unknown thrower")
        val to = it.throwers.maxBy { method.basicBlocks.indexOf(it) }
                ?: throw UnexpectedException("Unknown thrower")
        TryCatchBlockNode(getLabel(from), getLabel(method.getNext(to)), getLabel(it), it.exception.toInternalDesc())
    }.toList()

    fun build(): InsnList {
        if (method.name == "view")
            println("stop")
        visit()
        method.flatten().filter { it is PhiInst }.forEach { buildPhiInst(it as PhiInst) }
        val insnList = InsnList()
        for (bb in method.basicBlocks) {
            insnList.add(getLabel(bb))
            insnList.add(getInsnList(bb))
            insnList.add(getTerminateInsnList(bb))
        }
        return insnList
    }
}
package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ClassManager
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Class
import org.jetbrains.research.kfg.ir.Field
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.ir.value.Slot
import org.jetbrains.research.kfg.ir.value.StringName
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.type.ArrayType
import org.jetbrains.research.kfg.type.Type

class InstructionFactory(val cm: ClassManager) {
    private val types get() = cm.type
    private var instructionId = 0

    fun getNewArray(name: String, componentType: Type, count: Value): Instruction =
            getNewArray(StringName(name), types.getArrayType(componentType), count)

    fun getNewArray(name: Name, componentType: Type, count: Value): Instruction =
            NewArrayInst(name, types.getArrayType(componentType), arrayOf(count)).apply { setId(instructionId++) }

    fun getNewArray(componentType: Type, count: Value): Instruction =
            NewArrayInst(Slot(), types.getArrayType(componentType), arrayOf(count)).apply { setId(instructionId++) }

    fun getNewArray(name: String, type: Type, dimensions: Array<Value>): Instruction =
            getNewArray(StringName(name), type, dimensions)

    fun getNewArray(name: Name, type: Type, dimensions: Array<Value>): Instruction =
            NewArrayInst(name, type, dimensions).apply { setId(instructionId++) }

    fun getNewArray(type: Type, dimensions: Array<Value>): Instruction =
        NewArrayInst(Slot(), type, dimensions).apply { setId(instructionId++) }

    fun getArrayLoad(name: String, arrayRef: Value, index: Value): Instruction =
            getArrayLoad(StringName(name), arrayRef, index)

    fun getArrayLoad(name: Name, arrayRef: Value, index: Value): Instruction {
        val type = when {
            arrayRef.type === types.nullType -> types.nullType
            else -> (arrayRef.type as ArrayType).component
        }
        return ArrayLoadInst(name, type, arrayRef, index).apply { setId(instructionId++) }
    }

    fun getArrayLoad(arrayRef: Value, index: Value): Instruction {
        val type = when {
            arrayRef.type === types.nullType -> types.nullType
            else -> (arrayRef.type as ArrayType).component
        }
        return ArrayLoadInst(Slot(), type, arrayRef, index).apply { setId(instructionId++) }
    }

    fun getArrayStore(array: Value, index: Value, value: Value): Instruction =
            ArrayStoreInst(array, types.voidType, index, value).apply { setId(instructionId++) }

    fun getFieldLoad(name: String, field: Field): Instruction = getFieldLoad(StringName(name), field)
    fun getFieldLoad(name: Name, field: Field): Instruction = FieldLoadInst(name, field).apply { setId(instructionId++) }
    fun getFieldLoad(field: Field): Instruction = FieldLoadInst(Slot(), field).apply { setId(instructionId++) }

    fun getFieldLoad(name: String, owner: Value, field: Field): Instruction =
            getFieldLoad(StringName(name), owner, field)

    fun getFieldLoad(name: Name, owner: Value, field: Field): Instruction =
        FieldLoadInst(name, owner, field).apply { setId(instructionId++) }
    fun getFieldLoad(owner: Value, field: Field): Instruction =
        FieldLoadInst(Slot(), owner, field).apply { setId(instructionId++) }

    fun getFieldStore(field: Field, value: Value): Instruction =
        FieldStoreInst(field, value).apply { setId(instructionId++) }
    fun getFieldStore(owner: Value, field: Field, value: Value): Instruction =
        FieldStoreInst(owner, field, value).apply { setId(instructionId++) }


    fun getBinary(name: String, opcode: BinaryOpcode, lhv: Value, rhv: Value): Instruction =
            getBinary(StringName(name), opcode, lhv, rhv)

    fun getBinary(name: Name, opcode: BinaryOpcode, lhv: Value, rhv: Value): Instruction =
            BinaryInst(name, opcode, lhv, rhv).apply { setId(instructionId++) }

    fun getBinary(opcode: BinaryOpcode, lhv: Value, rhv: Value): Instruction =
        BinaryInst(Slot(), opcode, lhv, rhv).apply { setId(instructionId++) }

    fun getCmp(name: String, type: Type, opcode: CmpOpcode, lhv: Value, rhv: Value): Instruction =
            getCmp(StringName(name), type, opcode, lhv, rhv)

    fun getCmp(name: Name, type: Type, opcode: CmpOpcode, lhv: Value, rhv: Value): Instruction =
            CmpInst(name, type, opcode, lhv, rhv).apply { setId(instructionId++) }

    fun getCmp(type: Type, opcode: CmpOpcode, lhv: Value, rhv: Value): Instruction =
            getCmp(Slot(), type, opcode, lhv, rhv)

    fun getCast(name: String, type: Type, obj: Value): Instruction = getCast(StringName(name), type, obj)
    fun getCast(name: Name, type: Type, obj: Value): Instruction =
        CastInst(name, type, obj).apply { setId(instructionId++) }
    fun getCast(type: Type, obj: Value): Instruction =
        CastInst(Slot(), type, obj).apply { setId(instructionId++) }

    fun getInstanceOf(name: String, targetType: Type, obj: Value): Instruction =
            getInstanceOf(StringName(name), targetType, obj)

    fun getInstanceOf(name: Name, targetType: Type, obj: Value): Instruction =
            InstanceOfInst(name, types.boolType, targetType, obj).apply { setId(instructionId++) }

    fun getInstanceOf(targetType: Type, obj: Value): Instruction =
            InstanceOfInst(Slot(), types.boolType, targetType, obj).apply { setId(instructionId++) }

    fun getNew(name: String, type: Type): Instruction = getNew(StringName(name), type)
    fun getNew(name: String, `class`: Class): Instruction = getNew(StringName(name), `class`)
    fun getNew(name: Name, `class`: Class): Instruction = getNew(name, types.getRefType(`class`))
    fun getNew(name: Name, type: Type): Instruction = NewInst(name, type).apply { setId(instructionId++) }
    fun getNew(type: Type): Instruction = NewInst(Slot(), type).apply { setId(instructionId++) }

    fun getUnary(name: String, opcode: UnaryOpcode, obj: Value): Instruction = getUnary(StringName(name), opcode, obj)
    fun getUnary(name: Name, opcode: UnaryOpcode, obj: Value): Instruction {
        val type = if (opcode == UnaryOpcode.LENGTH) types.intType else obj.type
        return UnaryInst(name, type, opcode, obj).apply { setId(instructionId++) }
    }

    fun getUnary(opcode: UnaryOpcode, obj: Value): Instruction {
        val type = if (opcode == UnaryOpcode.LENGTH) types.intType else obj.type
        return UnaryInst(Slot(), type, opcode, obj).apply { setId(instructionId++) }
    }


    fun getEnterMonitor(owner: Value): Instruction = EnterMonitorInst(types.voidType, owner).apply { setId(instructionId++) }
    fun getExitMonitor(owner: Value): Instruction = ExitMonitorInst(types.voidType, owner).apply { setId(instructionId++) }

    fun getJump(successor: BasicBlock): Instruction = JumpInst(types.voidType, successor).apply { setId(instructionId++) }
    fun getBranch(cond: Value, trueSucc: BasicBlock, falseSucc: BasicBlock): Instruction =
            BranchInst(cond, types.voidType, trueSucc, falseSucc).apply { setId(instructionId++) }

    fun getSwitch(key: Value, default: BasicBlock, branches: Map<Value, BasicBlock>): Instruction =
            SwitchInst(key, types.voidType, default, branches).apply { setId(instructionId++) }

    fun getTableSwitch(index: Value, min: Value, max: Value, default: BasicBlock, branches: Array<BasicBlock>): Instruction =
            TableSwitchInst(types.voidType, index, min, max, default, branches).apply { setId(instructionId++) }

    fun getPhi(name: String, type: Type, incomings: Map<BasicBlock, Value>): Instruction =
            getPhi(StringName(name), type, incomings)

    fun getPhi(name: Name, type: Type, incomings: Map<BasicBlock, Value>): Instruction =
        PhiInst(name, type, incomings).apply { setId(instructionId++) }
    fun getPhi(type: Type, incomings: Map<BasicBlock, Value>): Instruction =
        PhiInst(Slot(), type, incomings).apply { setId(instructionId++) }

    fun getCall(opcode: CallOpcode, method: Method, `class`: Class, args: Array<Value>, isNamed: Boolean) =
            when {
                isNamed -> CallInst(opcode, Slot(), method, `class`, args).apply { setId(instructionId++) }
                else -> CallInst(opcode, method, `class`, args).apply { setId(instructionId++) }
            }

    fun getCall(opcode: CallOpcode, method: Method, `class`: Class, obj: Value, args: Array<Value>, isNamed: Boolean) =
            when {
                isNamed -> CallInst(opcode, Slot(), method, `class`, obj, args).apply { setId(instructionId++) }
                else -> CallInst(opcode, method, `class`, obj, args).apply { setId(instructionId++) }
            }

    fun getCall(opcode: CallOpcode, name: String, method: Method, `class`: Class, args: Array<Value>) =
            getCall(opcode, StringName(name), method, `class`, args)

    fun getCall(opcode: CallOpcode, name: Name, method: Method, `class`: Class, args: Array<Value>) =
            CallInst(opcode, name, method, `class`, args).apply { setId(instructionId++) }

    fun getCall(opcode: CallOpcode, name: String, method: Method, `class`: Class, obj: Value, args: Array<Value>) =
            getCall(opcode, StringName(name), method, `class`, obj, args)

    fun getCall(opcode: CallOpcode, name: Name, method: Method, `class`: Class, obj: Value, args: Array<Value>) =
            CallInst(opcode, name, method, `class`, obj, args).apply { setId(instructionId++) }

    fun getCatch(name: String, type: Type): Instruction = getCatch(StringName(name), type)
    fun getCatch(name: Name, type: Type): Instruction =
        CatchInst(name, type).apply { setId(instructionId++) }
    fun getCatch(type: Type): Instruction =
        CatchInst(Slot(), type).apply { setId(instructionId++) }
    fun getThrow(throwable: Value): Instruction =
        ThrowInst(types.voidType, throwable).apply { setId(instructionId++) }

    fun getReturn(): Instruction =
        ReturnInst(types.voidType).apply { setId(instructionId++) }
    fun getReturn(retval: Value): Instruction =
        ReturnInst(retval).apply { setId(instructionId++) }

    fun getUnreachable(): Instruction =
        UnreachableInst(types.voidType).apply { setId(instructionId++) }
}

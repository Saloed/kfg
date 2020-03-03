package org.jetbrains.research.kfg.type

import com.abdullin.kthelper.assert.unreachable
import com.abdullin.kthelper.logging.log
import org.jetbrains.research.kfg.InvalidOpcodeError
import org.jetbrains.research.kfg.InvalidStateError
import org.jetbrains.research.kfg.InvalidTypeDescError
import org.objectweb.asm.Opcodes
import java.util.regex.Pattern

val Type.internalDesc: String
    get() = when {
        this.isPrimary -> this.asmDesc
        this is ClassType -> this.`class`.fullname
        this is ArrayType -> "[${(component as? ClassType)?.asmDesc ?: component.internalDesc}"
        else -> throw InvalidStateError("Unknown type ${this.name}")
    }

fun mergeTypes(tf: TypeFactory, types: Set<Type>): Type? = when {
    tf.nullType in types -> {
        val filtered = types.filterNot { it == tf.nullType }.toSet()
        when {
            filtered.isEmpty() -> tf.nullType
            else -> mergeTypes(tf, filtered)
        }
    }
    types.size == 1 -> types.first()
    types.all { it is Integral } -> types.map { it as Integral }.maxBy { it.width }
    types.all { it is ClassType } -> {
        val classes = types.map { it as ClassType }
        var result = tf.objectType
        for (i in 0..classes.lastIndex) {
            val isAncestor = classes.fold(true) { acc, `class` ->
                acc && classes[i].`class`.isAncestorOf(`class`.`class`)
            }

            if (isAncestor) {
                result = classes[i]
            }
        }
        result
    }
    types.all { it is Reference } -> when {
        types.any { it is ClassType } -> tf.objectType
        types.map { it as ArrayType }.map { it.component }.toSet().size == 1 -> types.first()
        types.all { it is ArrayType } -> {
            val components = types.map { (it as ArrayType).component }.toSet()
            when (val merged = mergeTypes(tf, components)) {
                null -> tf.objectType
                else -> tf.getArrayType(merged)
            }
        }
        else -> tf.objectType
    }
    else -> null
}

fun parseDesc(tf: TypeFactory, desc: String): Type = when (desc[0]) {
    'V' -> tf.voidType
    'Z' -> tf.boolType
    'B' -> tf.byteType
    'C' -> tf.charType
    'S' -> tf.shortType
    'I' -> tf.intType
    'J' -> tf.longType
    'F' -> tf.floatType
    'D' -> tf.doubleType
    'L' -> {
        if (desc.last() != ';') throw InvalidTypeDescError(desc)
        tf.getRefType(desc.drop(1).dropLast(1))
    }
    '[' -> tf.getArrayType(parseDesc(tf, desc.drop(1)))
    else -> throw InvalidTypeDescError(desc)
}

fun parseFrameDesc(tf: TypeFactory, desc: String): Type = when (desc[0]) {
    'V' -> tf.voidType
    'Z' -> tf.boolType
    'B' -> tf.byteType
    'C' -> tf.charType
    'S' -> tf.shortType
    'I' -> tf.intType
    'J' -> tf.longType
    'F' -> tf.floatType
    'D' -> tf.doubleType
    '[' -> tf.getArrayType(parseDesc(tf, desc.drop(1)))
    else -> tf.getRefType(desc)
}

fun parsePrimaryType(tf: TypeFactory, opcode: Int): Type = when (opcode) {
    Opcodes.T_CHAR -> tf.charType
    Opcodes.T_BOOLEAN -> tf.boolType
    Opcodes.T_BYTE -> tf.byteType
    Opcodes.T_DOUBLE -> tf.doubleType
    Opcodes.T_FLOAT -> tf.floatType
    Opcodes.T_INT -> tf.intType
    Opcodes.T_LONG -> tf.longType
    Opcodes.T_SHORT -> tf.shortType
    else -> throw InvalidOpcodeError("PrimaryType opcode $opcode")
}

fun primaryTypeToInt(type: Type): Int = when (type) {
    is CharType -> Opcodes.T_CHAR
    is BoolType -> Opcodes.T_BOOLEAN
    is ByteType -> Opcodes.T_BYTE
    is DoubleType -> Opcodes.T_DOUBLE
    is FloatType -> Opcodes.T_FLOAT
    is IntType -> Opcodes.T_INT
    is LongType -> Opcodes.T_LONG
    is ShortType -> Opcodes.T_SHORT
    else -> throw InvalidOpcodeError("${type.name} is not primary type")
}

fun parseMethodDesc(tf: TypeFactory, desc: String): Pair<Array<Type>, Type> {
    val args = mutableListOf<Type>()
    val pattern = Pattern.compile("\\[*(V|Z|B|C|S|I|J|F|D|(L[a-zA-Z$0-9/_]+;))")
    val matcher = pattern.matcher(desc)
    while (matcher.find()) {
        args.add(parseDesc(tf, matcher.group(0)))
    }
    val returnType = args.last()
    return Pair(args.dropLast(1).toTypedArray(), returnType)
}

private fun parseNamedType(tf: TypeFactory, name: String): Type? = when (name) {
    "null" -> tf.nullType
    "void" -> tf.voidType
    "bool" -> tf.boolType
    "short" -> tf.shortType
    "long" -> tf.longType
    "char" -> tf.charType
    "int" -> tf.intType
    "float" -> tf.floatType
    "double" -> tf.doubleType
    else -> null
}

fun parseStringToType(tf: TypeFactory, name: String): Type {
    var arrCount = 0
    val end = name.dropLastWhile {
        if (it == '[') ++arrCount
        it == '[' || it == ']'
    }
    var subtype = parseNamedType(tf, end) ?: tf.getRefType(end)
    while (arrCount > 0) {
        --arrCount
        subtype = tf.getArrayType(subtype)
    }
    return subtype
}

val Type.expandedBitsize
    get() = when (this) {
        is ClassType -> `class`.fields.fold(0) { acc, field -> acc + field.type.bitsize }
        else -> bitsize
    }

fun parsePrimitiveType(tf: TypeFactory, opcode: Int) = when (opcode) {
    0 -> tf.voidType
    1 -> tf.intType
    2 -> tf.floatType
    3 -> tf.doubleType
    4 -> tf.longType
    5 -> tf.nullType
    6 -> TODO()
    else -> unreachable { log.error("Unknown opcode in primitive type parsing: $opcode") }
}
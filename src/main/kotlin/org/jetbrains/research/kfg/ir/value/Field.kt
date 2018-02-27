package org.jetbrains.research.kfg.ir.value

import org.jetbrains.research.kfg.ir.Class
import org.jetbrains.research.kfg.type.Type

class Field : Value {
    val klass: Class
    val value: Value?
    val isStatic: Boolean

    constructor(name: String, klass: Class, type: Type) : super("${klass.name}.$name", type) {
        this.klass = klass
        this.value = null
        this.isStatic = true
    }

    constructor(name: String, klass: Class, type: Type, obj: Value) : super("$obj.$name", type) {
        this.klass = klass
        this.value = obj
        this.isStatic = false
    }
}
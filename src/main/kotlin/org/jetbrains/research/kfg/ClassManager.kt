package org.jetbrains.research.kfg

import org.jetbrains.research.kfg.builder.cfg.CfgBuilder
import org.jetbrains.research.kfg.builder.cfg.InnerClassNormalizer
import org.jetbrains.research.kfg.container.Container
import org.jetbrains.research.kfg.ir.Class
import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.ir.OuterClass
import org.jetbrains.research.kfg.ir.value.ValueFactory
import org.jetbrains.research.kfg.ir.value.instruction.InstructionFactory
import org.jetbrains.research.kfg.type.TypeFactory
import org.jetbrains.research.kfg.util.Flags
import org.jetbrains.research.kthelper.KtException
import org.jetbrains.research.kthelper.defaultHashCode
import org.objectweb.asm.tree.ClassNode

class Package(name: String) {
    val name: String
    val isConcrete: Boolean = name.lastOrNull() != '*'

    companion object {
        val defaultPackage = Package("*")
        val emptyPackage = Package("")
        fun parse(string: String) = Package(string.replace('.', '/'))
    }

    init {
        this.name = when {
            isConcrete -> name
            else -> name.removeSuffix("*").removeSuffix("/")
        }
    }

    fun isParent(other: Package) = when {
        isConcrete -> this.name == other.name
        else -> other.name.startsWith(this.name)
    }

    fun isChild(other: Package) = other.isParent(this)
    fun isParent(name: String) = isParent(Package(name))
    fun isChild(name: String) = isChild(Package(name))

    override fun toString() = "$name${if (isConcrete) "" else "/*"}"
    override fun hashCode() = defaultHashCode(name, isConcrete)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as Package
        return this.name == other.name && this.isConcrete == other.isConcrete
    }
}

class ClassManager(val config: KfgConfig = KfgConfigBuilder().build()) {
    val value = ValueFactory(this)
    val instruction = InstructionFactory(this)
    val type = TypeFactory(this)

    val flags: Flags get() = config.flags
    val failOnError: Boolean get() = config.failOnError

    private val classes = hashMapOf<String, Class>()
    private val class2container = hashMapOf<Class, Container>()
    private val container2class = hashMapOf<Container, MutableSet<Class>>()

    val concreteClasses get() = classes.values.filterIsInstance<ConcreteClass>().toSet()

    fun initialize(loader: ClassLoader, vararg containers: Container) {
        val container2ClassNode = containers.map { it to it.parse(flags, config.failOnError, loader) }.toMap()
        initialize(container2ClassNode)
    }

    fun initialize(vararg containers: Container) {
        val container2ClassNode = containers.map { it to it.parse(flags) }.toMap()
        initialize(container2ClassNode)
    }

    private fun initialize(container2ClassNode: Map<Container, Map<String, ClassNode>>) {
        for ((container, classNodes) in container2ClassNode) {
            classNodes.forEach { (name, cn) ->
                val klass = ConcreteClass(this, cn)
                classes[name] = klass
                class2container[klass] = container
                container2class.getOrPut(container, ::mutableSetOf).add(klass)
            }
        }
        for (klass in classes.values) {
            InnerClassNormalizer(this).visit(klass)
        }
        classes.values.forEach { it.init() }

        for (klass in classes.values) {
            for (method in klass.allMethods) {
                try {
                    if (!method.isAbstract) CfgBuilder(this, method).build()
                } catch (e: KfgException) {
                    if (failOnError) throw e
                    klass.failingMethods += method
                    method.clear()
                } catch (e: KtException) {
                    if (failOnError) throw e
                    klass.failingMethods += method
                    method.clear()
                }
            }
        }
    }

    operator fun get(name: String): Class = classes.getOrElse(name) {
        val cn = ClassNode()
        cn.name = name
        OuterClass(this, cn)
    }

    fun getByPackage(`package`: Package): List<Class> = concreteClasses.filter { `package`.isParent(it.pkg) }

    fun getSubtypesOf(klass: Class): Set<Class> =
        concreteClasses.filter { it.isInheritorOf(klass) && it != klass }.toSet()

    fun getAllSubtypesOf(klass: Class): Set<Class> {
        val result = mutableSetOf(klass)
        var current = getSubtypesOf(klass)
        do {
            val newCurrent = mutableSetOf<Class>()
            for (it in current) {
                result += it
                newCurrent.addAll(getSubtypesOf(it))
            }
            current = newCurrent
        } while (current.isNotEmpty())
        return result
    }
}
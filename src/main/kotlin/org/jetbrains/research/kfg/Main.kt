package org.jetbrains.research.kfg

import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.util.printBytecode
import org.jetbrains.research.kfg.util.writeJar
import java.util.jar.JarFile

fun main(args: Array<String>) {
    require(args.size > 1, { "Specify input jar file" })
    val jar = JarFile(args[0])
    CM.parseJar(jar)

    val classes = CM.classes.values.filter { it is ConcreteClass }
    for (`class` in classes) {
        println("Visiting class $`class`")
        for ((_, method) in `class`.methods) {
            println("Visiting method $method")
            println("Bytecode: ")
            println(method.mn.printBytecode())
            println(method.print())
            println()
        }
    }

    writeJar(jar)
}
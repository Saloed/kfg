package org.jetbrains.research.kfg.container

import com.abdullin.kthelper.`try`
import com.abdullin.kthelper.tryOrNull
import org.jetbrains.research.kfg.ClassManager
import org.jetbrains.research.kfg.Package
import org.jetbrains.research.kfg.UnsupportedCfgException
import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.util.*
import org.objectweb.asm.tree.ClassNode
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest

class JarContainer(private val file: JarFile, pkg: Package? = null) : Container {
    private val manifest = Manifest()

    constructor(path: Path, `package`: Package?) : this(JarFile(path.toFile()), `package`)
    constructor(path: String, `package`: Package?) : this(Paths.get(path), `package`)
    constructor(path: String, `package`: String) : this(Paths.get(path), Package.parse(`package`))

    override val pkg: Package = pkg ?: commonPackage
    override val name: String get() = file.name
    override val classLoader get() = file.classLoader

    override val commonPackage: Package
        get() {
            val klasses = mutableListOf<String>()
            val enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement() as JarEntry

                if (entry.isClass) {
                    klasses += entry.name
                }

            }
            val commonSubstring = longestCommonPrefix(klasses).dropLastWhile { it != '/' }
            return Package.parse("$commonSubstring*")
        }

    init {
        if (file.manifest != null) {
            for ((key, value) in file.manifest.mainAttributes) {
                manifest.mainAttributes[key] = value
            }
            for ((key, value) in file.manifest.entries) {
                manifest.entries[key] = value
            }
        }
    }

    private fun <T> failSafeAction(failOnError: Boolean, action: () -> T): T? = `try`<T?> {
        action()
    }.getOrElse {
        if (failOnError) throw UnsupportedCfgException("")
        else null
    }

    override fun parse(flags: Flags, failOnError: Boolean, loader: ClassLoader): Map<String, ClassNode> {
        val classes = mutableMapOf<String, ClassNode>()
        val enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry

            if (entry.isClass && pkg.isParent(entry.className)) {
                val classNode = readClassNode(file.getInputStream(entry), flags)

                // need to recompute frames because sometimes original Jar classes don't contain frame info
                val newClassNode = when {
                    classNode.hasFrameInfo -> classNode
                    else -> failSafeAction(failOnError) { classNode.recomputeFrames(loader) }
                } ?: continue
                classes[classNode.name] = newClassNode
            }

        }
        return classes
    }

    override fun unpack(cm: ClassManager, target: Path, unpackAllClasses: Boolean, failOnError: Boolean, loader: ClassLoader) {
        val absolutePath = target.toAbsolutePath()
        val enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry
            if (entry.isManifest) continue

            if (entry.isClass) {
                val `class` = cm[entry.name.removeSuffix(".class")]
                when {
                    pkg.isParent(entry.name) && `class` is ConcreteClass -> {
                        val localPath = "${`class`.fullname}.class"
                        val path = "$absolutePath/$localPath"
                        failSafeAction(failOnError) { `class`.write(cm, loader, path, Flags.writeComputeFrames) }
                    }
                    unpackAllClasses -> {
                        val path = "$absolutePath/${entry.name}"
                        val classNode = readClassNode(file.getInputStream(entry))
                        failSafeAction(failOnError) { classNode.write(loader, path, Flags.writeComputeNone) }
                    }
                }
            }
        }
    }

    override fun update(cm: ClassManager, target: Path, loader: ClassLoader): JarContainer {
        val absolutePath = target.toAbsolutePath()
        val jarName = file.name.substringAfterLast('/').removeSuffix(".jar")
        val builder = JarBuilder("$absolutePath/$jarName.jar", manifest)
        val enumeration = file.entries()

        unpack(cm, target, false, false, loader)

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry
            if (entry.isManifest) continue

            if (entry.isClass && pkg.isParent(entry.name)) {
                val `class` = cm[entry.name.removeSuffix(".class")]

                if (`class` is ConcreteClass) {
                    val localPath = "${`class`.fullname}.class"
                    val path = "$absolutePath/$localPath"

                    val newEntry = JarEntry(localPath.replace("\\", "/"))
                    builder.add(newEntry, FileInputStream(path))
                } else {
                    builder.add(entry, file.getInputStream(entry))
                }
            } else {
                builder.add(entry, file.getInputStream(entry))
            }
        }
        builder.close()
        return JarContainer(builder.name, pkg)
    }

}
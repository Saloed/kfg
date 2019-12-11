package org.jetbrains.research.kfg

import org.apache.commons.cli.*
import org.jetbrains.research.kfg.util.Flags
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

data class KfgConfig(
        val flags: Flags = Flags.readCodeOnly,
        val `package`: Package = Package.defaultPackage,
        val failOnError: Boolean = true,
        val ignoreIncorrectClasses: Boolean = false
)

class KfgConfigBuilder private constructor(private val current: KfgConfig) {
    constructor() : this(KfgConfig())

    fun flags(flags: Flags) = KfgConfigBuilder(current.copy(flags = flags))
    fun `package`(`package`: Package) = KfgConfigBuilder(current.copy(`package` = `package`))
    fun failOnError(value: Boolean) = KfgConfigBuilder(current.copy(failOnError = value))
    fun ignoreIncorrectClasses(value: Boolean) = KfgConfigBuilder(current.copy(ignoreIncorrectClasses = value))

    fun build() = current
}

class KfgConfigParser(args: Array<String>) {
    private val options = Options()
    private val cmd: CommandLine

    init {
        setupOptions()

        val parser = DefaultParser()
        cmd = try {
            parser.parse(options, args)
        } catch (e: ParseException) {
            printHelp()
            exitProcess(1)
        }
    }

    private fun setupOptions() {
        val jarOpt = Option("j", "jar", true, "input jar file path")
        jarOpt.isRequired = true
        options.addOption(jarOpt)

        val packageOpt = Option("p", "package", true, "analyzed package")
        packageOpt.isRequired = false
        options.addOption(packageOpt)
    }

    fun getStringValue(param: String) = cmd.getOptionValue(param)!!

    fun getOptionalValue(param: String): String? = cmd.getOptionValue(param)
    fun getStringValue(param: String, default: String) = getOptionalValue(param) ?: default

    private fun printHelp() {
        val helpFormatter = HelpFormatter()
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        helpFormatter.printHelp(pw, 80, "kfg", null, options, 1, 3, null)

        println("$sw")
    }
}

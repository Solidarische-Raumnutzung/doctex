package de.mr_pine.doctex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class Cli : CliktCommand(name = "doctex") {
    val sourcedir by argument().file(mustExist = true, canBeFile = false, mustBeReadable = true)
    val rootPackage by argument()
    val output by option().file(canBeDir = false, mustBeWritable = true).default(File("./documentation.tex"))
    override fun run() {
        val doctex = DocTeX(sourcedir)
        doctex.writeJavadoc(output, rootPackage)
    }
}

fun main(args: Array<String>) = Cli().main(args)
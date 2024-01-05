package de.mr_pine.doctex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class Cli : CliktCommand(name = "doctex") {
    private val sourceDir by argument(help = "The directory containing all source files the documentation should be generated for").file(mustExist = true, canBeFile = false, mustBeReadable = true)
    private val rootPackage by argument(help = "The package containing all subpackages and classes the documentation should be generated for")
    private val output by option().file(canBeDir = false, mustBeWritable = true).default(File("./documentation.tex"))
    private val minimumVisibility by option().enum<Visibility>().default(Visibility.PROTECTED)

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
        }
    }
    override fun run() {
        val doctex = DocTeX(sourceDir)
        doctex.writeJavadoc(output, rootPackage, minimumVisibility)
    }
}

fun main(args: Array<String>) = Cli().main(args)
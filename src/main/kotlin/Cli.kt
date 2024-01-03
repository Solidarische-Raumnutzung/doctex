package de.mr_pine.doctex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

class Cli : CliktCommand(name = "doctex") {
    val sourcedir by argument().file(mustExist = true, canBeFile = false, mustBeReadable = true)
    override fun run() {
        val doctex = DocTeX(sourcedir)
    }
}

fun main(args: Array<String>) = Cli().main(args)
package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.DocTeXLauncher
import de.mr_pine.doctex.spoon.LaTeXGenerator
import spoon.OutputType
import spoon.javadoc.api.elements.JavadocCommentView
import spoon.javadoc.api.parsing.JavadocParser
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtPackage
import spoon.reflect.declaration.CtType
import spoon.support.compiler.FileSystemFolder
import java.io.File

class DocTeX(private val sourcedir: File) {
    private val launcher = DocTeXLauncher().apply {
        environment.apply {
            setShouldCompile(false)
            disableConsistencyChecks()
            outputType = OutputType.NO_OUTPUT
            setCommentEnabled(true)
        }
        addInputResource(FileSystemFolder(sourcedir))
    }
    private val model: CtModel = launcher.buildModel()

    fun writeJavadoc(to: File, forPackage: String) {
        val templateText = DocTeX::class.java.getResource("/template.tex")!!.readText()
        val rootPackage = model.allPackages.find { it.qualifiedName == forPackage } ?: throw Exception("Invalid root package provided. Available packages: ${model.allPackages}")
        val packagesDocumentation = rootPackage.resolveAllPackages().map(CtPackage::generateDocs)

        val documentation = templateText.replace("TEXT", packagesDocumentation.joinToString("\n".repeat(3)))
        to.writeText(documentation)
    }

    private fun CtPackage.resolveAllPackages(): List<CtPackage> = packages.flatMap { listOf(it) + it.resolveAllPackages() }
}

private fun CtPackage.generateDocs(): String {
    val generator = LaTeXGenerator()
    accept(generator)
    return generator.toString()
}

package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.inPackage
import de.mr_pine.doctex.spoon.javadoc.JavadocLaTeXConverter
import spoon.javadoc.api.elements.JavadocCommentView
import spoon.javadoc.api.parsing.JavadocParser
import spoon.reflect.declaration.CtPackage
import spoon.reflect.declaration.CtType
import spoon.reflect.reference.CtArrayTypeReference
import spoon.reflect.reference.CtTypeReference

class LaTeXBuilder(private val rootPackage: CtPackage) {
    private var indentedBuilders = Stack<StringBuilder>().apply { push(StringBuilder()) }
    private val stringBuilder
        get() = indentedBuilders.peek()
    private var sectionDepth = 0
    private val javadocConverter = JavadocLaTeXConverter()

    fun appendPackageSection(packageName: String, content: LaTeXContent) {
        appendSection("Package $packageName", content)
    }

    fun <T : Any?> appendTypeSection(typeType: String, type: CtType<T>, content: LaTeXContent) {
        val header: LaTeXContent = {
            appendText("$typeType ${type.simpleName}")
            appendCommand("label", type.qualifiedName)
        }

        val signature = teletyped {
            appendText(type.visibility.toString())
            appendText(" ")
            appendText(typeType.lowercase())
            appendText(" ")
            appendTypeSignature(type)
        }

        val docElements = JavadocParser.forElement(type)
        val javadoc = JavadocCommentView(docElements)

        appendSection(header) {
            appendTable {
                addRow(emphasized("Signature"), signature)
                if (docElements.isNotEmpty()) {
                    separator()
                    addRow(emphasized("Behaviour"), javadocConverter.convertElements(javadoc.body))
                }
            }
            this.content()
        }
    }

    private fun <T : Any?> appendTypeSignature(type: CtType<T>) {
        appendTypeReference(type.reference)
        if (type.isParameterized) {
            appendTypeParameters(type.formalCtTypeParameters) {
                appendTypeSignature(it)
            }
        }
        if (type.superclass != null && !type.isEnum && type.superclass.qualifiedName != Record::class.qualifiedName) {
            appendText(" extends ")
            appendTypeReference(type.superclass)
        }
        if (type.superInterfaces.isNotEmpty()) {
            appendText(" implements ")
            val references = type.superInterfaces.map {
                val reference: LaTeXContent = { appendTypeReference(it) }
                reference
            }.intersperse { appendText(", ") }
            references.forEach { this.it() }
        }
    }

    fun <T : Any?> appendTypeReference(reference: CtTypeReference<T>) {
        teletype {
            if (reference.typeDeclaration?.inPackage(rootPackage) != true) {
                appendText(reference.simpleName)
                return@teletype
            }

            if (reference.isArray) {
                val component = (reference as CtArrayTypeReference).componentType
                appendTypeReference(component)
                appendText("[]")
                return@teletype
            }

            if (reference.isParameterized) {
                appendTypeReference(reference.typeErasure)
                appendTypeParameters(reference.actualTypeArguments) { appendTypeReference(it) }
                return@teletype
            }

            appendCommand("hyperref[${reference.qualifiedName}]") {
                teletype {
                    appendText(reference.simpleName)
                }
            }
        }
    }

    private fun <T> appendTypeParameters(
        parameterReferences: Collection<T>,
        appender: LaTeXBuilder.(T) -> Unit
    ) {
        appendText("<")
        val parameters = parameterReferences.map { val content: LaTeXContent = { this.appender(it) }; content }.intersperse { appendText(", ") }
        parameters.forEach { this.it() }
        appendText(">")
    }

    private fun appendSection(name: String, content: LaTeXContent) =
        appendSection({ appendText(name) }, content)

    private fun appendSection(name: LaTeXContent, content: LaTeXContent) {
        val sectionCommand = when (sectionDepth) {
            in (0..2) -> "${"sub".repeat(sectionDepth)}section"
            3 -> "paragraph"
            else -> "subparagraph"
        }
        sectionDepth++
        appendCommandWithArgAppender(sectionCommand, listOf(name))
        appendLine()
        inEnvironment("adjustwidth", listOf("1.5em", "0pt"), content)
        sectionDepth--
    }

    fun inEnvironment(name: String, arguments: List<String>, content: LaTeXContent) {
        appendCommand("begin", listOf(name) + arguments)
        appendLine()

        indentedBuilders.push(StringBuilder())
        this.content()

        val builder = indentedBuilders.pop()
        stringBuilder.append(builder.toString().prependIndent(INDENT))

        appendLine()
        appendCommand("end", name)
    }

    fun appendCommand(name: String, argument: String) = appendCommand(name, listOf(argument))
    fun appendCommand(name: String, arguments: List<String>) {
        val mappedArgs: List<LaTeXContent> = arguments.map { { appendText(it) } }
        appendCommandWithArgAppender(name, mappedArgs)
    }

    private fun appendCommand(name: String, argument: LaTeXContent) =
        appendCommandWithArgAppender(name, listOf(argument))

    private fun appendCommandWithArgAppender(name: String, arguments: List<LaTeXContent>) {
        appendText("\\$name")
        arguments.forEach {
            appendText("{")
            this.it()
            appendText("}")
        }
    }

    fun appendText(text: String): LaTeXBuilder {
        stringBuilder.append(text)
        return this
    }

    fun appendLine() = appendText("\n")

    fun build(): String {
        var previousBuilder = indentedBuilders.pop()
        while (indentedBuilders.isNotEmpty()) {
            stringBuilder.appendLine()
            stringBuilder.appendLine(previousBuilder.toString().prependIndent(INDENT))
            previousBuilder = indentedBuilders.pop()
        }
        return previousBuilder.toString()
    }

    private fun appendTable(content: LaTeXTable.() -> Unit) {
        val table = LaTeXTable()
        table.content()
        table.separator()
        inEnvironment("tabularx", listOf("0.9\\textwidth", "@{}l R@{}")) {
            table.rows.forEach { (useSeparator, columns) ->
                columns.intersperse { appendText(" & ") }.forEach { this.it() }
                if (useSeparator) {
                    appendText(" \\\\")
                }
                appendLine()
            }
        }
    }

    private class LaTeXTable {
        val rows: MutableList<Pair<Boolean, List<LaTeXContent>>> = mutableListOf()

        fun addRow(vararg columns: LaTeXContent) {
            rows.add(true to columns.toList())
        }

        fun separator() = rows.add(false to listOf { appendCommand("hline", listOf()) })
    }

    private fun emphasized(content: LaTeXContent): LaTeXContent =
        { appendCommandWithArgAppender("emph", listOf(content)) }

    private fun emphasized(content: String): LaTeXContent = { appendCommand("emph", content) }

    private fun teletyped(content: LaTeXContent): LaTeXContent = { teletype(content) }

    private fun teletyped(content: String): LaTeXContent = teletyped { appendText(content) }
    private fun teletype(content: LaTeXContent) = appendCommandWithArgAppender("texttt", listOf(content))

    companion object {
        const val INDENT = "  "
    }
}

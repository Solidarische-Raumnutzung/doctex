package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.inPackage
import de.mr_pine.doctex.spoon.javadoc.JavadocLaTeXConverter
import spoon.javadoc.api.StandardJavadocTagType
import spoon.javadoc.api.elements.JavadocCommentView
import spoon.javadoc.api.elements.JavadocReference
import spoon.javadoc.api.elements.JavadocText
import spoon.javadoc.api.parsing.JavadocParser
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.declaration.CtModifiable
import spoon.reflect.declaration.CtPackage
import spoon.reflect.declaration.CtType
import spoon.reflect.reference.CtArrayTypeReference
import spoon.reflect.reference.CtReference
import spoon.reflect.reference.CtTypeReference
import kotlin.jvm.optionals.getOrNull

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
            appendModifiers(type)
            appendText(" ")
            appendText(typeType.lowercase())
            appendText(" ")
            appendTypeSignature(type)
        }

        val docElements = JavadocParser.forElement(type)
        val javadoc = JavadocCommentView(docElements)

        appendSection(header) {
            appendTable(1.0) {
                addRow(emphasized("Signature"), signature)
                separator()
                if (docElements.isNotEmpty()) {
                    addRow(emphasized("Behaviour"), javadocConverter.convertElements(javadoc.body))
                    separator()
                }
                parameterJavadoc(javadoc)?.let {
                    addRow(emphasized("Parameters"), it)
                    separator()
                }
            }
            this.content()
        }
    }

    fun <T : Any?, E> appendExecutableSection(
        executable: E,
        headerText: String
    ) where E : CtExecutable<T>, E : CtModifiable {
        val signature: LaTeXContent = teletyped {
            appendModifiers(executable)
            appendTypeReference(executable.type)
            if (!executable.reference.isConstructor) {
                appendText(" ")
                appendText(executable.simpleName)
            }
            appendExecutableParameters(executable)
        }

        val docElements = JavadocParser.forElement(executable)
        val javadoc = JavadocCommentView(docElements)



        val javadocReturns = javadoc.getBlockTag(StandardJavadocTagType.RETURN)
        val returns: LaTeXContent = {
            val returnDoc = javadocReturns.firstOrNull()
            if (returnDoc != null) {
                javadocConverter.visitBlockTag(returnDoc)()
            }
        }

        val javadocThrows = javadoc.getBlockTag(StandardJavadocTagType.THROWS)
        val throws: LaTeXContent = {
            appendTable(1.0) {
                for (thrown in javadocThrows) {
                    val throwable = thrown.getArgument(JavadocReference::class.java)
                    val typeref: LaTeXContent = {
                        teletype {
                            if (throwable.isPresent) {
                                appendReference(throwable.get().reference)
                            } else {
                                appendText("unknown")
                            }
                        }
                        appendText(":")
                    }
                    val description = javadocConverter.visitBlockTag(thrown, 1)
                    addRow(typeref, description)
                }
            }
        }

        appendSection(headerText) {
            appendTable {
                addRow(emphasized("Signature"), signature)
                separator()
                if (javadoc.body.isNotEmpty()) {
                    addRow(emphasized("Behaviour"), javadocConverter.convertElements(javadoc.body))
                    separator()
                }
                if (javadocReturns.isNotEmpty()) {
                    addRow(emphasized("Returns"), returns)
                    separator()
                }
                parameterJavadoc(javadoc)?.let {
                    addRow(emphasized("Parameters"), it)
                    separator()
                }
                if (javadocThrows.isNotEmpty()) {
                    addRow(emphasized("Throws"), throws)
                    separator()
                }
            }
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

    private fun appendReference(reference: CtReference) = when (reference) {
        is CtTypeReference<*> -> appendTypeReference(reference)
        else -> teletyped(reference.simpleName)()
    }

    fun <T : Any?> appendTypeReference(reference: CtTypeReference<T>) {
        teletype {

            if (reference.isPrimitive || reference.typeDeclaration?.inPackage(rootPackage) != true) {
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
        val parameters = parameterReferences.map { val content: LaTeXContent = { this.appender(it) }; content }
            .intersperse { appendText(", ") }
        parameters.forEach { this.it() }
        appendText(">")
    }

    private fun <T> appendExecutableParameters(executable: CtExecutable<T>) {
        val parameters = executable.parameters
        val parameterAppenders: List<LaTeXContent> = parameters.map {
            val type = it.type
            val name = it.simpleName
            {
                appendTypeReference(type)
                appendText(" ")
                appendText(name)
            }
        }

        appendText("(")
        parameterAppenders.intersperse { appendText(", ") }.forEach { this.it() }
        appendText(")")
    }

    private fun appendModifiers(test: CtModifiable) {
        val modifiers = test.modifiers
        for (modifier in modifiers) {
            appendText(modifier.toString())
            appendText(" ")
        }
    }

    private fun parameterJavadoc(javadoc: JavadocCommentView): LaTeXContent? {
        val javadocParameters = javadoc.getBlockTag(StandardJavadocTagType.PARAM)
        if(javadocParameters.isEmpty()) {
            return null
        }
        val parameters: LaTeXContent = {
            appendTable(1.0) {
                for (parameter in javadocParameters) {
                    val name: LaTeXContent = {
                        val argName = parameter.getArgument(JavadocText::class.java).getOrNull()?.text ?: "unknown"
                        teletype { appendText(argName) }
                        appendText(":")
                    }
                    val docComment = javadocConverter.visitBlockTag(parameter, 1)
                    addRow(name, docComment)
                }
            }
        }
        return parameters
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
        inEnvironment("adjustwidth", listOf("1.5em", "0pt"), content = content)
        sectionDepth--
    }

    fun inEnvironment(
        name: String,
        arguments: List<String> = listOf(),
        optionalArguments: List<String> = listOf(),
        content: LaTeXContent
    ) {
        appendCommand("begin", listOf(name) + arguments, optionalArguments)
        appendLine()

        indentedBuilders.push(StringBuilder())
        this.content()

        val builder = indentedBuilders.pop()
        stringBuilder.append(builder.toString().prependIndent(INDENT))

        appendLine()
        appendCommand("end", name)
    }

    fun appendCommand(name: String, argument: String) = appendCommand(name, listOf(argument))
    fun appendCommand(name: String, arguments: List<String>, optionalArguments: List<String> = listOf()) {
        val mappedArgs: List<LaTeXContent> = arguments.map { { appendText(it) } }
        val mappedOptions: List<LaTeXContent> = optionalArguments.map { { appendText(it) } }
        appendCommandWithArgAppender(name, mappedArgs, mappedOptions)
    }

    private fun appendCommand(name: String, argument: LaTeXContent) =
        appendCommandWithArgAppender(name, listOf(argument))

    private fun appendCommandWithArgAppender(
        name: String,
        arguments: List<LaTeXContent>,
        optionalArguments: List<LaTeXContent> = listOf()
    ) {
        appendText("\\$name")
        arguments.forEach {
            appendText("{")
            this.it()
            appendText("}")
        }
        optionalArguments.forEach {
            appendText("[")
            this.it()
            appendText("]")
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

    private fun appendTable(width: Double = 1.0, content: LaTeXTable.() -> Unit) {
        val table = LaTeXTable()
        table.content()
        appendText("{")
        inEnvironment("tabularx", listOf("$width\\linewidth", "@{}l R@{}")) {
            table.rows.forEach { (useSeparator, columns) ->
                columns.intersperse { appendText(" & ") }.forEach { this.it() }
                if (useSeparator) {
                    appendText(" \\\\")
                }
                appendLine()
            }
        }
        appendText("}")
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
    fun teletype(content: LaTeXContent) = appendCommandWithArgAppender("texttt", listOf(content))

    companion object {
        const val INDENT = "  "
    }
}

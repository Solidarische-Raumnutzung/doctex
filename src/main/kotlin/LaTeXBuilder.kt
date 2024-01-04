package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.inPackage
import de.mr_pine.doctex.spoon.javadoc.JavadocLaTeXConverter
import spoon.javadoc.api.StandardJavadocTagType
import spoon.javadoc.api.elements.JavadocCommentView
import spoon.javadoc.api.elements.JavadocReference
import spoon.javadoc.api.elements.JavadocText
import spoon.javadoc.api.parsing.JavadocParser
import spoon.reflect.declaration.*
import spoon.reflect.reference.*
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
            appendText("$typeType ${type.simpleName.escape()}")
            appendCommand("label", type.qualifiedName)
        }

        val signature = teletyped {
            appendModifiers(type)
            appendText(" ")
            appendText(typeType.lowercase())
            appendText(" ")
            appendTypeReference(type.reference, true)
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
        val header: LaTeXContent = {
            appendText(headerText.escape())
            appendCommand(
                "label",
                "${(executable.parent as CtType<*>).qualifiedName}$MEMBER_SEPARATOR${executable.signature}"
            )
        }

        val signature: LaTeXContent = teletyped {
            appendModifiers(executable)
            appendTypeReference(executable.type)
            if (!executable.reference.isConstructor) {
                appendText(" ")
                appendText(executable.simpleName.escape())
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

        appendSection(header) {
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

    fun <T : Any?> appendFieldSection(field: CtField<T>, fieldType: String) {
        val of = (field.parent as CtType<*>)
        val header: LaTeXContent = {
            appendText("$fieldType ${field.simpleName.escape()}")
            appendCommand(
                "label",
                "${of.qualifiedName}$MEMBER_SEPARATOR${field.simpleName}"
            )
        }

        val signature = teletyped {
            appendModifiers(field)
            appendTypeReference(of.reference)
            appendText(" ")
            appendText(field.simpleName.escape())
        }

        val docElements = JavadocParser.forElement(field)
        val javadoc = JavadocCommentView(docElements)

        appendSection(header) {
            appendTable {
                addRow(emphasized("Signature"), signature)
                separator()
                if (javadoc.body.isNotEmpty()) {
                    addRow(emphasized("Behaviour"), javadocConverter.convertElements(javadoc.body))
                    separator()
                }
            }
        }
    }

    fun appendReference(reference: CtReference) = when (reference) {
        is CtTypeReference<*> -> appendTypeReference(reference)
        is CtExecutableReference<*> -> appendExecutableReference(reference)
        is CtFieldReference<*> -> appendFieldReference(reference)
        else -> teletyped(reference.simpleName)()
    }

    private fun <T : Any?> appendTypeReference(reference: CtTypeReference<T>, showSuper: Boolean = false) {
        teletype {

            if (reference.isPrimitive || reference.typeDeclaration?.inPackage(rootPackage) != true) {
                appendText(reference.simpleName.escape())
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
                appendTypeParameters(reference.actualTypeArguments)
                return@teletype
            }

            appendCommand("hyperref[${reference.qualifiedName}]") {
                teletype {
                    appendText(reference.simpleName.escape())
                }
            }

            if (reference.isParameterized) {
                appendTypeParameters(reference.actualTypeArguments)
            }

            if (showSuper && reference.superclass != null && !reference.isEnum && reference.superclass.qualifiedName != Record::class.qualifiedName) {
                appendText(" extends ")
                appendTypeReference(reference.superclass)
            }

            if (showSuper && reference.superInterfaces.isNotEmpty()) {
                appendText(" implements ")
                val references = reference.superInterfaces.map {
                    val referenceText: LaTeXContent = { appendTypeReference(it) }
                    referenceText
                }.intersperse { appendText(", ") }
                references.forEach { this.it() }
            }
        }
    }

    private fun <T : Any?> appendExecutableReference(reference: CtExecutableReference<T>) {
        teletype {
            val declaring = reference.declaringType
            appendTypeReference(declaring)
            val hyperref =
                "hyperref[${declaring.qualifiedName}$MEMBER_SEPARATOR${reference.executableDeclaration.signature}]"
            if (!reference.isConstructor) {
                appendText(".")
                appendCommand(hyperref, reference.simpleName.escape())
            }
            if (reference.parameters.isNotEmpty() || reference.executableDeclaration.parameters.isEmpty()) {
                appendCommand(hyperref, "(")
                appendTypeParameters(reference.parameters, "" to "")
                appendCommand(hyperref, ")")
            }
        }
    }

    private fun <T : Any?> appendFieldReference(reference: CtFieldReference<T>) {
        teletype {
            val declaring = reference.declaringType
            appendTypeReference(declaring)
            val hyperref = "hyperref[${declaring.qualifiedName}$MEMBER_SEPARATOR${reference.simpleName}]"
                .replace("_", "~")
            appendCommand(hyperref, reference.simpleName.escape())
        }
    }

    private fun appendTypeParameters(
        parameterReferences: Collection<CtTypeReference<*>>,
        delimiters: Pair<String, String> = "<" to ">"
    ) {
        appendText(delimiters.first)
        val parameters = parameterReferences.map { val content: LaTeXContent = { appendTypeReference(it) }; content }
            .intersperse { appendText(", ") }
        parameters.forEach { this.it() }
        appendText(delimiters.second)
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
        if (javadocParameters.isEmpty()) {
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
        private const val INDENT = "  "
        private const val MEMBER_SEPARATOR = "@"
        private fun String.escape() = replace("_", "\\_")
    }
}

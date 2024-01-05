package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.inPackage
import de.mr_pine.doctex.spoon.javadoc.JavadocLaTeXConverter
import de.mr_pine.doctex.spoon.javadoc.JavadocReferenceParentVisitor
import spoon.javadoc.api.StandardJavadocTagType
import spoon.javadoc.api.elements.JavadocCommentView
import spoon.javadoc.api.elements.JavadocReference
import spoon.javadoc.api.elements.JavadocText
import spoon.javadoc.api.parsing.InheritanceResolver
import spoon.javadoc.api.parsing.JavadocParser
import spoon.reflect.cu.position.NoSourcePosition
import spoon.reflect.declaration.*
import spoon.reflect.reference.*
import java.io.File
import kotlin.jvm.optionals.getOrNull

class LaTeXBuilder(
    private val rootPackage: CtPackage,
    private val inheritDoc: Boolean,
    private val sourceRoot: File,
    private val gitlabSourceRoot: String?
) {
    private var indentedBuilders = Stack<StringBuilder>().apply { push(StringBuilder()) }
    private val stringBuilder
        get() = indentedBuilders.peek()
    private var sectionDepth = 0
    private val javadocConverter = JavadocLaTeXConverter()
    private val inheritanceResolver = InheritanceResolver()

    fun appendPackageSection(pkg: CtPackage, content: LaTeXContent) {
        appendSection("Package ${pkg.qualifiedName}", content)
    }

    fun <T : Any?> appendTypeSection(typeType: String, type: CtType<T>, content: LaTeXContent) {
        val header: LaTeXContent = {
            appendText("$typeType ${type.simpleName.escape()}")
            appendCommand("label", type.qualifiedName)
            appendText(" ")
            appendGitlabLink(type)
        }

        val signature = teletyped {
            appendModifiers(type)
            appendText(" ")
            appendText(typeType.lowercase())
            appendText(" ")
            appendTypeReference(type.reference, true)
        }

        val javadoc = type.javadoc

        appendSection(header) {
            appendTable(1.0) {
                addRow(emphasized("Signature"), signature)
                separator()
                if (javadoc.body.isNotEmpty()) {
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
            appendGitlabLink(executable)
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

        val javadoc = executable.javadoc

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
                    val typeRef: LaTeXContent = {
                        teletype {
                            if (throwable.isPresent) {
                                appendReference(throwable.get().reference)
                            } else {
                                appendText(thrown.getArgument(JavadocText::class.java).getOrNull()?.text ?: "unknown")
                            }
                        }
                        appendText(":")
                    }
                    val description = javadocConverter.visitBlockTag(thrown, 1)
                    addRow(typeRef, description)
                }
            }
        }

        val overrides: LaTeXContent? = executable.getParentExecutable()?.let { parent ->
            {
                appendExecutableReference(parent.reference, true)
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
                overrides?.let {
                    addRow(emphasized("Overrides"), it)
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
            appendGitlabLink(field)
        }

        val signature = teletyped {
            appendModifiers(field)
            appendTypeReference(of.reference)
            appendText(" ")
            appendText(field.simpleName.escape())
        }

        val javadoc = field.javadoc

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

    private fun <T : Any?> appendExecutableReference(reference: CtExecutableReference<T>, qualify: Boolean = false) {
        teletype {
            val declaring = reference.declaringType
            val hyperref =
                "hyperref[${declaring.qualifiedName}$MEMBER_SEPARATOR${reference.executableDeclaration.signature}]"
            val isOwnClass =
                !qualify && reference.getParent(CtType::class.java).qualifiedName == declaring.qualifiedName
            if (reference.isConstructor || !isOwnClass) {
                appendTypeReference(declaring)
            }
            if (!reference.isConstructor && !isOwnClass) {
                appendText(".")
            }
            if (!reference.isConstructor) {
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

    private fun <T> CtExecutable<T>.getParentExecutable(): CtExecutable<out Any>? {
        if (this !is CtMethod) {
            return null
        }
        return inheritanceResolver.findSuperMethodsInCommentInheritanceOrder(
            this.reference.declaringType.typeDeclaration,
            this
        ).firstOrNull()
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

    fun appendText(text: String) {
        stringBuilder.append(text)
    }

    fun appendLine() = appendText("\n")

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

    private fun appendGitlabLink(to: CtElement) {
        if (gitlabSourceRoot == null) {
            return
        }
        val position = to.position.takeUnless { it is NoSourcePosition } ?: return
        val gitLabFile = "${gitlabSourceRoot.trimEnd('/')}/${to.position.file.relativeTo(sourceRoot).path}"

        val linkUrl: LaTeXContent = {
            appendText("$gitLabFile\\#L${position.line}")
        }
        val image: LaTeXContent = {
            appendCommand("protect", listOf())
            appendCommandWithArgAppender(
                "scalerel*",
                listOf({ appendCommand("includegraphics", GITLAB_LOGO_FILE) }, { appendText("B") })
            )
        }
        appendCommand(" ", listOf())
        appendCommandWithArgAppender("href", listOf(linkUrl, image))
    }

    fun appendGitlabLogoFile() = appendResourceFile("/$GITLAB_LOGO_FILE")

    private fun appendResourceFile(name: String) {
        inEnvironment("filecontents*", listOf(name.trim('/'))) {
            appendText(
                LaTeXBuilder::class.java.getResource(name)?.readText() ?: throw Exception("Resource $name is missing")
            )
        }
    }

    fun build(): String {
        var previousBuilder = indentedBuilders.pop()
        while (indentedBuilders.isNotEmpty()) {
            stringBuilder.appendLine()
            stringBuilder.appendLine(previousBuilder.toString().prependIndent(INDENT))
            previousBuilder = indentedBuilders.pop()
        }
        return previousBuilder.toString()
    }

    companion object {
        private const val INDENT = "  "
        private const val MEMBER_SEPARATOR = "@"
        private const val GITLAB_LOGO_FILE = "gitlab-logo.eps"
        private fun String.escape() = replace("_", "\\_")

    }

    private val CtElement.javadoc: JavadocCommentView
        get() {
            val elements = JavadocParser.forElement(this)
            var javadoc = JavadocCommentView(elements)
            javadoc = if (inheritDoc) {
                JavadocCommentView(inheritanceResolver.completeJavadocWithInheritedTags(this, javadoc))
            } else {
                javadoc
            }
            val visitor = JavadocReferenceParentVisitor(this)
            javadoc.elements.forEach { it.accept(visitor) }
            return javadoc
        }
}

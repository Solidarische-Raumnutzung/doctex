package de.mr_pine.doctex.spoon.javadoc

import de.mr_pine.doctex.LaTeXBuilder
import de.mr_pine.doctex.LaTeXContent
import de.mr_pine.doctex.intersperse
import spoon.javadoc.api.StandardJavadocTagType
import spoon.javadoc.api.elements.*
import spoon.reflect.declaration.*
import spoon.reflect.reference.CtFieldReference
import spoon.reflect.reference.CtReference
import kotlin.jvm.optionals.getOrNull

class JavadocLaTeXConverter : JavadocVisitor<LaTeXContent> {
    override fun defaultValue(): LaTeXContent = {}

    private lateinit var context: CtElement

    fun convertElements(elements: List<JavadocElement>, context: CtElement): LaTeXContent = {
        this@JavadocLaTeXConverter.context = context
        elements.forEach {
            it.accept(this@JavadocLaTeXConverter)()
        }
    }

    override fun visitText(docText: JavadocText?): LaTeXContent = {
        if (docText != null) {
            var text = docText.text
            text = text.replace("\n", " ").replace("#", ".")
            val lines = text.split("<br/>".toRegex())
            lines.map { processLine(it) }
                .intersperse { appendCommand("newline", listOf()); appendText("%"); appendLine() }
            appendText(" ")
        }
    }

    private fun LaTeXBuilder.processLine(line: String) {
        val parts = line
            .split("<ul>".toRegex(), 2)
        appendText(parts.first())
        if (parts.size == 2) {
            val listParts = parts[1].split("</ul>".toRegex(), 2)
            processList(listParts[0])
            if (listParts.size == 2) {
                processLine(listParts[1]);
            }
        }
    }

    private fun LaTeXBuilder.processList(content: String) {
        inEnvironment("itemize", listOf()) {
            val items = content.replace("</li>", "").split("<li/?>".toRegex()).filter(String::isNotBlank)
            for (item in items) {
                appendLine()
                appendCommand("item", listOf())
                appendText(" ")
                processLine(item)
            }
        }
    }

    /**
     * @see spoon.javadoc.api.parsing.LinkResolver
     */
    override fun visitInlineTag(tag: JavadocInlineTag?): LaTeXContent = {
        if (tag != null) {
            outer@for (element in tag.elements) {
                when (tag.tagType) {
                    StandardJavadocTagType.CODE -> teletype { element.accept(this@JavadocLaTeXConverter)() }
                    StandardJavadocTagType.LINK -> {
                        // START BODGE: Reimplment spoon code for resolving types in a manner that works for @link
                        when (element) {
                            is JavadocText -> {
                                if (element.text.contains("#")) {
                                    val idx = element.text.indexOf('#')
                                    val pkg = element.text.substring(0, idx)
                                        .let {
                                            if (it.contains("/") && !it.endsWith("/"))
                                                it.substring(it.indexOf('/') + 1)
                                            else it
                                        }
                                    val entry = element.text.substring(idx + 1)
                                    val type = qualifyType(pkg)
                                    var next = type
                                    while (next != null) {
                                        val field: CtReference? =
                                            qualifyTypeNameForField(next, entry)
                                        if (field != null) {
                                            appendReference(field)
                                            continue@outer
                                        }
                                        next = next.getParent(CtType::class.java)
                                    }
                                    element.accept(this@JavadocLaTeXConverter)()
                                } else {
                                    qualifyType(element.text)?.reference
                                        ?.let { appendReference(it) }
                                        ?: element.accept(this@JavadocLaTeXConverter)()
                                }
                            }
                            is JavadocReference -> appendReference(element.reference)
                            else -> element.accept(this@JavadocLaTeXConverter)()
                        }
                        // END BODGE
                    }
                    else -> element.accept(this@JavadocLaTeXConverter)()
                }
            }
        }
    }

    private fun qualifyType(name: String): CtType<*>? {
        val ctx: CtType<*>? = if (context is CtType<*>) context as CtType<*> else context.getParent(CtType::class.java)
        ctx?.referencedTypes
            ?.firstOrNull { it.simpleName == name || it.qualifiedName == name }
            ?.typeDeclaration
            ?.let { return it }
        (ctx?.`package` ?: ctx?.declaringType?.`package`)
            ?.getType<CtType<*>>(name)
            ?.let { return it }
        if (ctx != null && name.isBlank()) return ctx
        context.position.compilationUnit.imports
            .filter { it.importKind != CtImportKind.UNRESOLVED }
            .firstOrNull { it.reference?.simpleName == name }
            ?.referencedTypes?.firstOrNull { it.simpleName == name }
            ?.typeDeclaration
            ?.let { return it }
        println("WARNING: Reference $name could not be resolved")
        return null
    }

    private fun qualifyTypeNameForField(enclosingType: CtType<*>, memberName: String): CtReference? {
        // START BODGE: this is part of the @link resolution code
        if (enclosingType is CtEnum<*>) {
            val enumRef = enclosingType.enumValues
                .stream()
                .filter { it: CtEnumValue<*> -> it.simpleName == memberName }
                .map<CtReference> { obj: CtEnumValue<*> -> obj.reference }
                .findFirst()
                .getOrNull()

            if (enumRef != null) {
                return enumRef
            }
        }
        return enclosingType.allFields
            .stream()
            .filter { it: CtFieldReference<*> -> it.simpleName == memberName }
            .map { it: CtFieldReference<*> -> it }
            .findFirst()
            .getOrNull()
        // END BODGE
    }

    override fun visitBlockTag(tag: JavadocBlockTag?): LaTeXContent = {
        if (tag != null) {
            for (element in tag.elements) {
                element.accept(this@JavadocLaTeXConverter)()
            }
        }
    }

    fun visitBlockTag(tag: JavadocBlockTag, dropCount: Int): LaTeXContent = {
        for (element in tag.elements.drop(dropCount)) {
            element.accept(this@JavadocLaTeXConverter)()
        }
    }

    override fun visitReference(docReference: JavadocReference?): LaTeXContent = {
        if (docReference != null) {
            appendReference(docReference.reference)
        }
    }
}
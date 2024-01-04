package de.mr_pine.doctex.spoon.javadoc

import de.mr_pine.doctex.LaTeXBuilder
import de.mr_pine.doctex.LaTeXContent
import de.mr_pine.doctex.intersperse
import spoon.javadoc.api.StandardJavadocTagType
import spoon.javadoc.api.elements.*
import spoon.reflect.code.CtJavaDocTag.TagType
import spoon.reflect.reference.CtExecutableReference
import spoon.reflect.reference.CtTypeReference

class JavadocLaTeXConverter : JavadocVisitor<LaTeXContent> {
    override fun defaultValue(): LaTeXContent = {}

    fun convertElements(elements: List<JavadocElement>): LaTeXContent = {
        elements.forEach {
            it.accept(this@JavadocLaTeXConverter)()
        }
    }

    override fun visitText(docText: JavadocText?): LaTeXContent = {
        if (docText != null) {
            var text = docText.text
            text = text.replace("\n", " ")
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

    override fun visitInlineTag(tag: JavadocInlineTag?): LaTeXContent = {
        if (tag != null) {
            for (element in tag.elements) {
                when (tag.tagType) {
                    StandardJavadocTagType.CODE -> teletype { element.accept(this@JavadocLaTeXConverter)() }
                    else -> element.accept(this@JavadocLaTeXConverter)()
                }
            }
        }
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
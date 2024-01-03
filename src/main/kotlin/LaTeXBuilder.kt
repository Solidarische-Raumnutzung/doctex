package de.mr_pine.doctex

class LaTeXBuilder {
    private var indentedBuilders = Stack<StringBuilder>().apply { push(StringBuilder()) }
    private val stringBuilder
        get() = indentedBuilders.peek()
    private var currentDepth = 0

    fun appendPackageSection(packageName: String, content: LaTeXBuilder.() -> Unit) {
        appendSection("Package $packageName", content)
    }

    fun appendClassSection(className: String, content: LaTeXBuilder.() -> Unit) {
        appendSection("Class $className", content)
    }

    private fun appendSection(name: String, content: LaTeXBuilder.() -> Unit) = appendSection({appendText(name)}, content)
    private fun appendSection(name: LaTeXBuilder.() -> Unit, content: LaTeXBuilder.() -> Unit) {
        val sectionCommand = when (currentDepth) {
            in (0..2) -> "${"sub".repeat(currentDepth)}section"
            3 -> "paragraph"
            else -> "subparagraph"
        }
        currentDepth++
        appendCommandWithArgAppender(sectionCommand, listOf(name))
        appendLine()
        inEnvironment(listOf("adjustwidth", "1.5em", "0pt"), content)
        currentDepth--
    }

    private fun inEnvironment(arguments: List<String>, content: LaTeXBuilder.() -> Unit) {
        appendCommand("begin", arguments)
        appendLine()

        indentedBuilders.push(StringBuilder())
        this.content()

        val builder = indentedBuilders.pop()
        stringBuilder.append(builder.toString().prependIndent(INDENT))

        appendLine()
        appendCommand("end", arguments)
    }

    private fun appendCommand(name: String, arguments: List<String>) {
        val mappedArgs: List<LaTeXBuilder.() -> Unit> = arguments.map { { appendText(it) } }
        appendCommandWithArgAppender(name, mappedArgs)
    }

    private fun appendCommandWithArgAppender(name: String, arguments: List<LaTeXBuilder.() -> Unit>) {
        appendText("\\$name")
        arguments.forEach {
            appendText("{")
            this.it()
            appendText("}")
        }
    }

    private fun appendText(text: String): LaTeXBuilder {
        stringBuilder.append(text)
        return this
    }

    private fun appendLine() = appendText("\n")

    override fun toString(): String {
        var previousBuilder = indentedBuilders.pop()
        while (indentedBuilders.isNotEmpty()) {
            stringBuilder.appendLine()
            stringBuilder.appendLine(previousBuilder.toString().prependIndent(INDENT))
            previousBuilder = indentedBuilders.pop()
        }
        return previousBuilder.toString()
    }

    companion object {
        const val INDENT = "  "
    }
}
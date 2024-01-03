package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.DocTeXLauncher
import spoon.Launcher
import spoon.OutputType
import spoon.reflect.CtModel
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

    init {
        println("hi")
    }

}
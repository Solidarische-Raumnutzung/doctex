package de.mr_pine.doctex.spoon

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.eclipse.jdt.internal.compiler.lookup.ClassScope
import spoon.Launcher
import spoon.SpoonModelBuilder
import spoon.reflect.factory.Factory
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler
import spoon.support.compiler.jdt.JDTTreeBuilder

class DocTeXLauncher() : Launcher() {
    override fun getCompilerInstance(factory: Factory?): SpoonModelBuilder {
        return NoMethodBodyCompiler(factory)
    }

    class NoMethodBodyCompiler(factory: Factory?) : JDTBasedSpoonCompiler(factory) {
        private val noMethodBodyTreeBuilder = object : JDTTreeBuilder(factory) {
            override fun visit(methodDeclaration: MethodDeclaration?, scope: ClassScope?): Boolean {
                methodDeclaration?.statements = null
                return super.visit(methodDeclaration, scope)
            }
        }

        override fun traverseUnitDeclaration(builder: JDTTreeBuilder?, unitDeclaration: CompilationUnitDeclaration?) {
            super.traverseUnitDeclaration(noMethodBodyTreeBuilder, unitDeclaration)
            unitDeclaration?.apply {
                // remove non-javadoc comments to avoid warnings from JDTCommentBuilder later
                comments = reduceComments(this)
            }
        }

        companion object {
            private fun reduceComments(unitDeclaration: CompilationUnitDeclaration): Array<IntArray> {
                return unitDeclaration.comments.filter { it[0] >= 0 && it[1] >= 0 }.toTypedArray()
            }
        }
    }
}
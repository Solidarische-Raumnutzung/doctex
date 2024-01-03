package de.mr_pine.doctex.spoon

import de.mr_pine.doctex.LaTeXBuilder
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtPackage
import spoon.reflect.visitor.CtAbstractVisitor

class LaTeXGenerator : CtAbstractVisitor() {
    private val builder: LaTeXBuilder = LaTeXBuilder()
    override fun visitCtPackage(ctPackage: CtPackage?) {
        if (ctPackage == null || ctPackage.isEmpty) {
            return
        }

        builder.appendPackageSection(ctPackage.qualifiedName) {
            ctPackage.types.map { it.accept(this@LaTeXGenerator) }
        }
    }

    override fun <T : Any?> visitCtClass(ctClass: CtClass<T>?) {
        if(ctClass == null) {
            return
        }
        builder.appendClassSection(ctClass.qualifiedName) {
            ctClass.typeMembers.map { it.accept(this@LaTeXGenerator) }
        }
    }

    override fun toString() = builder.toString()
}
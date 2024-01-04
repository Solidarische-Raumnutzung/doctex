package de.mr_pine.doctex.spoon

import de.mr_pine.doctex.LaTeXBuilder
import spoon.reflect.declaration.*
import spoon.reflect.visitor.CtAbstractVisitor

class LaTeXGenerator(rootPackage: CtPackage) : CtAbstractVisitor() {
    private val builder: LaTeXBuilder = LaTeXBuilder(rootPackage)
    override fun visitCtPackage(ctPackage: CtPackage?) {
        if (ctPackage == null || ctPackage.isEmpty) {
            return
        }

        builder.appendPackageSection(ctPackage.qualifiedName) {
            ctPackage.types.map { it.accept(this@LaTeXGenerator) }
        }
    }

    override fun <T : Any?> visitCtClass(ctClass: CtClass<T>?) = visitCtType("Class", ctClass)
    override fun <T : Any?> visitCtInterface(ctInterface: CtInterface<T>?) = visitCtType("Interface", ctInterface)
    override fun visitCtRecord(ctRecord: CtRecord?) =
        visitCtType("Record", ctRecord)

    override fun <T : Enum<*>?> visitCtEnum(ctEnum: CtEnum<T>?) =
        visitCtType("Enum", ctEnum)

    private fun <T : Any?> visitCtType(typeType: String, ctType: CtType<T>?) {
        if (ctType == null) {
            return
        }
        builder.appendTypeSection(typeType, ctType) {
            // Sort constructors first then alphabetically
            ctType.declaredExecutables.sortedBy { it.simpleName }.sortedBy { !it.isConstructor }.map { it.executableDeclaration.accept(this@LaTeXGenerator) }
            ctType.nestedTypes.map { it.accept(this@LaTeXGenerator) }
        }
    }

    override fun <T : Any?> visitCtConstructor(constructor: CtConstructor<T>?) {
        if (constructor == null) {
            return
        }
        builder.appendExecutableSection(constructor, "Constructor")
    }

    override fun <T : Any?> visitCtMethod(method: CtMethod<T>?) {
        if (method == null) {
            return
        }
        builder.appendExecutableSection(method, "Method ${method.simpleName}")
    }



    fun generate() = builder.build()
}
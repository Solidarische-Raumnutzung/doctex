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
            ctType.nestedTypes.map { it.accept(this@LaTeXGenerator) }
        }
    }

    fun generate() = builder.build()
}
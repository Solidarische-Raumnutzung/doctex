package de.mr_pine.doctex.spoon

import de.mr_pine.doctex.LaTeXBuilder
import de.mr_pine.doctex.Visibility
import de.mr_pine.doctex.annotations.DoctexIgnore
import spoon.reflect.declaration.*
import spoon.reflect.visitor.CtAbstractVisitor
import java.io.File

class LaTeXGenerator(
    rootPackage: CtPackage,
    private val minimumVisibility: Visibility,
    inheritDoc: Boolean,
    sourceRoot: File,
    gitlabSourceRoot: String?,
    externalJavaDocs: Map<String, String>
) : CtAbstractVisitor() {
    private val builder: LaTeXBuilder = LaTeXBuilder(rootPackage, inheritDoc, sourceRoot, gitlabSourceRoot, externalJavaDocs)

    init {
        if (gitlabSourceRoot != null) {
            builder.appendGitlabLogoFile()
        }

        builder.appendLine()
        builder.appendLine()
    }
    override fun visitCtPackage(ctPackage: CtPackage?) {
        if (ctPackage == null || !ctPackage.hasTypes()) {
            return
        }

        builder.appendPackageSection(ctPackage) {
            ctPackage.types.filter { Visibility.fromModifiers(it.modifiers) >= minimumVisibility }
                .map { it.accept(this@LaTeXGenerator) }
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
            ctType.directChildren.filterIsInstance<CtExecutable<*>>().reversed().asSequence().sortedBy { it !is CtConstructor }
                .filter { it is CtModifiable && Visibility.fromModifiers(it.modifiers) >= minimumVisibility }
                .filter { DoctexIgnore::class.qualifiedName !in it.annotations.map { it.annotationType.qualifiedName } }
                .map {it.accept(this@LaTeXGenerator) }.toList()
            ctType.declaredFields.filter { Visibility.fromModifiers(it.modifiers) >= minimumVisibility }.reversed()
                .filter { DoctexIgnore::class.qualifiedName !in it.annotations.map { it.annotationType.qualifiedName } }
                .map { it.fieldDeclaration.accept(this@LaTeXGenerator) }
            ctType.nestedTypes.filter { Visibility.fromModifiers(it.modifiers) >= minimumVisibility }.reversed()
                .filter { DoctexIgnore::class.qualifiedName !in it.annotations.map { it.annotationType.qualifiedName } }
                .map { it.accept(this@LaTeXGenerator) }
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

    override fun <T : Any?> visitCtField(field: CtField<T>?) {
        if (field == null) {
            return
        }
        builder.appendFieldSection(field, "Field")
    }

    override fun <T : Any?> visitCtEnumValue(enumValue: CtEnumValue<T>?) {
        if (enumValue == null) {
            return
        }
        builder.appendFieldSection(enumValue, "Enum value")
    }

    fun generate() = builder.build()
}
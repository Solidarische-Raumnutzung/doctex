package de.mr_pine.doctex.spoon.javadoc

import spoon.javadoc.api.elements.JavadocReference
import spoon.javadoc.api.elements.JavadocVisitor
import spoon.reflect.declaration.CtElement

class JavadocReferenceParentVisitor(private val parent: CtElement): JavadocVisitor<Unit> {
    override fun defaultValue() {}

    override fun visitReference(reference: JavadocReference?) {
        reference?.reference?.setParent<CtElement?>(parent)
        super.visitReference(reference)
    }
}
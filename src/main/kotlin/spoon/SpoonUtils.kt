package de.mr_pine.doctex.spoon

import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtPackage
import spoon.reflect.declaration.CtType
import spoon.reflect.reference.CtExecutableReference
import spoon.reflect.reference.CtFieldReference
import spoon.reflect.reference.CtReference
import spoon.reflect.reference.CtTypeReference

fun <T> CtType<T>.inPackage(pkg: CtPackage) = qualifiedName.startsWith(pkg.qualifiedName)

fun CtReference.javaDocUrl(): String = getPlainJavaDocUrl().replace("#", ".html\\#")

private fun CtReference.getPlainJavaDocUrl(): String = when (this) {
    is CtExecutableReference<*>-> "${declaringType.getPlainJavaDocUrl()}#$signature"
    is CtFieldReference<*> -> "${declaringType.getPlainJavaDocUrl()}#$simpleName"
    is CtTypeReference<*> -> (if (topLevelType.qualifiedName != qualifiedName) "${topLevelType.getPlainJavaDocUrl()}.$simpleName" else qualifiedName.replace(
        '.', '/'
    )) + "#"
    else -> ""
}
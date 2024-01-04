package de.mr_pine.doctex.spoon

import spoon.reflect.declaration.CtPackage
import spoon.reflect.declaration.CtType

fun <T> CtType<T>.inPackage(pkg: CtPackage) = generateSequence(`package`) { it.declaringPackage }.any { it == pkg }
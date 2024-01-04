package de.mr_pine.doctex

import spoon.reflect.declaration.ModifierKind

enum class Visibility(private val corresponding: ModifierKind?) {
    PRIVATE(ModifierKind.PRIVATE), PROTECTED(ModifierKind.PROTECTED), PACKAGE(null), PUBLIC(ModifierKind.PUBLIC);

    companion object {
        fun fromModifiers(modifiers: Collection<ModifierKind>): Visibility {
            val visibilityModifiers = entries.map(Visibility::corresponding)
            val visibilityModifier = modifiers.intersect(visibilityModifiers.toSet()).firstOrNull()
            return entries.find { it.corresponding == visibilityModifier }!!
        }
    }
}
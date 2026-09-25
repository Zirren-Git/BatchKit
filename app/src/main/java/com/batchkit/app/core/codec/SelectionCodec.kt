package com.batchkit.app.core.codec

import com.batchkit.app.core.model.BatchAction

/**
 * Flat, dependency free encoding for app selections and action sets.
 *
 * Android package names only contain letters, digits, underscores and dots, so a
 * newline separated list is unambiguous and stays readable in the database.
 */
object SelectionCodec {

    private const val PACKAGE_SEPARATOR = "\n"
    private const val ACTION_SEPARATOR = ","

    private val PACKAGE_PATTERN = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

    fun isValidPackageName(value: String): Boolean =
        value.isNotEmpty() && value.length <= 255 && PACKAGE_PATTERN.matches(value)

    fun encodePackages(packages: Collection<String>): String = packages
        .asSequence()
        .map { it.trim() }
        .filter { isValidPackageName(it) }
        .distinct()
        .sorted()
        .joinToString(PACKAGE_SEPARATOR)

    fun decodePackages(raw: String?): List<String> = raw
        .orEmpty()
        .split(PACKAGE_SEPARATOR)
        .asSequence()
        .map { it.trim() }
        .filter { isValidPackageName(it) }
        .distinct()
        .toList()

    fun encodeActions(actions: Collection<BatchAction>): String = actions
        .asSequence()
        .map { it.id }
        .distinct()
        .joinToString(ACTION_SEPARATOR)

    fun decodeActions(raw: String?): List<BatchAction> = raw
        .orEmpty()
        .split(ACTION_SEPARATOR)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { BatchAction.fromId(it) }
        .distinct()
        .toList()
}

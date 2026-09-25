package com.batchkit.app.core.model

/**
 * A named selection of apps plus the actions that belong to it.
 *
 * Persisted through Room; encoding of the two lists is handled by
 * [com.batchkit.app.core.codec.SelectionCodec] so that the database stays flat.
 */
data class Profile(
    val id: Long = 0L,
    val name: String,
    val packages: List<String>,
    val actions: List<BatchAction>,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val scheduleEnabled: Boolean = false,
    val scheduleHour: Int = 22,
    val scheduleMinute: Int = 0,
) {
    val primaryAction: BatchAction get() = actions.firstOrNull() ?: BatchAction.FORCE_STOP
}

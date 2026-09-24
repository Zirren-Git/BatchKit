package com.batchkit.app.data.model

data class SingleActionResult(
    val packageName: String,
    val appLabel: String,
    val actionType: BatchActionType,
    val isSuccess: Boolean,
    val isSkipped: Boolean = false,
    val message: String? = null
)

data class BatchExecutionReport(
    val actionType: BatchActionType,
    val results: List<SingleActionResult>,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis()
) {
    val totalCount: Int get() = results.size
    val successCount: Int get() = results.count { it.isSuccess && !it.isSkipped }
    val failureCount: Int get() = results.count { !it.isSuccess && !it.isSkipped }
    val skippedCount: Int get() = results.count { it.isSkipped }
}

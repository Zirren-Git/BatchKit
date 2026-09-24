package com.batchkit.app.data.model

data class Profile(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val targetPackageNames: List<String>,
    val actions: List<BatchActionType>,
    val createdAt: Long = System.currentTimeMillis()
)

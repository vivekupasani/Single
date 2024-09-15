package com.vivekupasani.single.models

data class Users(
    val userId: String = "",
    val email: String = "",
    val password: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val about: String = "",
    val userName: String? = null,
    val profilePicURL: String? = null,
    val lastMessage: String? = null,
    val friends: MutableList<String> = mutableListOf(),
    val friendRequests: List<Map<String, Any>> = emptyList()
)

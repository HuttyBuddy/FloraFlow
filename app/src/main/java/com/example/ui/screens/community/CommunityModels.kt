package com.example.ui.screens.community

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_posts")
data class CommunityPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // e.g., "Tips", "Experiences", "Questions", "General"
    val author: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val gridString: String = ""
)

@Entity(tableName = "community_comments")
data class CommunityComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val author: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val isLiked: Boolean = false
)

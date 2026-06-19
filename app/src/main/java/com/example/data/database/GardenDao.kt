package com.example.data.database

import androidx.room.*
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDao {
    // --- Garden Layout ---
    @Query("SELECT * FROM garden_layouts ORDER BY id DESC")
    fun getAllLayouts(): Flow<List<GardenLayout>>

    @Query("SELECT * FROM garden_layouts WHERE id = :id")
    fun getLayoutById(id: Int): Flow<GardenLayout?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: GardenLayout): Long

    @Query("UPDATE garden_layouts SET gridString = :newGridString WHERE id = :id")
    suspend fun updateLayoutGrid(id: Int, newGridString: String)

    @Delete
    suspend fun deleteLayout(layout: GardenLayout)

    // --- Plants ---
    @Query("SELECT * FROM plants WHERE layoutId = :layoutId ORDER BY id ASC")
    fun getPlantsForLayout(layoutId: Int): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<Plant>)

    @Update
    suspend fun updatePlant(plant: Plant)

    @Query("DELETE FROM plants WHERE id = :plantId")
    suspend fun deletePlantById(plantId: Int)

    @Query("DELETE FROM plants WHERE layoutId = :layoutId")
    suspend fun deletePlantsByLayout(layoutId: Int)

    // --- Mood Logs ---
    @Query("SELECT * FROM mood_logs ORDER BY timestamp DESC")
    fun getAllMoodLogs(): Flow<List<MoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(moodLog: MoodLog): Long

    @Query("DELETE FROM mood_logs WHERE id = :id")
    suspend fun deleteMoodLogById(id: Int)

    // --- Community ---
    @Query("SELECT * FROM community_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<com.example.ui.screens.community.CommunityPost>>

    @Query("SELECT * FROM community_posts WHERE id = :id")
    fun getPostById(id: Int): Flow<com.example.ui.screens.community.CommunityPost?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: com.example.ui.screens.community.CommunityPost): Long

    @Query("UPDATE community_posts SET likes = :likes, isLiked = :isLiked WHERE id = :id")
    suspend fun updatePostLikes(id: Int, likes: Int, isLiked: Boolean)

    @Query("DELETE FROM community_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)

    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: Int): Flow<List<com.example.ui.screens.community.CommunityComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: com.example.ui.screens.community.CommunityComment): Long

    @Query("UPDATE community_comments SET likes = :likes, isLiked = :isLiked WHERE id = :id")
    suspend fun updateCommentLikes(id: Int, likes: Int, isLiked: Boolean)

    @Query("DELETE FROM community_comments WHERE postId = :postId")
    suspend fun deleteCommentsByPostId(postId: Int)
}

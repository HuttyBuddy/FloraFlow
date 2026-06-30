package com.example.data.repository

import com.example.data.database.GardenDao
import com.example.data.model.GardenLayout
import com.example.data.model.MoodLog
import com.example.data.model.Plant
import com.example.data.model.CareTask
import com.example.data.model.RestorationLog
import com.example.data.model.AssessmentResult
import kotlinx.coroutines.flow.Flow

class GardenRepository(private val gardenDao: GardenDao) {

    // --- Garden Layouts ---
    val allLayouts: Flow<List<GardenLayout>> = gardenDao.getAllLayouts()

    fun getLayoutById(id: Int): Flow<GardenLayout?> = gardenDao.getLayoutById(id)

    suspend fun insertLayout(layout: GardenLayout): Long = gardenDao.insertLayout(layout)

    suspend fun updateLayoutGrid(id: Int, newGridString: String) = gardenDao.updateLayoutGrid(id, newGridString)

    suspend fun updateLayoutClimate(id: Int, climate: String) = gardenDao.updateLayoutClimate(id, climate)

    suspend fun deleteLayout(layout: GardenLayout) = gardenDao.deleteLayout(layout)

    // --- Plants ---
    val allPlants: Flow<List<Plant>> = gardenDao.getAllPlants()

    fun getPlantsForLayout(layoutId: Int): Flow<List<Plant>> = gardenDao.getPlantsForLayout(layoutId)

    suspend fun insertPlant(plant: Plant): Long = gardenDao.insertPlant(plant)

    suspend fun insertPlants(plants: List<Plant>) = gardenDao.insertPlants(plants)

    suspend fun updatePlant(plant: Plant) = gardenDao.updatePlant(plant)

    suspend fun deletePlantById(plantId: Int) = gardenDao.deletePlantById(plantId)

    suspend fun deletePlantsByLayout(layoutId: Int) = gardenDao.deletePlantsByLayout(layoutId)

    // --- Mood Logs ---
    val allMoodLogs: Flow<List<MoodLog>> = gardenDao.getAllMoodLogs()

    suspend fun insertMoodLog(moodLog: MoodLog): Long = gardenDao.insertMoodLog(moodLog)

    suspend fun deleteMoodLogById(id: Int) = gardenDao.deleteMoodLogById(id)

    // --- Community ---
    val allPosts: Flow<List<com.example.data.model.CommunityPost>> = gardenDao.getAllPosts()

    fun getPostById(id: Int): Flow<com.example.data.model.CommunityPost?> = gardenDao.getPostById(id)

    suspend fun insertPost(post: com.example.data.model.CommunityPost): Long = gardenDao.insertPost(post)

    suspend fun updatePostLikes(id: Int, likes: Int, isLiked: Boolean) = gardenDao.updatePostLikes(id, likes, isLiked)

    suspend fun deletePost(id: Int) {
        gardenDao.deletePostById(id)
        gardenDao.deleteCommentsByPostId(id)
    }

    fun getCommentsForPost(postId: Int): Flow<List<com.example.data.model.CommunityComment>> = gardenDao.getCommentsForPost(postId)

    suspend fun insertComment(comment: com.example.data.model.CommunityComment): Long = gardenDao.insertComment(comment)

    suspend fun updateCommentLikes(id: Int, likes: Int, isLiked: Boolean) = gardenDao.updateCommentLikes(id, likes, isLiked)

    // --- Care Tasks ---
    val allCareTasks: Flow<List<CareTask>> = gardenDao.getAllCareTasks()
    val pendingCareTasks: Flow<List<CareTask>> = gardenDao.getPendingCareTasks()

    fun getCareTasksForPlant(plantId: Int): Flow<List<CareTask>> = gardenDao.getCareTasksForPlant(plantId)

    suspend fun insertCareTask(careTask: CareTask): Long = gardenDao.insertCareTask(careTask)
    suspend fun insertCareTasks(careTasks: List<CareTask>) = gardenDao.insertCareTasks(careTasks)
    suspend fun updateCareTask(careTask: CareTask) = gardenDao.updateCareTask(careTask)
    suspend fun completeCareTask(id: Int, completedDate: Long) = gardenDao.completeCareTask(id, completedDate)
    suspend fun deleteCareTaskById(id: Int) = gardenDao.deleteCareTaskById(id)
    suspend fun deleteCareTasksByPlant(plantId: Int) = gardenDao.deleteCareTasksByPlant(plantId)

    // --- Restoration Logs ---
    val allRestorationLogs: Flow<List<RestorationLog>> = gardenDao.getAllRestorationLogs()
    suspend fun insertRestorationLog(log: RestorationLog): Long = gardenDao.insertRestorationLog(log)

    // --- Assessment Results ---
    val allAssessmentResults: Flow<List<AssessmentResult>> = gardenDao.getAllAssessmentResults()
    suspend fun insertAssessmentResult(result: AssessmentResult): Long = gardenDao.insertAssessmentResult(result)
}

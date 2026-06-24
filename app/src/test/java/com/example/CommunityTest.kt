package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.GardenDatabase
import com.example.ui.viewmodel.GardenViewModel
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityComment
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CommunityTest {

    private lateinit var context: Context
    private lateinit var db: GardenDatabase
    private lateinit var viewModel: GardenViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Application>()
        
        // Build isolated in-memory database for each test
        db = Room.inMemoryDatabaseBuilder(
            context,
            GardenDatabase::class.java
        )
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun waitForPostsSeeded(viewModel: GardenViewModel) {
        var attempts = 0
        while (attempts < 100 && viewModel.allCommunityPosts.value.isEmpty()) {
            kotlinx.coroutines.delay(20)
            attempts++
        }
        assertTrue("Community posts seeding timed out", viewModel.allCommunityPosts.value.isNotEmpty())
    }

    private suspend fun waitForPostCount(viewModel: GardenViewModel, expectedCount: Int) {
        var attempts = 0
        while (attempts < 100 && viewModel.allCommunityPosts.value.size != expectedCount) {
            kotlinx.coroutines.delay(20)
            attempts++
        }
        assertEquals("Post count mismatch", expectedCount, viewModel.allCommunityPosts.value.size)
    }

    private suspend fun waitForPostLiked(viewModel: GardenViewModel, postId: Int, expectedLiked: Boolean): CommunityPost {
        var attempts = 0
        var post = viewModel.allCommunityPosts.value.find { it.id == postId }
        while (attempts < 100 && (post == null || post.isLiked != expectedLiked)) {
            kotlinx.coroutines.delay(20)
            attempts++
            post = viewModel.allCommunityPosts.value.find { it.id == postId }
        }
        assertNotNull("Post not found", post)
        assertEquals("Liked status mismatch", expectedLiked, post?.isLiked)
        return post!!
    }

    private suspend fun waitForCommentsCount(viewModel: GardenViewModel, postId: Int, expectedCount: Int): List<CommunityComment> {
        var attempts = 0
        var comments = emptyList<CommunityComment>()
        while (attempts < 100) {
            try {
                comments = viewModel.getCommentsForPost(postId).first()
                if (comments.size == expectedCount) break
            } catch (e: Exception) {
                // Ignore temporary database reading errors
            }
            kotlinx.coroutines.delay(20)
            attempts++
        }
        assertEquals("Comments count mismatch", expectedCount, comments.size)
        return comments
    }

    @Test
    fun testCommunitySeeding() = runTest {
        viewModel = GardenViewModel(context as Application, db, testDispatcher)
        
        // Start collecting to activate the WhileSubscribed StateFlow
        backgroundScope.launch {
            viewModel.allCommunityPosts.collect {}
        }
        
        waitForPostsSeeded(viewModel)

        val posts = viewModel.allCommunityPosts.value
        
        // Verify we have specific seeded posts
        val succulentPost = posts.find { it.title.contains("Succulents") }
        assertNotNull("Watering succulents post should be seeded", succulentPost)
        assertEquals("GardenGuru", succulentPost?.author)
        assertEquals("Tips", succulentPost?.category)

        // Verify comments are seeded
        val comments = waitForCommentsCount(viewModel, succulentPost!!.id, 2)
        val cactusComment = comments.find { it.author == "CactusJack" }
        assertNotNull("CactusJack comment should be present", cactusComment)
    }

    @Test
    fun testCreatePost() = runTest {
        viewModel = GardenViewModel(context as Application, db, testDispatcher)
        
        // Start collecting to activate the WhileSubscribed StateFlow
        backgroundScope.launch {
            viewModel.allCommunityPosts.collect {}
        }
        waitForPostsSeeded(viewModel)

        val initialCount = viewModel.allCommunityPosts.value.size

        // Create new post
        viewModel.createPost(
            title = "Pruning Lavender Tips",
            content = "Make sure to prune them right after summer blooms for optimal health next spring.",
            category = "Tips",
            author = "LavenderLover"
        )
        
        waitForPostCount(viewModel, initialCount + 1)

        val updatedPosts = viewModel.allCommunityPosts.value
        val newPost = updatedPosts.find { it.author == "LavenderLover" }
        assertNotNull(newPost)
        assertEquals("Pruning Lavender Tips", newPost?.title)
        assertEquals("Tips", newPost?.category)
        assertFalse(newPost!!.isLiked)
    }

    @Test
    fun testToggleLikePost() = runTest {
        viewModel = GardenViewModel(context as Application, db, testDispatcher)
        
        // Start collecting to activate the WhileSubscribed StateFlow
        backgroundScope.launch {
            viewModel.allCommunityPosts.collect {}
        }
        waitForPostsSeeded(viewModel)

        val posts = viewModel.allCommunityPosts.value
        val firstPost = posts.first()
        val initialLikes = firstPost.likes
        val initialLiked = firstPost.isLiked

        // Like the post
        viewModel.toggleLikePost(firstPost.id, initialLikes, initialLiked)
        val updatedPost = waitForPostLiked(viewModel, firstPost.id, !initialLiked)
        assertEquals(initialLikes + 1, updatedPost.likes)

        // Unlike the post
        viewModel.toggleLikePost(firstPost.id, updatedPost.likes, updatedPost.isLiked)
        val revertedPost = waitForPostLiked(viewModel, firstPost.id, initialLiked)
        assertEquals(initialLikes, revertedPost.likes)
    }

    @Test
    fun testDeletePost() = runTest {
        viewModel = GardenViewModel(context as Application, db, testDispatcher)
        
        // Start collecting to activate the WhileSubscribed StateFlow
        backgroundScope.launch {
            viewModel.allCommunityPosts.collect {}
        }
        waitForPostsSeeded(viewModel)

        val posts = viewModel.allCommunityPosts.value
        val targetPost = posts.find { it.title.contains("Monstera") }!!
        val initialCount = posts.size

        // Verify comments exist first
        waitForCommentsCount(viewModel, targetPost.id, 1)

        // Delete post
        viewModel.deletePost(targetPost.id)
        waitForPostCount(viewModel, initialCount - 1)

        val updatedPosts = viewModel.allCommunityPosts.value
        assertNull(updatedPosts.find { it.id == targetPost.id })

        // Verify comments are deleted
        waitForCommentsCount(viewModel, targetPost.id, 0)
    }

    @Test
    fun testAddCommentAndLikeComment() = runTest {
        viewModel = GardenViewModel(context as Application, db, testDispatcher)
        
        // Start collecting to activate the WhileSubscribed StateFlow
        backgroundScope.launch {
            viewModel.allCommunityPosts.collect {}
        }
        waitForPostsSeeded(viewModel)

        val posts = viewModel.allCommunityPosts.value
        val post = posts.find { it.title.contains("Succulents") }!!

        // Verify initial comments count
        val initialComments = waitForCommentsCount(viewModel, post.id, 2)

        // Add comment
        viewModel.addComment(
            postId = post.id,
            author = "FlowerPower",
            content = "This is an awesome tip!"
        )

        val updatedComments = waitForCommentsCount(viewModel, post.id, initialComments.size + 1)
        val newComment = updatedComments.find { it.author == "FlowerPower" }
        assertNotNull(newComment)
        assertEquals("This is an awesome tip!", newComment?.content)
        assertEquals(0, newComment?.likes)
        assertFalse(newComment!!.isLiked)

        // Like the comment
        viewModel.toggleLikeComment(newComment.id, newComment.likes, newComment.isLiked)
        
        // Wait for comments update and find liked comment
        var commentLiked = false
        var attempts = 0
        while (attempts < 100) {
            val commentsList = viewModel.getCommentsForPost(post.id).first()
            val comment = commentsList.find { it.id == newComment.id }
            if (comment != null && comment.isLiked) {
                assertEquals(1, comment.likes)
                commentLiked = true
                break
            }
            kotlinx.coroutines.delay(20)
            attempts++
        }
        assertTrue("Comment upvoting timed out", commentLiked)
    }
}

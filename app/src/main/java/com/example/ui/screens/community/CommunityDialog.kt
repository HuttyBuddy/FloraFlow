package com.example.ui.screens.community

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CommunityViewState {
    FEED, DETAIL, CREATE
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val posts by viewModel.allCommunityPosts.collectAsState()
    var viewState by remember { mutableStateOf(CommunityViewState.FEED) }
    var selectedPost by remember { mutableStateOf<CommunityPost?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("community_dialog_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = viewState,
                    transitionSpec = {
                        if (targetState == CommunityViewState.FEED) {
                            (slideInHorizontally { -it } + fadeIn()) with (slideOutHorizontally { it } + fadeOut())
                        } else {
                            (slideInHorizontally { it } + fadeIn()) with (slideOutHorizontally { -it } + fadeOut())
                        }
                    },
                    label = "community_state_transition"
                ) { state ->
                    when (state) {
                        CommunityViewState.FEED -> {
                            CommunityFeedView(
                                posts = posts,
                                onPostClick = { post ->
                                    selectedPost = post
                                    viewState = CommunityViewState.DETAIL
                                },
                                onCreatePostClick = {
                                    viewState = CommunityViewState.CREATE
                                },
                                onLikeClick = { post ->
                                    viewModel.toggleLikePost(post.id, post.likes, post.isLiked)
                                },
                                onDeleteClick = { post ->
                                    viewModel.deletePost(post.id)
                                },
                                onDismiss = onDismiss
                            )
                        }
                        CommunityViewState.DETAIL -> {
                            selectedPost?.let { currentPost ->
                                // Keep reference to updated post from DB stream
                                val updatedPost = posts.find { it.id == currentPost.id } ?: currentPost
                                CommunityDetailView(
                                    post = updatedPost,
                                    commentsFlow = viewModel.getCommentsForPost(updatedPost.id),
                                    onBackClick = {
                                        selectedPost = null
                                        viewState = CommunityViewState.FEED
                                    },
                                    onLikePost = {
                                        viewModel.toggleLikePost(updatedPost.id, updatedPost.likes, updatedPost.isLiked)
                                    },
                                    onAddComment = { author, content ->
                                        viewModel.addComment(updatedPost.id, author, content)
                                    },
                                    onLikeComment = { comment ->
                                        viewModel.toggleLikeComment(comment.id, comment.likes, comment.isLiked)
                                    }
                                )
                            }
                        }
                        CommunityViewState.CREATE -> {
                            CommunityCreatePostView(
                                onBackClick = {
                                    viewState = CommunityViewState.FEED
                                },
                                onPublishClick = { title, content, category, author ->
                                    viewModel.createPost(title, content, category, author)
                                    viewState = CommunityViewState.FEED
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedView(
    posts: List<CommunityPost>,
    onPostClick: (CommunityPost) -> Unit,
    onCreatePostClick: () -> Unit,
    onLikeClick: (CommunityPost) -> Unit,
    onDeleteClick: (CommunityPost) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredPosts = remember(posts, searchQuery, selectedCategory) {
        posts.filter { post ->
            val matchesSearch = post.title.contains(searchQuery, ignoreCase = true) ||
                    post.content.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || post.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FloraFlow Circle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Share tips, swap stories, and grow together",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("community_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Community")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tips or experiences...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("community_search_input"),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Chips
        val categories = listOf("All", "Tips", "Experiences", "Questions", "General")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(3).forEach { cat ->
                CommunityCategoryChip(
                    label = cat,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.drop(3).forEach { cat ->
                CommunityCategoryChip(
                    label = cat,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // New Post Button
            Button(
                onClick = onCreatePostClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("community_new_post_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Post", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Posts Feed List
        if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No posts found",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Be the first to share a tips or experience with the garden circle!",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("community_posts_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        onClick = { onPostClick(post) },
                        onLikeClick = { onLikeClick(post) },
                        onDeleteClick = { onDeleteClick(post) }
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (post.isLiked) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Meta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                CommunityCategoryBadge(category = post.category)
                Spacer(modifier = Modifier.width(8.dp))
                // Author info
                Text(
                    text = "by ${post.author}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                // Time
                Text(
                    text = formatRelativeTime(post.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Content
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onLikeClick)
                        .testTag("post_like_button_${post.id}")
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Post",
                        tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(scale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likes}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Detail comments preview button
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Button (Only for user-added content, but we allow simple delete for user control in sandbox)
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("post_delete_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Post",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityDetailView(
    post: CommunityPost,
    commentsFlow: Flow<List<CommunityComment>>,
    onBackClick: () -> Unit,
    onLikePost: () -> Unit,
    onAddComment: (String, String) -> Unit,
    onLikeComment: (CommunityComment) -> Unit
) {
    val comments by commentsFlow.collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val isCommentValid = commentText.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("community_detail_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Feed")
            }
            Text(
                text = "Post Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag("community_comments_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Post Card Detail
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CommunityCategoryBadge(category = post.category)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "by ${post.author}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = formatRelativeTime(post.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = post.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Like Post
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onLikePost)
                                .testTag("detail_post_like_button")
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${post.likes} Upvotes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Comments Header
            item {
                Text(
                    text = "Discussion (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Comments
            if (comments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "No comments yet. Start the conversation below!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    CommentItemRow(comment = comment, onLikeClick = { onLikeComment(comment) })
                }
            }

            // Add Comment Form inside Scroll list
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Leave a comment",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Author Name (Optional)
                        OutlinedTextField(
                            value = authorName,
                            onValueChange = { authorName = it },
                            placeholder = { Text("Your Name (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("comment_author_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Comment Text
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Write your reply...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .testTag("comment_text_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (isCommentValid) {
                                    val finalAuthor = authorName.trim().ifBlank { "Anonymous Gardener" }
                                    onAddComment(finalAuthor, commentText.trim())
                                    commentText = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("comment_submit_button"),
                            enabled = isCommentValid,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post Comment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItemRow(
    comment: CommunityComment,
    onLikeClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (comment.isLiked) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_row_${comment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(comment.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Likes button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onLikeClick)
                    .testTag("comment_like_button_${comment.id}")
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Comment",
                    tint = if (comment.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .scale(scale)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${comment.likes}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (comment.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CommunityCreatePostView(
    onBackClick: () -> Unit,
    onPublishClick: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tips") }

    val isFormValid = title.isNotBlank() && content.isNotBlank()
    val categories = listOf("Tips", "Experiences", "Questions", "General")

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("community_create_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Feed")
            }
            Text(
                text = "Plant a Post",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Category Selector
            item {
                Column {
                    Text(
                        text = "Choose a Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.take(2).forEach { cat ->
                            CommunityCategoryChip(
                                label = cat,
                                selected = category == cat,
                                onClick = { category = cat }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.drop(2).forEach { cat ->
                            CommunityCategoryChip(
                                label = cat,
                                selected = category == cat,
                                onClick = { category = cat }
                            )
                        }
                    }
                }
            }

            // Title
            item {
                Column {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("What is your post about?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Author Name
            item {
                Column {
                    Text(
                        text = "Your Name (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        placeholder = { Text("e.g., CactusJack (defaults to Anonymous)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_author_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Content
            item {
                Column {
                    Text(
                        text = "Content",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Share your gardening insights, stories, or ask a question...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("post_content_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Publish Button
            item {
                Button(
                    onClick = {
                        if (isFormValid) {
                            onPublishClick(title.trim(), content.trim(), category, author.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("post_publish_button"),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Eco, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish to Circle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun CommunityCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val emoji = when (label) {
        "Tips" -> "💡"
        "Experiences" -> "🌸"
        "Questions" -> "❓"
        "General" -> "💬"
        else -> "🌿"
    }

    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("community_chip_${label}"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (label == "All") "🌿 All" else "$emoji $label",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun CommunityCategoryBadge(category: String) {
    val (emoji, containerColor, textColor) = when (category) {
        "Tips" -> Triple("💡", Color(0xFFFFF9C4), Color(0xFF5D4037))
        "Experiences" -> Triple("🌸", Color(0xFFF8BBD0), Color(0xFF880E4F))
        "Questions" -> Triple("❓", Color(0xFFE1BEE7), Color(0xFF4A148C))
        else -> Triple("💬", Color(0xFFD1C4E9), Color(0xFF311B92))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = textColor,
        modifier = Modifier.testTag("category_badge_$category")
    ) {
        Text(
            text = "$emoji $category",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

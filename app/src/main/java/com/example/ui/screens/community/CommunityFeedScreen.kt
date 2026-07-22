package com.example.ui.screens.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel

data class CommunityPost(
    val id: String,
    val authorName: String,
    val timeAgo: String,
    val title: String,
    val description: String,
    val beforeScore: Int,
    val afterScore: Int,
    val plantTags: List<String>,
    var likesCount: Int,
    var isLiked: Boolean = false,
    val commentsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    var posts by remember {
        mutableStateOf(
            listOf(
                CommunityPost(
                    id = "1",
                    authorName = "Elena R.",
                    timeAgo = "2 hours ago",
                    title = "My Urban Balcony Sanctuary Transformation",
                    description = "Transformed a cluttered 4x6 balcony into a morning reflection sanctuary with Monstera, Lavender, and bamboo wind chimes.",
                    beforeScore = 8,
                    afterScore = 18,
                    plantTags = listOf("Monstera", "Lavender", "Bonsai"),
                    likesCount = 24,
                    commentsCount = 5
                ),
                CommunityPost(
                    id = "2",
                    authorName = "Marcus K.",
                    timeAgo = "5 hours ago",
                    title = "Minimalist Desk Meditation Corner",
                    description = "Added daylight plant shelves and a tabletop fountain. My stress levels during work dropped noticeably!",
                    beforeScore = 6,
                    afterScore = 16,
                    plantTags = listOf("ZZ Plant", "Snake Plant", "Pothos"),
                    likesCount = 42,
                    commentsCount = 9
                ),
                CommunityPost(
                    id = "3",
                    authorName = "Aria Chen",
                    timeAgo = "1 day ago",
                    title = "Sunlit Living Room Biophilic Wall",
                    description = "Installed vertical hanging planters near my south-facing window. The light and oxygen boost is amazing.",
                    beforeScore = 11,
                    afterScore = 19,
                    plantTags = listOf("Fern", "Pothos", "Peace Lily"),
                    likesCount = 57,
                    commentsCount = 12
                )
            )
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Meditation Corners", "Urban Balconies", "Indoor Sanctuaries")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Biophilic Community Feed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("community_feed_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share Your Transformation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inspire others with your biophilic score improvements and space setup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Post")
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) }
                        )
                    }
                }
            }

            items(posts, key = { it.id }) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = post.authorName.take(1),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = post.authorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = post.timeAgo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${post.beforeScore}/20 → ${post.afterScore}/20",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = post.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            post.plantTags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text("🌿 $tag", fontSize = 11.sp) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    posts = posts.map {
                                        if (it.id == post.id) {
                                            it.copy(
                                                isLiked = !it.isLiked,
                                                likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1
                                            )
                                        } else it
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${post.likesCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comment",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${post.commentsCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

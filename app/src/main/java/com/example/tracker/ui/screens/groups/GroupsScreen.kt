package com.example.tracker.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Focus Groups",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.Search, "Search", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Join a Community",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(focusGroups) { group ->
                    GroupCard(group)
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Global Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(leaderboardUsers.indices.toList()) { index ->
                    LeaderboardItem(leaderboardUsers[index], index + 1)
                }
                
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: FocusGroup) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, null, tint = PrimaryAccent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                Text("${group.memberCount} members • ${group.liveCount} live now", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BackgroundDark),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Join", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LeaderboardItem(user: LeaderboardUser, rank: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Black,
                color = when(rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> TextSecondary
                },
                modifier = Modifier.width(40.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${user.focusTime} focused", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            
            if (rank <= 3) {
                Icon(Icons.Default.EmojiEvents, null, tint = when(rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    else -> Color(0xFFCD7F32)
                }, modifier = Modifier.size(20.dp))
            }
        }
    }
}

data class FocusGroup(val name: String, val memberCount: Int, val liveCount: Int)
data class LeaderboardUser(val name: String, val focusTime: String)

val focusGroups = listOf(
    FocusGroup("UPSC Aspirants", 1250, 42),
    FocusGroup("Code Grind 2024", 850, 18),
    FocusGroup("Meditation Circle", 420, 12),
    FocusGroup("Book Worms", 310, 5)
)

val leaderboardUsers = listOf(
    LeaderboardUser("Soham", "12h 45m"),
    LeaderboardUser("Ananya", "10h 20m"),
    LeaderboardUser("Rahul", "9h 55m"),
    LeaderboardUser("Priya", "8h 30m"),
    LeaderboardUser("Vikram", "7h 15m")
)

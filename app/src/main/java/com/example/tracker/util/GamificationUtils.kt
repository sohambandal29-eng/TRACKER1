package com.example.tracker.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean,
    val progress: Long,
    val target: Long,
    val unit: String
)

object GamificationUtils {
    fun calculateLevel(totalSeconds: Long): Int {
        val hours = totalSeconds / 3600
        return (hours / 5).toInt() + 1 // Level up every 5 hours
    }

    fun getProgressToNextLevel(totalSeconds: Long): Float {
        val hours = totalSeconds / 3600f
        return (hours % 5) / 5f
    }

    fun getBadges(totalSeconds: Long, todaySeconds: Long, streak: Int): List<Badge> {
        val totalHours = totalSeconds / 3600
        val todayHours = todaySeconds / 3600

        return listOf(
            Badge(
                "starter", "Focused Starter", "Complete your first hour",
                Icons.Default.Bolt, totalHours >= 1, totalHours, 1, "h"
            ),
            Badge(
                "deep_work", "Deep Work Master", "4 hours of focus today",
                Icons.Default.WorkspacePremium, todayHours >= 4, todayHours, 4, "h"
            ),
            Badge(
                "consistency", "Consistency King", "Reach a 7-day streak",
                Icons.Default.AutoGraph, streak >= 7, streak.toLong(), 7, "d"
            ),
            Badge(
                "century", "Century Club", "Complete 100 hours total",
                Icons.Default.MilitaryTech, totalHours >= 100, totalHours, 100, "h"
            )
        )
    }
}

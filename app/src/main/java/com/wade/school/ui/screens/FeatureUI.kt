package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wade.school.ui.data.BadgeType
import com.wade.school.ui.data.FeatureData
import com.wade.school.ui.data.FeatureGroup
import com.wade.school.ui.data.FeatureItem

@Composable
fun RoleFeatureContent(role: String, onNavigate: (String) -> Unit = {}) {
    val groups = FeatureData.getFeaturesForRole(role)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(groups) { group ->
            Text(
                text = group.groupTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
            )
            group.items.forEach { feature ->
                FeatureRow(feature, onNavigate)
            }
        }
    }
}

@Composable
fun FeatureRow(feature: FeatureItem, onNavigate: (String) -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onNavigate(feature.route) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForName(feature.iconName),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (feature.badge != null) {
                BadgeBox(text = feature.badge, type = feature.badgeType)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun BadgeBox(text: String, type: BadgeType) {
    val containerColor = when (type) {
        BadgeType.URGENT -> Color(0xFFE53935)
        BadgeType.WARNING -> Color(0xFFFB8C00)
        BadgeType.INFO -> Color(0xFF1E88E5)
        BadgeType.SUCCESS -> Color(0xFF43A047)
        else -> Color.Gray
    }
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getIconForName(name: String): ImageVector {
    return when (name) {
        "qr_code" -> Icons.Default.Check
        "mail" -> Icons.Default.Email
        "bar_chart" -> Icons.Default.Info
        "edit_note" -> Icons.Default.Edit
        "campaign" -> Icons.Default.Notifications
        "contact_page" -> Icons.Default.Person
        "grid_view" -> Icons.AutoMirrored.Filled.List
        "schedule" -> Icons.Default.DateRange
        "upload_file" -> Icons.AutoMirrored.Filled.Send
        "assessment" -> Icons.Default.Star
        "event_available" -> Icons.Default.CheckCircle
        "quiz" -> Icons.Default.Info
        "mood" -> Icons.Default.Face
        "signature" -> Icons.Default.Build
        "notifications_active" -> Icons.Default.Notifications
        "chat" -> Icons.AutoMirrored.Filled.Send
        "assignment_ind" -> Icons.Default.AccountBox
        "security" -> Icons.Default.Lock
        "topic" -> Icons.AutoMirrored.Filled.List
        "draw" -> Icons.Default.Create
        "map" -> Icons.Default.Place
        "search" -> Icons.Default.Search
        "camera_alt" -> Icons.Default.CameraAlt
        "archive" -> Icons.Default.Archive
        "calendar_month" -> Icons.Default.CalendarMonth
        "how_to_reg" -> Icons.Default.HowToReg
        "swap_horiz" -> Icons.Default.SwapHoriz
        "drafts" -> Icons.Default.Drafts
        "history" -> Icons.Default.History
        "label" -> Icons.AutoMirrored.Filled.Label
        "groups" -> Icons.Default.Groups
        "event_busy" -> Icons.Default.EventBusy
        "gavel" -> Icons.Default.Gavel
        "event" -> Icons.Default.Event
        "assignment" -> Icons.AutoMirrored.Filled.Assignment
        "summarize" -> Icons.Default.Summarize
        "meeting_room" -> Icons.Default.MeetingRoom
        "build" -> Icons.Default.Build
        "inventory" -> Icons.Default.Inventory
        else -> Icons.Default.Star
    }
}

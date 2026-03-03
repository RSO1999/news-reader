package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.viewmodel.InsightsViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun InsightsScreen(
    onUpgrade: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenArticle: (String) -> Unit = {},
    viewModel: InsightsViewModel = remember { InsightsViewModel() },
    userEmail: String? = null,
    userTier: String = "FREE"
) {
    val email = userEmail
    val tier = userTier

    val insights by remember { derivedStateOf { viewModel.insights } }
    val loading by remember { derivedStateOf { viewModel.loading } }
    val error by remember { derivedStateOf { viewModel.error } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = email ?: "guest",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tier:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tier,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (tier != "PREMIUM") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        viewModel.upgradeUser(onUpgrade)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Upgrade to Premium")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    viewModel.logout(onLogout)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Log out")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Insights",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            loading -> {
                Text(text = "Loading insights...")
            }
            error != null -> {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
            insights != null -> {
                val daily = insights!!.dailyUsage
                val serverTier = insights!!.user.tier
                val showUsage = serverTier != "PREMIUM"
                val progress = if (daily.isUnlimited || daily.limit == 0) 1f else (daily.reads.toFloat() / daily.limit.toFloat()).coerceIn(0f, 1f)

                if (showUsage) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Daily usage", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (daily.isUnlimited) "Unlimited" else "${daily.reads} of ${daily.limit} reads",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Top sections", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        insights!!.topSections.forEach { section ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = section.section, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = "${section.views}", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { section.percent.toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Recent history", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            items(insights!!.recentHistory) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenArticle(item.articleId) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.sourceName} • ${item.publishedAt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.snippet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                if (showUsage && !daily.isUnlimited && daily.reads >= daily.limit) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You are at your daily limit. Upgrade for unlimited reads.",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Saved articles will appear here when the saved endpoint is available.")
    }
}

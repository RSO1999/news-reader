package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.theme.LocalAppColors
import com.storystream.reader_app.ui.components.ContextEntityCard
import com.storystream.reader_app.ui.components.SaveButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.storystream.reader_app.ui.viewmodel.ArticleDetailViewModel
import com.storystream.reader_app.repository.AuthStateHolder
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import coil.compose.AsyncImage

@Composable
fun ArticleDetailScreen(articleId: String, onBack: () -> Unit = {}, onRequireAuth: () -> Unit = {}, viewModel: ArticleDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), userTier: String = "FREE") {
    val article by remember { derivedStateOf { viewModel.article } }
    val contextEntities by remember { derivedStateOf { viewModel.contextEntities } }
    val loading by remember { derivedStateOf { viewModel.loading } }
    val error by remember { derivedStateOf { viewModel.error } }
    val gated by remember { derivedStateOf { viewModel.gated } }
    val contextLoading by remember { derivedStateOf { viewModel.contextLoading } }
    val contextLocked by remember { derivedStateOf { viewModel.contextLocked } }
    val isSaved by remember { derivedStateOf { viewModel.isSaved } }

    // Fetch article on load
    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    // Derive premium status from AuthStateHolder for instant reactivity after upgrade
    val authState by AuthStateHolder.authState.collectAsState()
    val isPremium = authState.tier == "PREMIUM"
    val appColors = LocalAppColors.current

    AppTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                return@Column
            }

            if (gated) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Read limit reached",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please upgrade or log in to continue reading.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.upgradeUser(articleId) }) {
                            Text(text = "Upgrade to Premium")
                        }
                    }
                }
                return@Column
            }

            if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadArticle(articleId) }) {
                            Text(text = "Retry")
                        }
                    }
                }
                return@Column
            }

            // Article content
            article?.let { art ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero image with back button overlay
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (!art.imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = art.imageUrl,
                                contentDescription = art.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📰", style = MaterialTheme.typography.displayMedium)
                            }
                        }

                        // Back button overlay
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Article body
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        // Section badge
                        Text(
                            text = art.section.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontSize = 10.sp
                            ),
                            color = appColors.sectionBadge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                            text = art.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Meta info
                        Text(
                            text = "${art.sourceName} · ${art.publishedAt}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Snippet / body
                        Text(
                            text = art.snippet,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 26.sp
                        )

                        // Placeholder paragraphs
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This article continues with additional reporting and analysis. Full article content is available at the source.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // External link button
                        if (art.externalUrl.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { /* Open external URL */ },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Read Full Article")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save button row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SaveButton(
                                isSaved = isSaved,
                                onClick = { viewModel.saveArticle(articleId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaved) "Saved" else "Save for later",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // ---- Context Panel ----
                        if (isPremium) {
                            Text(
                                text = "AI Context",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (contextEntities.isEmpty()) {
                                if (contextLoading) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Loading AI context...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (contextLocked) {
                                    Text(
                                        text = "AI Context locked. Upgrade to Premium.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.loadContext(articleId) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = "Load AI Context")
                                    }
                                }
                            }

                            if (contextEntities.isNotEmpty()) {
                                Column {
                                    contextEntities.forEachIndexed { idx, e ->
                                        ContextEntityCard(entity = e, index = idx)
                                    }
                                }
                            }
                        } else {
                            // Non-premium context panel
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✨ AI Context",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Upgrade to Premium to view AI-generated context and insights.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.upgradeUser(articleId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = appColors.premium,
                                            contentColor = appColors.premiumForeground
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = "Upgrade to Premium")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

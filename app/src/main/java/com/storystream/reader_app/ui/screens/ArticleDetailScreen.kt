package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.data.LocalStore
import com.storystream.reader_app.data.UserSession
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.data.ContextEntity
import com.storystream.reader_app.ui.components.ContextEntityCard
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder

@Composable
fun ArticleDetailScreen(articleId: String, onBack: () -> Unit = {}, onRequireAuth: () -> Unit = {}) {
    val repo = remember { ArticlesRepository() }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var gated by remember { mutableStateOf(false) }
    var article by remember { mutableStateOf<com.storystream.reader_app.data.ArticleResponse?>(null) }
    var contextLoading by remember { mutableStateOf(false) }
    var contextEntities by remember { mutableStateOf<List<ContextEntity>>(emptyList()) }
    var contextLocked by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    // Fetch article on load
    LaunchedEffect(articleId) {
        loading = true
        error = null
        gated = false
        val res = repo.getArticle(articleId)
        loading = false
        if (res.isSuccess) {
            val resp = res.getOrNull()!!
            if (resp.isSuccessful) {
                article = resp.body()
                // record view locally
                LocalStore.recordView(articleId)
            } else {
                if (resp.code() == 403) {
                    gated = true
                } else {
                    error = "Failed to load article: ${resp.code()}"
                }
            }
        } else {
            error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load article"
        }
    }

    val isPremium = UserSession.tier == "PREMIUM"
    val coroutineScope = rememberCoroutineScope()

    AppTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp)) {

            Masthead(title = "Article", showBack = true, onBack = onBack)

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (gated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Read limit reached. Please upgrade or log in to continue.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequireAuth) {
                        Text(text = "Upgrade / Login")
                    }
                }
                return@Column
            }

            if (error != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = error!!)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* retry */ }) {
                        Text(text = "Retry")
                    }
                }
                return@Column
            }

            // article should be non-null here
            article?.let { art ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = art.title, style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${art.sourceName} • ${art.publishedAt}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = art.snippet, style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Body placeholder
                    Text(text = "Full article would render here or open externalUrl.")

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        IconButton(onClick = {
                            isSaved = true
                            coroutineScope.launch {
                                repo.saveArticle(articleId)
                            }
                        }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isSaved) "Saved" else "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Context panel — LOAD ON DEMAND for premium users only
                    if (isPremium) {
                        if (contextEntities.isEmpty()) {
                            if (contextLoading) {
                                Text(text = "Loading AI context...")
                            } else if (contextLocked) {
                                Text(text = "AI Context locked. Upgrade to Premium.")
                            } else {
                                // Show a button to load context on demand
                                Button(onClick = {
                                    contextLocked = false
                                    coroutineScope.launch {
                                        contextLoading = true
                                        val cres = repo.getContext(articleId)
                                        contextLoading = false
                                        if (cres.isSuccess) {
                                            val r = cres.getOrNull()!!
                                            if (r.isSuccessful) {
                                                val body = r.body()
                                                if (body != null) {
                                                    contextEntities = body.entities
                                                }
                                            } else if (r.code() == 403) {
                                                contextLocked = true
                                            } else {
                                                // optional: set error or show toast; keep minimal
                                            }
                                        } else {
                                            // network failure - keep minimal
                                        }
                                    }
                                }) {
                                    Text(text = "Load AI Context")
                                }
                            }
                        }

                        if (contextEntities.isNotEmpty()) {
                            Column {
                                contextEntities.forEach { e ->
                                    ContextEntityCard(entity = e)
                                }
                            }
                        }
                    } else {
                        Text(text = "AI Context is a Premium feature. Upgrade to view.")
                    }
                }
            }
        }
    }
}

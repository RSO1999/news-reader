package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.ui.components.ArticleCard
import com.storystream.reader_app.ui.viewmodel.SavedViewModel
import kotlinx.coroutines.launch

@Composable
fun SavedScreen(onOpenArticle: (String) -> Unit = {}, vmParam: SavedViewModel? = null) {
    // use provided SavedViewModel or create one
    val vm: SavedViewModel = vmParam ?: androidx.compose.runtime.remember { SavedViewModel() }
    val saved = vm.saved
    val loading = vm.loading
    val error = vm.error

    val scope = rememberCoroutineScope()
    val repo = androidx.compose.runtime.remember { ArticlesRepository() }

    when {
        loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { vm.refresh() }) {
                    Text(text = "Retry")
                }
            }
        }
        saved.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No saved articles yet.")
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = saved) { article: ArticleResponse ->
                    ArticleCard(article = article, onClick = { onOpenArticle(article.id) }, onSave = { id ->
                        scope.launch {
                            val res = repo.saveArticle(id)
                            if (res.isSuccess) {
                                // ensure SavedViewModel reloads its data from server
                                vm.refresh()
                            }
                        }
                    })
                }
            }
        }
    }
}


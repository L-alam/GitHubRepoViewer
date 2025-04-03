package com.example.githubrepoviewer.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubrepoviewer.Repository
import com.example.githubrepoviewer.RepositoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoriesScreen(viewModel: RepositoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // State to track selected repository for detail view
    var selectedRepository by remember { mutableStateOf<Repository?>(null) }

    // Show repository detail screen if a repository is selected
    selectedRepository?.let { repository ->
        RepositoryDetailScreen(
            repository = repository,
            onBackClick = { selectedRepository = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar section
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("GitHub Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (username.isNotEmpty()) {
                            viewModel.searchRepositories(username)
                            // Scroll to top when searching
                            coroutineScope.launch {
                                listState.scrollToItem(0)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Search button
        Button(
            onClick = {
                if (username.isNotEmpty()) {
                    viewModel.searchRepositories(username)
                    // Scroll to top when searching
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search Repositories")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Repository list or status messages
        when {
            uiState.isLoading -> {
                InitialLoadingIndicator()
            }
            uiState.error != null && uiState.repositories.isEmpty() -> {
                ErrorMessage(uiState.error!!)
            }
            uiState.repositories.isEmpty() -> {
                EmptyStateMessage()
            }
            else -> {
                // Display repositories with pagination
                RepositoryList(
                    repositories = uiState.repositories,
                    hasNextPage = uiState.hasNextPage,
                    isLoadingNextPage = uiState.isLoadingNextPage,
                    listState = listState,
                    onLoadMore = { viewModel.loadNextPage() },
                    pageInfo = "Page ${uiState.currentPage} of ${uiState.totalPages}",
                    onRepositoryClick = { selectedRepository = it }
                )
            }
        }
    }
}

@Composable
fun RepositoryList(
    repositories: List<Repository>,
    hasNextPage: Boolean,
    isLoadingNextPage: Boolean,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    pageInfo: String,
    onRepositoryClick: (Repository) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Page info header
            item {
                Text(
                    text = pageInfo,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Repository items
            items(repositories) { repository ->
                RepositoryItem(
                    repository = repository,
                    onClick = { onRepositoryClick(repository) }
                )
            }

            // Footer with load more button or loading indicator
            if (hasNextPage || isLoadingNextPage) {
                item {
                    LoadMoreSection(
                        isLoading = isLoadingNextPage,
                        onLoadMore = onLoadMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RepositoryItem(
    repository: Repository,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Repository name
            Text(
                text = repository.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Full name
            Text(
                text = repository.fullName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            repository.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats and language row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Language
                repository.language?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Stars and forks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐ ${repository.stars}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "🍴 ${repository.forks}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun LoadMoreSection(
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp)
            )
        } else {
            Button(onClick = onLoadMore) {
                Text("Load More")
            }
        }
    }
}

@Composable
fun InitialLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading repositories...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Enter a GitHub username to view their repositories",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
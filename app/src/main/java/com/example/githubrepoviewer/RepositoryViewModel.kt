package com.example.githubrepoviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubrepoviewer.util.extractLinks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * ViewModel for managing GitHub repository data and UI state
 * This improved version includes better error handling and cleaner state management
 */
class RepositoryViewModel : ViewModel() {

    // UI state for the repository screen
    private val _uiState = MutableStateFlow(RepositoriesUiState())
    val uiState: StateFlow<RepositoriesUiState> = _uiState

    /**
     * Fetch repositories for a given username
     * @param username GitHub username
     */
    fun searchRepositories(username: String) {
        // Reset state for new search
        _uiState.value = RepositoriesUiState(isLoading = true)

        fetchRepositories(username, 1, true)
    }

    /**
     * Load the next page of repositories
     */
    fun loadNextPage() {
        val currentState = _uiState.value

        // Skip if already loading or no more pages
        if (currentState.isLoadingNextPage || !currentState.hasNextPage) {
            return
        }

        _uiState.update { it.copy(isLoadingNextPage = true) }

        val username = currentState.username ?: return
        val nextPage = currentState.currentPage + 1

        fetchRepositories(username, nextPage, false)
    }

    /**
     * Retry the most recent request that failed
     */
    fun retryLastRequest() {
        val currentState = _uiState.value
        val username = currentState.username ?: return

        if (currentState.repositories.isEmpty()) {
            // Retry initial load
            searchRepositories(username)
        } else {
            // Retry loading next page
            loadNextPage()
        }
    }

    /**
     * Fetch repositories from the API with comprehensive error handling
     * @param username GitHub username
     * @param page Page number to fetch
     * @param isNewSearch Whether this is a new search or pagination
     */
    private fun fetchRepositories(username: String, page: Int, isNewSearch: Boolean) {
        viewModelScope.launch {
            try {
                val response = GitHubApiClient.apiService.getUserRepositories(
                    username = username,
                    page = page,
                    perPage = 10 // Set a reasonable page size
                )

                if (response.isSuccessful) {
                    val repositories = response.body() ?: emptyList()

                    // Parse pagination links from response header
                    val linkHeader = response.headers()["Link"]
                    val linkMap = linkHeader.extractLinks()

                    // Determine if there's a next page
                    val hasNextPage = linkMap.containsKey("next")

                    // Get total pages from "last" link if available
                    val lastPageUrl = linkMap["last"]
                    val totalPages = if (lastPageUrl != null) {
                        val pageMatch = Regex("page=(\\d+)").find(lastPageUrl)
                        pageMatch?.groupValues?.get(1)?.toIntOrNull() ?: page
                    } else {
                        page
                    }

                    // Keep current repositories for pagination, or empty list for new search
                    val currentRepos = if (isNewSearch) emptyList() else _uiState.value.repositories

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingNextPage = false,
                            repositories = currentRepos + repositories,
                            error = null,
                            username = username,
                            currentPage = page,
                            hasNextPage = hasNextPage,
                            totalPages = totalPages
                        )
                    }
                } else {
                    handleHttpError(response.code(), username, isNewSearch)
                }
            } catch (e: Exception) {
                handleException(e, username, isNewSearch)
            }
        }
    }

    /**
     * Handle HTTP errors with specific error messages
     */
    private fun handleHttpError(code: Int, username: String, isNewSearch: Boolean) {
        val errorMessage = when (code) {
            404 -> "User '$username' not found. Please check the username and try again."
            403 -> "API rate limit exceeded. Please try again later."
            401 -> "Authentication required. The API requires authentication."
            503 -> "GitHub service is unavailable. Please try again later."
            500 -> "GitHub server error. Please try again later."
            else -> "Error: HTTP $code - Something went wrong."
        }

        updateStateWithError(errorMessage, username, isNewSearch)
    }

    /**
     * Handle exceptions with user-friendly error messages
     */
    private fun handleException(e: Exception, username: String, isNewSearch: Boolean) {
        val errorMessage = when (e) {
            is HttpException -> "Network error (${e.code()}). Please try again."
            is SocketTimeoutException -> "Connection timed out. Please check your internet and try again."
            is IOException -> "Network error. Please check your internet connection."
            else -> "Error: ${e.localizedMessage ?: "Unknown error"}"
        }

        updateStateWithError(errorMessage, username, isNewSearch)
    }

    /**
     * Update state with error information
     */
    private fun updateStateWithError(errorMessage: String, username: String, isNewSearch: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingNextPage = false,
                error = errorMessage,
                username = if (isNewSearch) username else it.username
            )
        }
    }
}

/**
 * Data class representing the UI state for repositories screen
 * Includes all information needed to render the UI
 */
data class RepositoriesUiState(
    val isLoading: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val repositories: List<Repository> = emptyList(),
    val error: String? = null,
    val username: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false
) {
    // Computed property to check if there's any repositories to display
    val hasRepositories: Boolean get() = repositories.isNotEmpty()

    // Computed property to format the page info text
    val pageInfoText: String get() = "Page $currentPage of $totalPages"
}
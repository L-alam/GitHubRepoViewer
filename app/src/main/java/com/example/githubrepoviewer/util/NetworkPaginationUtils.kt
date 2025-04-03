package com.example.githubrepoviewer.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter

/**
 * Utility composable to implement "infinite scroll" behavior with a LazyListState.
 * It will trigger the onLoadMore callback when the user scrolls close to the end of the list.
 *
 * @param listState The LazyListState to monitor
 * @param buffer Number of items from the end to trigger loading more
 * @param onLoadMore Callback to invoke when more data should be loaded
 */
@Composable
fun InfiniteScrollHandler(
    listState: LazyListState,
    buffer: Int = 3,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            // Return true if we're at the end of the list minus our buffer
            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .filter { it }
            .collect {
                onLoadMore()
            }
    }
}

/**
 * Extension function for parsing GitHub API pagination link headers
 *
 * @return Map of rel values (next, last, first, prev) to their URLs
 */
fun String?.extractLinks(): Map<String, String> {
    if (this == null || this.isEmpty()) return emptyMap()

    val linkMap = mutableMapOf<String, String>()

    val links = this.split(",")
    for (link in links) {
        val segments = link.split(";")
        if (segments.size < 2) continue

        val urlPart = segments[0].trim()
        val relPart = segments[1].trim()

        val urlMatch = Regex("<(.+)>").find(urlPart)
        val url = urlMatch?.groupValues?.get(1) ?: continue

        val relMatch = Regex("rel=\"(.+)\"").find(relPart)
        val rel = relMatch?.groupValues?.get(1) ?: continue

        linkMap[rel] = url
    }

    return linkMap
}
package com.example.githubrepoviewer

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit interface for GitHub API
 */
interface GitHubApiService {
    /**
     * Fetch repositories for a specific user with pagination
     * @param username GitHub username
     * @param perPage Number of items per page (default: 30, max: 100)
     * @param page Page number (1-indexed)
     */
    @GET("users/{username}/repos")
    suspend fun getUserRepositories(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): Response<List<Repository>>
}

/**
 * Singleton object for GitHub API client
 */
object GitHubApiClient {
    private const val BASE_URL = "https://api.github.com/"

    // Create OkHttpClient with logging interceptor
    private val httpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Create Moshi instance
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Create Retrofit instance
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Create API service
    val apiService: GitHubApiService = retrofit.create(GitHubApiService::class.java)
}

/**
 * Helper class to parse GitHub pagination links from response headers
 */
class PaginationLinks(linkHeader: String?) {
    var nextPage: Int? = null
    var lastPage: Int? = null

    init {
        if (!linkHeader.isNullOrEmpty()) {
            // Parse the Link header
            // Format: <https://api.github.com/user/repos?page=2>; rel="next", <https://api.github.com/user/repos?page=10>; rel="last"
            val links = linkHeader.split(",")

            for (link in links) {
                val segments = link.split(";")
                if (segments.size < 2) continue

                val urlPart = segments[0].trim()
                val relPart = segments[1].trim()

                // Extract URL
                val urlMatch = Regex("<(.+)>").find(urlPart)
                val url = urlMatch?.groupValues?.get(1) ?: continue

                // Extract page number
                val pageMatch = Regex("page=(\\d+)").find(url)
                val pageNum = pageMatch?.groupValues?.get(1)?.toIntOrNull() ?: continue

                // Set the appropriate page value based on the rel type
                when {
                    relPart.contains("rel=\"next\"") -> nextPage = pageNum
                    relPart.contains("rel=\"last\"") -> lastPage = pageNum
                }
            }
        }
    }

    fun hasNextPage(): Boolean = nextPage != null
}
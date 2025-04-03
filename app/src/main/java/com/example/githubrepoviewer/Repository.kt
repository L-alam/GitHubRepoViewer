package com.example.githubrepoviewer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data model for a GitHub repository
 */
@JsonClass(generateAdapter = true)
data class Repository(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val owner: Owner,
    val description: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "stargazers_count") val stars: Int,
    @Json(name = "forks_count") val forks: Int,
    val language: String?,
    @Json(name = "updated_at") val updatedAt: String
)

/**
 * Data model for a repository owner
 */
@JsonClass(generateAdapter = true)
data class Owner(
    val id: Long,
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String,
    @Json(name = "html_url") val htmlUrl: String
)
package com.example.motormates.data.dto.news

import com.google.gson.annotations.SerializedName

/**
 * DTO principal de la respuesta de la API rss2json.
 */
data class NewsRssResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("feed") val feed: NewsFeedDto,
    @SerializedName("items") val items: List<NewsItemDto>
)

data class NewsFeedDto(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("link") val link: String
)

data class NewsItemDto(
    @SerializedName("title") val title: String,
    @SerializedName("pubDate") val pubDate: String,
    @SerializedName("link") val link: String,
    @SerializedName("guid") val guid: String,
    @SerializedName("author") val author: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("description") val description: String,
    @SerializedName("content") val content: String,
    @SerializedName("enclosure") val enclosure: NewsEnclosureDto?
)

data class NewsEnclosureDto(
    @SerializedName("link") val link: String,
    @SerializedName("type") val type: String
)

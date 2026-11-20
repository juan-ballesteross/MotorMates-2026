package com.example.motormates.data.source.news

import com.example.motormates.data.dto.news.NewsRssResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Servicio Retrofit para conectarse a rss2json y convertir el RSS de un portal automotriz a JSON.
 */
interface NewsApiService {

    // En lugar de requerir inyectarlo por Header, rss2json acepta parámetros de query
    @GET("v1/api.json")
    suspend fun getNewsFeed(
        @Query("rss_url") rssUrl: String = "https://www.caranddriver.com/rss/all.xml",
        @Query("api_key") apiKey: String = "" // Si la API es gratuita, puede dejarse en blanco
    ): NewsRssResponseDto
}

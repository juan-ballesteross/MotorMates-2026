package com.example.motormates.data.repository.news

import com.example.motormates.data.model.news.NewsArticleUi
import com.example.motormates.data.source.news.NewsApiService
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

interface NewsRepository {
    suspend fun getLatestNews(): Result<List<NewsArticleUi>>
}

class NewsRepositoryImpl @Inject constructor(
    private val newsApiService: NewsApiService
) : NewsRepository {

    override suspend fun getLatestNews(): Result<List<NewsArticleUi>> {
        return try {
            val response = newsApiService.getNewsFeed()
            
            if (response.status == "ok") {
                val articles = response.items.map { item ->
                    NewsArticleUi(
                        title = item.title,
                        link = item.link,
                        author = item.author.ifBlank { response.feed.title },
                        pubDate = item.pubDate,
                        thumbnailUrl = item.thumbnail.ifBlank { item.enclosure?.link ?: "" },
                        description = item.description
                    )
                }
                Result.success(articles)
            } else {
                Result.failure(Exception("Error en la API de noticias"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet"))
        } catch (e: HttpException) {
            Result.failure(Exception("No se pudieron cargar las noticias"))
        } catch (e: Exception) {
            Result.failure(Exception("Error inesperado al cargar noticias"))
        }
    }
}

package com.example.motormates.data.repository

import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.CarListing

interface SearchRepository {
    fun getCars(): List<CarListing>
    fun findCarById(id: Int): CarListing?
}

class LocalSearchRepository : SearchRepository {
    override fun getCars(): List<CarListing> = SearchMocks.sampleSearchCars

    override fun findCarById(id: Int): CarListing? = SearchMocks.findById(id)
}

object SearchRepositoryProvider {
    val repository: SearchRepository = LocalSearchRepository()
}

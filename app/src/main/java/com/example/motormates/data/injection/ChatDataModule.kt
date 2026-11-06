package com.example.motormates.data.injection

import com.example.motormates.data.repository.ChatRepository
import com.example.motormates.data.repository.ChatRepositoryImpl
import com.example.motormates.data.source.ChatDataSource
import com.example.motormates.data.source.FirestoreChatDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatDataModule {

    @Binds
    @Singleton
    abstract fun bindChatDataSource(impl: FirestoreChatDataSource): ChatDataSource

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}

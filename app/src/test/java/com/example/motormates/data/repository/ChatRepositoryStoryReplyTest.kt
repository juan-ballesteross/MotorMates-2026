package com.example.motormates.data.repository

import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.StoryReplyRef
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.ChatDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Responder una historia: llega como mensaje directo, con la referencia a la historia. */
class ChatRepositoryStoryReplyTest {

    private val chatDataSource = mockk<ChatDataSource>()
    private val authDataSource = mockk<AuthDataSource>()
    private val userDataSource = mockk<UserDataSource>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)

    private val repository = ChatRepositoryImpl(chatDataSource, authDataSource, userDataSource, notificationRepository)

    @Before
    fun setUp() {
        every { authDataSource.getCurrentUser() } returns AuthUser(id = "me", email = null, displayName = "Diego")
        coEvery { chatDataSource.getOrCreateChat("me", "pepito") } returns "chat-1"
        coEvery { chatDataSource.sendMessage(any(), any(), any(), any()) } just runs
    }

    @Test
    fun `la respuesta va al chat con el duenio y lleva la historia`() = runTest {
        val result = repository.replyToStory("pepito", "story-9", "https://story.jpg", "  ¡Qué nave!  ")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) {
            chatDataSource.sendMessage("chat-1", "me", "¡Qué nave!", StoryReplyRef("story-9", "https://story.jpg"))
        }
    }

    @Test
    fun `al duenio le llega la notificacion de mensaje`() = runTest {
        repository.replyToStory("pepito", "story-9", null, "Hola")

        coVerify(exactly = 1) {
            notificationRepository.notify("pepito", NotificationType.MESSAGE, "me", any(), any())
        }
    }

    @Test
    fun `no se puede responder la historia propia`() = runTest {
        val result = repository.replyToStory("me", "story-9", null, "Hola")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("No puedes responder tu propia historia")
        coVerify(exactly = 0) { chatDataSource.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `una respuesta vacia no se envia`() = runTest {
        val result = repository.replyToStory("pepito", "story-9", null, "   ")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { chatDataSource.getOrCreateChat(any(), any()) }
    }

    @Test
    fun `un mensaje normal del chat no lleva historia`() = runTest {
        repository.sendMessage("chat-1", "pepito", "Hola")

        coVerify(exactly = 1) { chatDataSource.sendMessage("chat-1", "me", "Hola", null) }
    }
}

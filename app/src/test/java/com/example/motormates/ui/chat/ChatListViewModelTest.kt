package com.example.motormates.ui.chat

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.User
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.ChatRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Mensajes: el buscador encuentra personas (aunque no haya chat) y deja seguirlas. */
class ChatListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chatRepository: ChatRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val followRepository: FollowRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val following = MutableStateFlow<List<String>>(emptyList())

    private fun user(id: String, name: String, username: String) =
        User(id, name, "$username@x.com", username, "", "", null, null, 0, 0)

    @Before
    fun setUp() {
        every { authRepository.getCurrentUser() } returns AuthUser("yo", null, "Yo")
        every { chatRepository.observeChats() } returns flowOf(Result.success(emptyList()))
        every { followRepository.observeFollowingIds("yo") } returns following
        coEvery { userRepository.getAllRegisteredUsers() } returns Result.success(
            listOf(
                user("yo", "Yo Mismo", "yo"),
                user("u1", "Laura Gómez", "laurag"),
                user("u2", "Luis Pérez", "luchop"),
                user("u3", "Andrés Ruiz", "andresr")
            )
        )
        coEvery { followRepository.followUser(any(), any()) } returns Result.success(Unit)
        coEvery { followRepository.unfollowUser(any(), any()) } returns Result.success(Unit)
    }

    private fun newViewModel() = ChatListViewModel(chatRepository, userRepository, followRepository, authRepository)

    @Test
    fun `sin buscar no se cargan personas`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.people).isEmpty()
        coVerify(exactly = 0) { userRepository.getAllRegisteredUsers() }
    }

    @Test
    fun `buscar encuentra personas por nombre o usuario y nunca a mi mismo`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateSearchQuery("l")
        advanceUntilIdle()
        assertThat(vm.uiState.value.people.map { it.uid }).containsExactly("u1", "u2")

        vm.updateSearchQuery("@andres")
        assertThat(vm.uiState.value.people.map { it.uid }).containsExactly("u3")

        vm.updateSearchQuery("yo")
        assertThat(vm.uiState.value.people).isEmpty()
        assertThat(vm.uiState.value.hasNoSearchResults).isTrue()
    }

    @Test
    fun `seguir es optimista y lo confirma el listener`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.updateSearchQuery("laura"); advanceUntilIdle()

        vm.toggleFollow("u1"); advanceUntilIdle()
        assertThat(vm.uiState.value.followingIds).contains("u1")
        coVerify { followRepository.followUser("yo", "u1") }

        following.value = listOf("u1"); advanceUntilIdle()
        vm.toggleFollow("u1"); advanceUntilIdle()
        assertThat(vm.uiState.value.followingIds).doesNotContain("u1")
        coVerify { followRepository.unfollowUser("yo", "u1") }
    }

    @Test
    fun `si seguir falla el boton vuelve a como estaba`() = runTest {
        coEvery { followRepository.followUser("yo", "u2") } returns Result.failure(Exception("Sin red"))
        val vm = newViewModel()
        advanceUntilIdle()

        vm.toggleFollow("u2"); advanceUntilIdle()

        assertThat(vm.uiState.value.followingIds).doesNotContain("u2")
    }
}

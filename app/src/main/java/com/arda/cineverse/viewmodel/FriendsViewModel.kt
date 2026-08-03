package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.model.FriendRequest
import com.arda.cineverse.data.model.FriendSearchResult
import com.arda.cineverse.data.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class FriendsTab { FRIENDS, REQUESTS }

data class FriendsUiState(
    val selectedTab: FriendsTab = FriendsTab.FRIENDS,
    val friends: List<Friend> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val isLoadingRequests: Boolean = false,
    val searchQuery: String = "",
    val searchResult: FriendSearchResult? = null,
    val searchError: String? = null,
    val isSearching: Boolean = false,
    // Kısa süreliğine gösterilecek toast benzeri mesaj (istek gönderildi,
    // offline hatası, vb.) — bkz. MyListViewModel.offlineMessage deseni.
    val actionMessage: String? = null,
) {
    val isAlreadyFriend: Boolean
        get() = searchResult != null && friends.any { it.friendUid == searchResult.uid }
}

class FriendsViewModel(
    private val repository: FriendRepository = FriendRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeFriends().collect { friends ->
                _uiState.value = _uiState.value.copy(friends = friends)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.syncFriends()
            loadIncomingRequests()
        }
    }

    fun selectTab(tab: FriendsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == FriendsTab.REQUESTS) loadIncomingRequests()
    }

    private fun loadIncomingRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRequests = true)
            repository.getIncomingRequests().fold(
                onSuccess = { requests ->
                    _uiState.value = _uiState.value.copy(incomingRequests = requests, isLoadingRequests = false)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingRequests = false)
                },
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query.lowercase(),
            searchResult = null,
            searchError = null,
        )
    }

    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null, searchResult = null)
            repository.searchUserByUsername(query).fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResult = result,
                        searchError = if (result == null) "Kullanıcı bulunamadı" else null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchError = error.message ?: "Arama başarısız",
                    )
                },
            )
        }
    }

    fun sendFriendRequest(target: FriendSearchResult) {
        viewModelScope.launch {
            repository.sendFriendRequest(target).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        actionMessage = "İstek gönderildi",
                        searchResult = null,
                        searchQuery = "",
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(actionMessage = error.message ?: "İstek gönderilemedi")
                },
            )
        }
    }

    fun acceptRequest(request: FriendRequest) {
        // İyimser güncelleme: istek listeden anında kaybolur.
        val previous = _uiState.value.incomingRequests
        _uiState.value = _uiState.value.copy(incomingRequests = previous.filterNot { it.fromUid == request.fromUid })
        viewModelScope.launch {
            repository.acceptFriendRequest(request).onFailure { error ->
                _uiState.value = _uiState.value.copy(incomingRequests = previous, actionMessage = error.message)
            }
        }
    }

    fun declineRequest(fromUid: String) {
        val previous = _uiState.value.incomingRequests
        _uiState.value = _uiState.value.copy(incomingRequests = previous.filterNot { it.fromUid == fromUid })
        viewModelScope.launch {
            repository.declineFriendRequest(fromUid).onFailure { error ->
                _uiState.value = _uiState.value.copy(incomingRequests = previous, actionMessage = error.message)
            }
        }
    }

    fun removeFriend(friendUid: String) {
        val previous = _uiState.value.friends
        _uiState.value = _uiState.value.copy(friends = previous.filterNot { it.friendUid == friendUid })
        viewModelScope.launch {
            repository.removeFriend(friendUid).onFailure { error ->
                _uiState.value = _uiState.value.copy(friends = previous, actionMessage = error.message)
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}

package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.model.Friend
import com.arda.cineverse.data.model.FriendRequest
import com.arda.cineverse.data.model.FriendSearchResult
import com.arda.cineverse.data.repository.FriendRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    // İsteği gönderilmekte olan kullanıcı — buton bu sırada kilitli kalır ki
    // arka arkaya basılıp aynı kişiye ikinci bir istek denenmesin (ikincisi
    // firestore.rules'ta create-only kuralına takılıp hata döndürürdü).
    val sendingRequestUid: String? = null,
    // Bu oturumda istek gönderilen kullanıcılar. Firestore'dan okunamıyor:
    // friendRequests/{fromUid} belgesini SADECE alıcı okuyabiliyor (bkz.
    // firestore.rules), gönderen kendi gönderdiği isteği göremiyor. Bu yüzden
    // "Gönderildi" bilgisi bellekte tutuluyor — uygulama yeniden açılınca
    // sıfırlanır.
    val sentRequestUids: Set<String> = emptySet(),
    val actionMessage: String? = null,
    val selectedFriendForActivity: Friend? = null,
    val friendActivities: List<com.arda.cineverse.data.model.FriendActivity> = emptyList(),
    val isLoadingActivities: Boolean = false,
) {
    val isAlreadyFriend: Boolean
        get() = searchResult != null && friends.any { it.friendUid == searchResult.uid }

    val isRequestSentToResult: Boolean
        get() = searchResult != null && searchResult.uid in sentRequestUids
}

class FriendsViewModel(
    private val repository: FriendRepository = FriendRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState

    private var searchJob: Job? = null

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

    /**
     * Yazdıkça arar — ayrı bir "Ara" butonuna basmaya gerek yok. Her tuş
     * vuruşunda sorgu atmamak için önceki bekleyen arama iptal edilip
     * [SEARCH_DEBOUNCE_MS] kadar bekleniyor. Kullanıcı adları en az 3 karakter
     * olduğundan (bkz. FriendRepository.USERNAME_REGEX) daha kısa girdilerde
     * hiç sorgu atılmıyor — aksi halde her "a" harfinde boşuna bir Firestore
     * okuması ve anlık "Kullanıcı bulunamadı" hatası görünürdü.
     */
    fun onSearchQueryChange(query: String) {
        val normalized = query.lowercase()
        _uiState.value = _uiState.value.copy(
            searchQuery = normalized,
            searchResult = null,
            searchError = null,
            isSearching = false,
        )
        searchJob?.cancel()
        if (normalized.trim().length < MIN_SEARCH_LENGTH) return
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            search()
        }
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

    /**
     * Sonucu ekrandan KALDIRMIYORUZ — kart yerinde kalıp butonu "Gönderildi"ye
     * dönüşüyor. Eskiden arama kutusu da temizlendiğinden kart bir anda yok
     * oluyordu ve isteğin gerçekten gidip gitmediği belirsiz kalıyordu.
     */
    fun sendFriendRequest(target: FriendSearchResult) {
        if (_uiState.value.sendingRequestUid != null) return
        _uiState.value = _uiState.value.copy(sendingRequestUid = target.uid)
        viewModelScope.launch {
            repository.sendFriendRequest(target).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        sendingRequestUid = null,
                        sentRequestUids = _uiState.value.sentRequestUids + target.uid,
                        actionMessage = "@${target.username} kullanıcısına istek gönderildi",
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        sendingRequestUid = null,
                        actionMessage = error.message ?: "İstek gönderilemedi",
                    )
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

    fun selectFriendForActivity(friend: Friend?) {
        if (friend == null) {
            _uiState.value = _uiState.value.copy(
                selectedFriendForActivity = null,
                friendActivities = emptyList(),
                isLoadingActivities = false,
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedFriendForActivity = friend,
            friendActivities = emptyList(),
            isLoadingActivities = true,
        )

        viewModelScope.launch {
            repository.getFriendRecentActivities(friend.friendUid).fold(
                onSuccess = { activities ->
                    _uiState.value = _uiState.value.copy(
                        friendActivities = activities,
                        isLoadingActivities = false,
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        friendActivities = emptyList(),
                        isLoadingActivities = false,
                    )
                },
            )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MIN_SEARCH_LENGTH = 3
    }
}

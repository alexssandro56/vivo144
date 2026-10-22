package com.example.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.GameProfile
import com.example.data.models.OverlayPreferences
import com.example.data.repositories.GameProfileRepository
import com.example.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameProfilesUiState(
    val games: List<GameProfile> = emptyList(),
    val searchQuery: String = "",
    val editingGame: GameProfile? = null,
    val isAddEditDialogOpen: Boolean = false,
    val overlayPreferences: OverlayPreferences = OverlayPreferences.DEFAULT,
    val userNotification: String? = null
)

class GameProfilesViewModel(
    private val gameProfileRepository: GameProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _editingGame = MutableStateFlow<GameProfile?>(null)
    private val _isAddEditDialogOpen = MutableStateFlow(false)
    private val _userNotification = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GameProfilesUiState> = combine(
        combine(
            gameProfileRepository.getAllGameProfiles(),
            _searchQuery,
            _editingGame
        ) { games, query, editing ->
            Triple(games, query, editing)
        },
        combine(
            _isAddEditDialogOpen,
            userPreferencesRepository.overlayPreferencesFlow,
            _userNotification
        ) { isOpen, overlayPrefs, notif ->
            Triple(isOpen, overlayPrefs, notif)
        }
    ) { (games, query, editing), (isOpen, overlayPrefs, notif) ->
        val filtered = if (query.isBlank()) {
            games
        } else {
            games.filter {
                it.gameName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        GameProfilesUiState(
            games = filtered,
            searchQuery = query,
            editingGame = editing,
            isAddEditDialogOpen = isOpen,
            overlayPreferences = overlayPrefs,
            userNotification = notif
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameProfilesUiState()
    )

    init {
        viewModelScope.launch {
            gameProfileRepository.initializeDefaultsIfEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun updateOverlayPreferences(prefs: OverlayPreferences) {
        userPreferencesRepository.saveOverlayPreferences(prefs)
        _userNotification.value = if (prefs.isOverlayEnabled) "FPS Overlay started" else "FPS Overlay stopped"
    }

    fun openAddDialog() {
        _editingGame.value = null
        _isAddEditDialogOpen.value = true
    }

    fun openEditDialog(game: GameProfile) {
        _editingGame.value = game
        _isAddEditDialogOpen.value = true
    }

    fun closeDialog() {
        _editingGame.value = null
        _isAddEditDialogOpen.value = false
    }

    fun saveGameProfile(game: GameProfile) {
        viewModelScope.launch {
            if (game.id == 0L) {
                gameProfileRepository.addGameProfile(game)
                _userNotification.value = "Game profile added for ${game.gameName}"
            } else {
                gameProfileRepository.updateGameProfile(game)
                _userNotification.value = "Updated configuration for ${game.gameName}"
            }
            closeDialog()
        }
    }

    fun toggleGame(gameId: Long, enabled: Boolean) {
        viewModelScope.launch {
            gameProfileRepository.toggleGameProfile(gameId, enabled)
        }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            gameProfileRepository.deleteGameProfile(gameId)
            _userNotification.value = "Game profile deleted"
        }
    }

    fun clearNotification() {
        _userNotification.value = null
    }

    companion object {
        fun provideFactory(
            gameProfileRepository: GameProfileRepository,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameProfilesViewModel(
                    gameProfileRepository = gameProfileRepository,
                    userPreferencesRepository = userPreferencesRepository
                ) as T
            }
        }
    }
}

package com.zaaam.zreming.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.remote.MaintenanceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import javax.inject.Inject

data class MaintenanceUiState(
    val isChecking: Boolean = true,
    val isMaintenance: Boolean = false,
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val api: MaintenanceApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    init {
        checkMaintenance()
    }

    fun checkMaintenance() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            try {
                val result = api.getMaintenance()
                val isOn = when (result) {
                    is JsonPrimitive -> result.booleanOrNull == true
                    is JsonObject -> {
                        val field = result["maintenance"]
                            ?: result["maintance"]
                            ?: result["enabled"]
                        (field as? JsonPrimitive)?.booleanOrNull == true
                    }
                    else -> false
                }
                _uiState.update {
                    it.copy(isChecking = false, isMaintenance = isOn)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isChecking = false, isMaintenance = false)
                }
            }
        }
    }
}
package com.sergiom.thebestdamkebap.viewmodel.auth

sealed interface AuthEvent {
    data class Error(val text: String) : AuthEvent
    data class Info(val text: String)  : AuthEvent
    object RegisterSuccess : AuthEvent          // ← nuevo
    object NavigateToLogin : AuthEvent          // ← nuevo
}
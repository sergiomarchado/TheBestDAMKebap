package com.sergiom.thebestdamkebap.viewmodel.auth

sealed interface AuthEvent {
    data class Error(val text: String) : AuthEvent
    data class Info(val text: String)  : AuthEvent
    object RegisterSuccess : AuthEvent
    object NavigateToLogin : AuthEvent
}
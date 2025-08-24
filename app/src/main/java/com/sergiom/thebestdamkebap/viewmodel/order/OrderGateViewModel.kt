package com.sergiom.thebestdamkebap.viewmodel.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.order.OrderContext
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OrderGateViewModel @Inject constructor(
    private val session: OrderSessionRepository
) : ViewModel() {

    val context: StateFlow<OrderContext> = session.context

    fun confirmStart(mode: OrderMode, addressId: String?) {
        viewModelScope.launch { session.startOrder(mode, addressId) }
    }

    fun chooseBrowsing() {
        viewModelScope.launch { session.setBrowsingOnly() }
    }

    fun clear() {
        viewModelScope.launch { session.clear() }
    }
}

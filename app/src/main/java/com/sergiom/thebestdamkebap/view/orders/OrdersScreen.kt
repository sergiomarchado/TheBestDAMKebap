// view/orders/OrdersScreen.kt
package com.sergiom.thebestdamkebap.view.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.orders.components.OrderCard
import com.sergiom.thebestdamkebap.view.orders.components.TitleBar
import com.sergiom.thebestdamkebap.viewmodel.orders.OrdersViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
    onOpenCart: () -> Unit = {}   // llevar al carrito tras "Repetir pedido"
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    val nf = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
            currency = Currency.getInstance("EUR")
        }
    }
    val fmt = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
    }

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.isGuest -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inicia sesión para ver tus pedidos.")
        }
        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
        }
        state.list.isEmpty() -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            TitleBar()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes pedidos todavía.")
            }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                TitleBar()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.list, key = { it.id }) { o ->
                        OrderCard(
                            o = o,
                            nf = nf,
                            fmt = fmt,
                            onRepeat = {
                                viewModel.repeatOrder(o.reorderLines)
                                onOpenCart()
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

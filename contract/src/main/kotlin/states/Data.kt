package states

import net.corda.core.serialization.CordaSerializable
import java.time.ZonedDateTime
import java.util.*

@CordaSerializable
enum class AssetType { CURRENCY, STOCK, BOND }

@CordaSerializable
enum class OrderAction { SELL, BUY, EXCHANGE }

@CordaSerializable
enum class AttemptStatus { SUCCEEDED, FAILED }

// Refer here https://tradovate.zendesk.com/hc/en-us/articles/219454917-What-do-different-order-statuses-mean-
@CordaSerializable
enum class OrderStatus { FILLED, WORKING, CANCELLED, REJECTED, SUSPENDED, COMPLETED, EXPIRED }

@CordaSerializable
data class OrderAttempt (
    val status: AttemptStatus,
    val message: String,
    val date: ZonedDateTime
)

@CordaSerializable
data class Order (
    val id: UUID,
    val action: OrderAction,
    val attempt: OrderAttempt? = null,
    val status: OrderStatus,
    val fromCurrency: Currency,
    val toCurrency: Currency
)
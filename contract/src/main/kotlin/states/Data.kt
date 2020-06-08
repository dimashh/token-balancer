package states

import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
enum class AssetType { CURRENCY, STOCK, BOND }

@CordaSerializable
enum class OrderAction { SELL, BUY, EXCHANGE }

@CordaSerializable
data class Order (
    val id: UUID,
    val action: OrderAction,
    val assetType: AssetType,
    val meta: Map<String, String>
)
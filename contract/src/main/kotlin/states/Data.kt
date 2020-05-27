package states

import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
enum class AssetType { Currency, Stock, Bond }

@CordaSerializable
data class Order (
    val id: UUID,
    val assetType: AssetType,
    val meta: Map<String, String>
)
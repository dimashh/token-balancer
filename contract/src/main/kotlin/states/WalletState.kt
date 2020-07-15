package states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import contracts.WalletContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
enum class WalletStatus { OPEN, CLOSED }

@BelongsToContract(WalletContract::class)
data class WalletState(
    val walletId: UUID,
    val baseCurrency: Currency?,
    // Upon balance changes the fungible tokens must be updated
    val tokens: List<FungibleToken>,
    val owner: Party,
    val balance: Long,
    val transactions: List<TransactionState>,
    // TODO create a factory class to work out the calculations based on orders
    // i.e. buy orders decrease available balance
    val orders: List<Order>,
    val status: WalletStatus,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState
package states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import contracts.TradingAccountContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
enum class AccountAction { ISSUE, WITHDRAW, TRANSFER }

@CordaSerializable
enum class AccountStatus { ACTIVE, INACTIVE }

// TODO trading account should be deleted as it now same as the wallet
@BelongsToContract(TradingAccountContract::class)
data class TradingAccountState(
    val accountId: UUID,
    val baseCurrency: Currency,
    val tokens: Map<String, FungibleToken>,
    val balance: Long,
    val owner: Party,
    // TODO create a factory class to work out the calculations based on orders
    // i.e. buy orders decrease available balance
    val orders: List<Order>,
    val transfers: List<TransactionState>,
    val status: AccountStatus,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    
}
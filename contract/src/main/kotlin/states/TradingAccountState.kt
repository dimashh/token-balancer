package states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import contracts.TradingAccountContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

enum class AccountAction { ISSUE, WITHDRAW }

@BelongsToContract(TradingAccountContract::class)
data class TradingAccountState(
    val accountId: UUID,
    val balance: FungibleToken,
    val owner: Party,
    val orders: List<TransactionState>,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier
): LinearState {

}
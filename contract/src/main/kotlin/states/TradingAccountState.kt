package states

import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import contracts.TradingAccountContract
import net.corda.core.contracts.Amount
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
    val balance: Amount<IssuedTokenType>,
    val owner: Party,
    val transactions: List<TransactionState>,
    //TODO add status to track lifecycle
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {

}
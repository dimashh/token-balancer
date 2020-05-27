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
    val fiatToken: FungibleToken,
    val owner: Party,
    val transactions: List<TransactionState>,
    val status: WalletStatus,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {

    fun getBalance(): Long {
        return fiatToken.amount.quantity
    }

    fun getIssuer(): Party {
        return fiatToken.issuer
    }

    fun getCurrencyCode(): String {
        return fiatToken.issuedTokenType.tokenIdentifier
    }
}
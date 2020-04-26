package states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import contracts.WalletContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(WalletContract::class)
data class WalletState(
    val fiatToken: FungibleToken,
    val owner: Party,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {

    fun getBalance(): Long {
        return fiatToken.amount.quantity
    }

    fun getCurrencyCode(): String {
        return fiatToken.issuedTokenType.tokenIdentifier
    }
}
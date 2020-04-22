package states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class WalletState(
    val fiatTokens: FungibleToken,
    val owner: Party,
    override val linearId: UniqueIdentifier,
    override val participants: List<AbstractParty>
): LinearState {

    fun getBalance(): Long {
        return fiatTokens.amount.quantity
    }

    fun getCurrencyCode(): String {
        return fiatTokens.issuedTokenType.tokenIdentifier
    }
}
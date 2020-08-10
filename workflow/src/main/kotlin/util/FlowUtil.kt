package util

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import states.Order
import states.WalletState

internal fun exchangeTokens(tokens: List<FungibleToken>, order: Order, walletState: WalletState): List<FungibleToken> {
    val fromToken = tokens.singleOrNull { it.issuedTokenType.tokenIdentifier == order.fromCurrency.currencyCode }
            ?: throw FlowException("Token with currency code ${order.fromCurrency.currencyCode} not found.")
    val toToken = tokens.singleOrNull { it.issuedTokenType.tokenIdentifier == order.toCurrency.currencyCode }
        ?: createToken(order.toCurrency.currencyCode, order.total, walletState, fromToken.issuer)
    return listOf(fromToken, toToken)
}

private fun createToken(currencyCode: String, total: Long, walletState: WalletState, issuer: Party): FungibleToken {
    val fiatCurrency = FiatCurrency.getInstance(currencyCode)
    val issuedTokenType = fiatCurrency issuedBy issuer
    return total of issuedTokenType heldBy walletState.owner
}

internal fun buyTokens(tokens: List<FungibleToken>, order: Order, walletState: WalletState) {}

internal fun sellTokens(tokens: List<FungibleToken>, order: Order) {}



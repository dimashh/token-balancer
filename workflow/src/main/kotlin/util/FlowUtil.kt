package util

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.flows.FlowException
import states.Order

internal fun exchangeTokens(tokens: List<FungibleToken>, order: Order): List<FungibleToken> {
    val fromToken =
        tokens.singleOrNull { it.issuedTokenType.tokenIdentifier == order.fromCurrency.currencyCode }?.amount?.quantity
            ?: throw FlowException("Token with currency code ${order.fromCurrency.currencyCode} not found.")
}

internal fun buyTokens(tokens: List<FungibleToken>, order: Order): List<FungibleToken> {

}

internal fun sellTokens(tokens: List<FungibleToken>, order: Order): List<FungibleToken> {

}



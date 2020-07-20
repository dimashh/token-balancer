package tests

import FlowTest
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import states.*
import states.AssetType.CURRENCY
import states.OrderAction.BUY
import workflow.CreateWalletFlow
import workflow.ExecuteOrderFlow
import workflow.IssueFlow
import java.util.*

/**
 *  Add -ea -javaagent:../lib/quasar.jar to VM options
 **/

class ExecuteOrderFlowTest : FlowTest() {

    private val currencyUnit = CurrencyUnit.GBP
    private val testMoney = Money.of(currencyUnit, 10.toBigDecimal())

    private val wallet by lazy {
        runNetwork {
            nodeB.startFlow(CreateWalletFlow.Initiator(currencyUnit.toCurrency(), null, partyB, listOf(partyA, partyB)))
        }.get().tx.outputsOfType<WalletState>().single()
    }

    private val walletStateWithTokens by lazy {
        runNetwork {
            nodeB.startFlow(IssueFlow.Initiator(testMoney, wallet.walletId, receiver = partyB, issuer = partyA))
        }.get().tx.outputsOfType<WalletState>().single()
    }

    @Test
    fun `flow to transfer tokens from wallet to trading account`() {
        // TODO exchange rate should be supplied by an oracle
        val exchangeRate = 0.75.toLong()
        val orderMeta = mapOf(
            "rate" to "$exchangeRate",
            "amount" to "${walletStateWithTokens.balance}",
            "baseCurrency" to walletStateWithTokens.baseCurrency!!.currencyCode,
            "exchangeCurrency" to walletStateWithTokens.baseCurrency!!.currencyCode
        )
        val order = Order(UUID.randomUUID(), BUY, CURRENCY, null, OrderStatus.WORKING, orderMeta)

        runNetwork {
            nodeB.startFlow(ExecuteOrderFlow.Initiator(walletStateWithTokens.walletId, order, partyB))
        }
    }

}
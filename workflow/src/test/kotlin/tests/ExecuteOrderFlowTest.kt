package tests

import FlowTest
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import states.*
import states.AssetType.CURRENCY
import states.OrderAction.BUY
import workflow.ExecuteOrderFlow
import workflow.IssueFlow
import workflow.TransferFlow
import java.util.*

/**
 *  Add -ea -javaagent:../lib/quasar.jar to VM options
 **/

class ExecuteOrderFlowTest : FlowTest() {

    private val testMoney = Money.of(CurrencyUnit.GBP, 10.toBigDecimal())

    private val afterIssueFlow by lazy {
        runNetwork {
            nodeB.startFlow(IssueFlow.Initiator(testMoney, receiver = partyB, issuer = partyA))
        }
    }

    private val walletState by lazy {
        afterIssueFlow.get().tx.outputsOfType<WalletState>().single()
    }

    private val afterTransferFlow by lazy {
        runNetwork {
            nodeB.startFlow(TransferFlow.Initiator(walletState.fiatToken, walletState.walletId, null, AccountAction.ISSUE))
        }
    }

    @Test
    fun `flow to transfer tokens from waller to trading account`() {
        // TODO exchange rate should be supplied by an oracle
        val exchangeRate = 0.75.toLong()
        val orderMeta = mapOf(
            "rate" to "$exchangeRate",
            "amount" to "${walletState.balance}",
            "baseCurrency" to walletState.fiatToken.amount.token.tokenType.tokenIdentifier,
            "exchangeCurrency" to walletState.fiatToken.amount.token.tokenType.tokenIdentifier)
        val tradingAccount = afterTransferFlow.get().tx.outputsOfType<TradingAccountState>().single()
        val order = Order(UUID.randomUUID(), BUY, CURRENCY, null, OrderStatus.WORKING, orderMeta)

        runNetwork {
            nodeB.startFlow(ExecuteOrderFlow.Initiator(tradingAccount.accountId, order, partyB))
        }
    }

}
package tests

import FlowTest
import flows.ExecuteOrderFlow
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import states.*
import states.OrderAction.EXCHANGE
import workflow.CreateWalletFlow
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
        val order = Order(UUID.randomUUID(), EXCHANGE, null, OrderStatus.WORKING, 5.toLong(), currencyUnit.toCurrency(), CurrencyUnit.USD.toCurrency())

        runNetwork {
            nodeB.startFlow(ExecuteOrderFlow.Initiator(walletStateWithTokens.walletId, order, partyC))
        }
    }

}
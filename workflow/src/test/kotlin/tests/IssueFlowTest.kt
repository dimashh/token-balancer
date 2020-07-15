package tests

import FlowTest
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import states.WalletState
import workflow.CreateWalletFlow
import workflow.IssueFlow

/**
 *  Add -ea -javaagent:../lib/quasar.jar to VM options
 **/

class IssueFlowTest : FlowTest() {

    private val currencyUnit = CurrencyUnit.GBP

    private val walletState by lazy {
        runNetwork {
            nodeB.startFlow(CreateWalletFlow.Initiator(currencyUnit.toCurrency(), null, partyB, listOf(partyA, partyB)))
        }.get().tx.outputsOfType<WalletState>().single()
    }
    
    @Test
    fun `flow to issue tokens`() {
        val money = Money.of(currencyUnit, 10.toBigDecimal())
        val flow = IssueFlow.Initiator(money, walletState.walletId, receiver = partyB, issuer = partyA)

        assertDoesNotThrow {
            runNetwork { nodeB.startFlow(flow) }
        }
    }

}
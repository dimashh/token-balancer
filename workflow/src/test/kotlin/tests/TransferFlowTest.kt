package tests

import FlowTest
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import states.AccountAction
import states.WalletState
import workflow.IssueFlow
import workflow.TransferFlow

/**
 *  Add -ea -javaagent:../lib/quasar.jar to VM options
 **/

class TransferFlowTest : FlowTest() {

    @Test
    fun `flow to transfer tokens from waller to trading account`() {
        val money = Money.of(CurrencyUnit.GBP, 10.toBigDecimal())

        runNetwork {
            val walletState = nodeB.startFlow(IssueFlow.Initiator(money, receiver = partyB, issuer = partyA)).get().tx.outputsOfType<WalletState>().single()
            val tokens = walletState.fiatToken

            val tx = nodeB.startFlow(TransferFlow.Initiator(tokens, walletState.walletId, null, AccountAction.ISSUE))
            tx
        }

    }

}
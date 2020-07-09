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

    private val testMoney = Money.of(CurrencyUnit.GBP, 10.toBigDecimal())

    private val afterIssueFlow by lazy {
        runNetwork {
            nodeB.startFlow(IssueFlow.Initiator(testMoney, receiver = partyB, issuer = partyA))
        }
    }

    @Test
    fun `flow to transfer tokens from waller to trading account`() {
        val walletState = afterIssueFlow.get().tx.outputsOfType<WalletState>().single()
        runNetwork {
            nodeB.startFlow(TransferFlow.Initiator(walletState.tokens.values.last(), walletState.walletId, null, AccountAction.ISSUE))
        }
    }

}
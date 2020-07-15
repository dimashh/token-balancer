package tests

import FlowTest
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import workflow.CreateWalletFlow
import workflow.IssueFlow

/**
 *  Add -ea -javaagent:../lib/quasar.jar to VM options
 **/

class CreateWalletFlowTest : FlowTest() {

    @Test
    fun `flow to issue tokens`() {
        val flow = CreateWalletFlow.Initiator(CurrencyUnit.GBP.toCurrency(), null, partyB, listOf(partyA, partyB))

        assertDoesNotThrow {
            runNetwork { nodeB.startFlow(flow) }
        }
    }

}
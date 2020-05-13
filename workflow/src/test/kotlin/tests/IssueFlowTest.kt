package tests

import FlowTest
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import workflow.IssueFlow

/**
 *  Add -ea -javaagent:../lib/quasar.jar to VM options
 */

class IssueFlowTest : FlowTest() {

    @Test
    fun `flow to issue tokens`() {
        val money = Money.of(CurrencyUnit.GBP, 10.toBigDecimal())

        val flow = IssueFlow.Initiator(money, receiver = partyB, issuer = partyA)

        assertDoesNotThrow {
            runNetwork { nodeB.startFlow(flow) }
        }

    }

}
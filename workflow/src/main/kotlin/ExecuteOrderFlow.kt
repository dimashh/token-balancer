package workflow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import states.Order
import java.util.*

// TODO move out to an interface
/**
 *  Flow to execute placed orders
 *  @param tradingAccountId The ID of the account placing the order
 *  @param order The order details to be executed. Must contain all the metadata
 *  @param tradingParty The party making the trade
 */

object ExecuteOrderFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val tradingAccountId: UUID,
        private val order: Order,
        private val tradingParty: Party
        ) : FlowLogic<SignedTransaction>() {

        companion object {
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION :
                ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        }

        fun tracker() = ProgressTracker(
            INITIALISING_TX,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val stx = subFlow(object : SignTransactionFlow(counterPartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    requireThat {
                        // TODO
                    }
                }
            })
            return subFlow(ReceiveFinalityFlow(counterPartySession, stx.id))
        }
    }
}
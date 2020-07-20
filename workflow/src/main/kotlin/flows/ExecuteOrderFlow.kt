package workflow

import co.paralleluniverse.fibers.Suspendable
import contracts.WalletContract
import javassist.NotFoundException
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import states.*
import java.time.ZonedDateTime
import java.util.*

// TODO move out to an interface
/**
 *  Flow to execute placed orders
 *  @param walletId The ID of the wallet placing the order
 *  @param order The order details to be executed. Must contain all the metadata
 *  @param tradingParty The party making the trade
 */

object ExecuteOrderFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val walletId: UUID,
        private val order: Order,
        private val tradingParty: Party
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object GET_WALLET : ProgressTracker.Step("Retrieving user wallet.")
            object EXECUTE_ORDER : ProgressTracker.Step("Executing order at the exchange.")
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        }

        fun tracker() = ProgressTracker(
            GET_WALLET,
            EXECUTE_ORDER,
            INITIALISING_TX,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            progressTracker.currentStep = GET_WALLET
            val walletStateAndRef = serviceHub.vaultService.queryBy<WalletState>()
                .states.singleOrNull { it.state.data.walletId == walletId } ?: throw NotFoundException("Wallet with $walletId not found.")
            val walletState = walletStateAndRef.state.data

            progressTracker.currentStep = EXECUTE_ORDER

            // TODO this should be a subflow to an exchange (or our own node for now) where the order gets verified and processed
            val attempt = OrderAttempt(AttemptStatus.SUCCEEDED, "Order successfully verified.", ZonedDateTime.now())
            val completedOrder = order.copy(attempt = attempt, status = OrderStatus.COMPLETED)
            // TODO calculations must be done and checked before contract verification
            val updatedWalletState = walletState.copy(orders = walletState.orders + completedOrder)

            progressTracker.currentStep = INITIALISING_TX
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)

            txBuilder.addInputState(walletStateAndRef)
            txBuilder.addOutputState(updatedWalletState)
            // TODO check who actually needs to sign
            txBuilder.addCommand(WalletContract.Commands.Update(), walletState.participants.map { it.owningKey })

            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // TODO the transaction must be signed by the party that executes the subflow and an oracle
//            progressTracker.currentStep = GATHERING_SIGS
//            val otherPartySession = initiateFlow()

            progressTracker.currentStep = FINALISING_TRANSACTION
            return signedTx

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
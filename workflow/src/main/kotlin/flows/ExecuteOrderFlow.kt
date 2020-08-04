package flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.toParty
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
import oracle.ExchangeRateOracle.Companion.filterForOracle
import workflow.CreateWalletFlow

// TODO move out to an interface
/**
 *  Flow to execute placed orders
 *  @param walletId The ID of the wallet placing the order
 *  @param order The order details to be executed. Must contain all the metadata
 *  @param tradingParty The party making the trade
 *  @param oracle The oracle party that will verify the order
 */

object ExecuteOrderFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val walletId: UUID,
        private val order: Order,
        private val tradingParty: Party,
        private val oracle: Party
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object GETTING_WALLET : ProgressTracker.Step("Retrieving user wallet.")
            object QUERYING_THE_ORACLE : ProgressTracker.Step("Requesting an exhange rate from an oracle.")
            object UPDATING_ORDER : ProgressTracker.Step("Updating the order details.")
            object UPDATING_WALLET : ProgressTracker.Step("Updating the wallet details.")
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object ORACLE_SIGNING : ProgressTracker.Step("Signing transaction with oracle's key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION :
                ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        }

        private fun tracker() = ProgressTracker(
            GETTING_WALLET,
            QUERYING_THE_ORACLE,
            UPDATING_ORDER,
            UPDATING_WALLET,
            INITIALISING_TX,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            ORACLE_SIGNING,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            progressTracker.currentStep = GETTING_WALLET
            val walletStateAndRef = serviceHub.vaultService.queryBy<WalletState>()
                .states.singleOrNull { it.state.data.walletId == walletId }
                ?: throw NotFoundException("Wallet with $walletId not found.")
            val walletState = walletStateAndRef.state.data

            progressTracker.currentStep = QUERYING_THE_ORACLE
            val fromCurrency = order.fromCurrency.currencyCode
            val toCurrency = order.toCurrency.currencyCode
            val exchangeRateFromOracle = subFlow(ExchangeRateFlow.RequestFlow(oracle, fromCurrency, toCurrency))

            progressTracker.currentStep = UPDATING_ORDER
            val attempt = OrderAttempt(AttemptStatus.SUCCEEDED, "Order successfully verified.", ZonedDateTime.now())
            val completedOrder = order.copy(attempt = attempt, status = OrderStatus.COMPLETED)

            // TODO need to update the token number and total balance too
            progressTracker.currentStep = UPDATING_WALLET
            val updatedWalletState = walletState.copy(orders = walletState.orders + completedOrder)

            progressTracker.currentStep = INITIALISING_TX
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val exchangeCommand = WalletContract.Commands.Exchange(fromCurrency, toCurrency, exchangeRateFromOracle)
            val txBuilder = TransactionBuilder(notary)

            txBuilder.addInputState(walletStateAndRef)
            txBuilder.addOutputState(updatedWalletState)
            txBuilder.addCommand(exchangeCommand, walletState.participants.map { it.owningKey } + oracle.owningKey)

            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val signedTx1 = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = ORACLE_SIGNING
            val oracleSignature = subFlow(ExchangeRateFlow.SignFlow(
                oracle,
                signedTx1.filterForOracle(oracle.owningKey)))
            val signedTx = signedTx1.withAdditionalSignature(oracleSignature)

            progressTracker.currentStep = GATHERING_SIGS
            val otherParty = (walletState.participants.map { it } - oracle - ourIdentity).single().toParty(serviceHub)
            val otherPartySessions = listOf(otherParty).map { initiateFlow(Party(it.nameOrNull(), it.owningKey)) }

            progressTracker.currentStep = FINALISING_TRANSACTION
            val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTx,otherPartySessions))
            return subFlow(FinalityFlow(fullySignedTransaction, otherPartySessions))
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
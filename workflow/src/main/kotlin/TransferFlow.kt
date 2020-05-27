package workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import contracts.TradingAccountContract
import contracts.TransactionContract
import contracts.WalletContract
import javassist.NotFoundException
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import states.AccountAction
import states.TradingAccountState
import states.TransactionState
import states.WalletState
import java.time.ZonedDateTime
import java.util.*

object TransferFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val tokens: FungibleToken,
        private val walletId: UUID,
        private val tradingAccountId: UUID? = null,
        private val action: AccountAction
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object GET_WALLET : ProgressTracker.Step("Retrieving user wallet.")
            object ASSIGNING_ACCOUNT : ProgressTracker.Step("Moving tokens to owner's trading account.")
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION :
                ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        }

        fun tracker() = ProgressTracker(
            GET_WALLET,
            ASSIGNING_ACCOUNT,
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
            val walletStateAndRef = serviceHub.vaultService.queryBy<WalletState>().states
                .singleOrNull { it.state.data.walletId == walletId } ?: throw NotFoundException("Wallet with $walletId not found.")
            val walletState = walletStateAndRef.state.data

            progressTracker.currentStep = ASSIGNING_ACCOUNT
            // TODO trading account must keep track of account transfers as well as sell orders
            val tradingAccountState = TradingAccountState(UUID.randomUUID(), tokens.amount , walletState.owner, emptyList(), listOf(walletState.owner))

            // TODO exchange rate should be supplied by an oracle
            val exchangeRate = 0.75.toLong()
            // TODO must keep track of currencies being exchanged
            val transactionState = TransactionState(UUID.randomUUID(), exchangeRate, walletState.getBalance(), ZonedDateTime.now(), listOf(walletState.owner))
            val updatedWalletState = walletStateAndRef.state.data.copy(transactions = listOf(transactionState))

            progressTracker.currentStep = INITIALISING_TX
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            // TODO counterParty should be identified dynamically
            val issuer = walletState.getIssuer()
            val txBuilder = TransactionBuilder(notary)

            txBuilder.addInputState(walletStateAndRef)
            txBuilder.addOutputState(updatedWalletState)
            txBuilder.addCommand(WalletContract.Commands.Update(), listOf(walletState.owner.owningKey, issuer.owningKey))

            txBuilder.addOutputState(tradingAccountState)
            txBuilder.addCommand(TradingAccountContract.Commands.Create(), listOf(walletState.owner.owningKey))

            txBuilder.addOutputState(transactionState)
            txBuilder.addCommand(TransactionContract.Commands.Create(), listOf(walletState.owner.owningKey))

            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GATHERING_SIGS
            val otherPartySession = initiateFlow(issuer)
            val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession)))

            progressTracker.currentStep = FINALISING_TRANSACTION
            return subFlow(FinalityFlow(fullySignedTransaction, listOf(otherPartySession)))
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
package workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import contracts.TransactionContract
import contracts.WalletContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.joda.money.Money
import states.TransactionState
import states.TransactionStatus
import states.WalletState
import states.WalletStatus
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.abs

// TODO move out to an interface
/**
 *  Flow to issue virtual representation of real currency
 *  @param money The amount/type of money being moved
 *  @param receiver The receiving party
 *  @param issuer The issuing party. Must be a well established organisation such the England Bank
 */

object IssueFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val money: Money,
        private val receiver: Party,
        private val issuer: Party
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object ISSUE_TOKENS : ProgressTracker.Step("Creating tokens.")
            object ASSIGNING_WALLET : ProgressTracker.Step("Moving tokens to owner's wallet.")
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        }

        fun tracker() = ProgressTracker(
            ISSUE_TOKENS,
            ASSIGNING_WALLET,
            INITIALISING_TX,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            progressTracker.currentStep = ISSUE_TOKENS
            val currencyUnit = money.currencyUnit
            val total = money.amount.toLong()
            val fiatCurrency = FiatCurrency.getInstance(currencyUnit.code)
            val issuedTokenType = fiatCurrency issuedBy issuer
            val fiatToken: FungibleToken = total of issuedTokenType heldBy receiver
            IssueTokensFlow(fiatToken)

            progressTracker.currentStep = ASSIGNING_WALLET
            val transactionState = TransactionState(UUID.randomUUID(), total, 0, total, ZonedDateTime.now(), TransactionStatus.COMPLETED, listOf(receiver, issuer))
            val walletState = WalletState(UUID.randomUUID(), currencyUnit.toCurrency(), mapOf(currencyUnit.code to fiatToken), receiver, abs(total) ,listOf(transactionState), WalletStatus.OPEN, listOf(receiver, issuer))

            progressTracker.currentStep = INITIALISING_TX
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)

            txBuilder.addOutputState(transactionState)
            txBuilder.addCommand(TransactionContract.Commands.Create(), listOf(receiver.owningKey, issuer.owningKey))

            txBuilder.addOutputState(walletState)
            txBuilder.addCommand(WalletContract.Commands.Issue(), listOf(receiver.owningKey, issuer.owningKey))

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
                        val walletState = stx.tx.outputsOfType<WalletState>().single()
                        val responderParty = serviceHub.myInfo.legalIdentities.single()
                        "Token issuer must be the signer" using (walletState.tokens.map { it.value.issuer }.contains(responderParty))
                    }
                }
            })
            return subFlow(ReceiveFinalityFlow(counterPartySession, stx.id))
        }
    }

}

package workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import contracts.WalletContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.joda.money.Money
import states.WalletState
import states.WalletStatus
import java.util.*

/**
 *  Flow to issue virtual representation of real currency
 */

object IssueFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val money: Money, private val receiver: Party, private val issuer: Party) :
        FlowLogic<SignedTransaction>() {

        companion object {
            object ISSUE_TOKENS : ProgressTracker.Step("Creating tokens.")
            object ASSIGNING_WALLET : ProgressTracker.Step("Moving tokens to owner's wallet.")
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION :
                ProgressTracker.Step("Obtaining notary signature and recording transaction.")
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
            val currencyCode = FiatCurrency.getInstance(money.currencyUnit.code)
            val issuedTokenType = currencyCode issuedBy issuer
            val fiatToken: FungibleToken = money.amount.toLong() of issuedTokenType heldBy receiver
            IssueTokensFlow(fiatToken)

            progressTracker.currentStep = ASSIGNING_WALLET
            val walletState = WalletState(
                UUID.randomUUID(),
                fiatToken,
                receiver,
                listOf(),
                WalletStatus.OPEN,
                listOf(receiver, issuer)
            )

            progressTracker.currentStep = INITIALISING_TX
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)

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
                        val issueTokens = stx.tx.outputsOfType<WalletState>()
                        val responderParty = serviceHub.myInfo.legalIdentities.single()
                        "Token issuer must be the signer" using (issueTokens.all { it.fiatToken.issuer == responderParty })
                    }
                }
            })
            return subFlow(ReceiveFinalityFlow(counterPartySession, stx.id))
        }
    }

}

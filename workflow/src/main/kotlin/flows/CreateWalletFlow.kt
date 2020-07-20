package workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import contracts.WalletContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import states.WalletState
import states.WalletStatus
import java.util.*

// TODO move out to an interface
/**
 *  Flow to create a wallet with supplied tokens
 *  @param baseCurrency The main currency for representing the value of the wallet
 *  @param token The tokens that the wallet will hold
 *  @param owner The owner of the the wallet
 *  @param participants The parties that will be signers of the transaction. Must include the issuer
 */

object CreateWalletFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val baseCurrency: Currency?,
        private val token: FungibleToken?,
        private val owner: Party,
        private val participants: List<AbstractParty>
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object CREATE_WALLET : ProgressTracker.Step("Creating wallet.")
            object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        }

        fun tracker() = ProgressTracker(
            CREATE_WALLET,
            INITIALISING_TX,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            progressTracker.currentStep = CREATE_WALLET
            val walletTokens = if (token != null) listOf(token) else listOf()
            val total = token?.amount?.quantity ?: 0.toLong()

            val walletState = WalletState(UUID.randomUUID(), baseCurrency, walletTokens, owner, total, listOf(), listOf(), WalletStatus.OPEN, participants)

            progressTracker.currentStep = INITIALISING_TX
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)

            txBuilder.addOutputState(walletState)
            txBuilder.addCommand(WalletContract.Commands.Issue(), participants.map { it.owningKey })

            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GATHERING_SIGS
            val otherPartySessions = (participants - owner).map { initiateFlow(Party(it.nameOrNull()!!, it.owningKey)) }
            val fullySignedTransaction =subFlow(CollectSignaturesFlow(signedTx, otherPartySessions))

            progressTracker.currentStep = FINALISING_TRANSACTION

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
//                        val walletState = stx.tx.outputsOfType<WalletState>().single()
//                        val responderParty = serviceHub.myInfo.legalIdentities.single()
//                        "Token issuer must be the signer" using (walletState.tokens.map { it.value.issuer }.contains(responderParty))
                    }
                }
            })
            return subFlow(ReceiveFinalityFlow(counterPartySession, stx.id))
        }
    }

}

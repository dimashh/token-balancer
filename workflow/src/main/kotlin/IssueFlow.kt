package workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import contracts.WalletContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.joda.money.Money
import states.WalletState

// *********
// * Flow to issue virtual representation of real currency
// *********
@InitiatingFlow
@StartableByRPC
class Initiator(private val money: Money, private val receiver: Party, private val issuer: Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    companion object Tracker {
        object ISSUE_TOKENS : ProgressTracker.Step("Creating tokens.")
        object ASSIGNING_WALLET : ProgressTracker.Step("Moving tokens to owner's wallet.")
        object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")
    }

    @Suspendable
    override fun call() {

        progressTracker.currentStep = ISSUE_TOKENS
        val currencyCode = FiatCurrency.getInstance(money.currencyUnit.code)
        val issuedTokenType = currencyCode issuedBy issuer
        val fiatToken: FungibleToken = money.amount.toLong() of issuedTokenType heldBy issuer

        progressTracker.currentStep = ASSIGNING_WALLET
        val walletState = WalletState(fiatToken, receiver, listOf(receiver, issuer))

        progressTracker.currentStep = INITIALISING_TX
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)

        txBuilder.addOutputState(fiatToken)
        txBuilder.addCommand(IssueTokenCommand(issuedTokenType), listOf(issuer.owningKey))

        txBuilder.addOutputState(walletState)
        txBuilder.addCommand(WalletContract.Commands.Deposit(), listOf(receiver.owningKey, issuer.owningKey))
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}

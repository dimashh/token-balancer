package workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import org.joda.money.Money

// *********
// * Flow to issue virtual representation of real currency
// *********
@InitiatingFlow
@StartableByRPC
class Initiator(private val money: Money, private val receiver: Party, private val issuer: Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    companion object Tracker {
        object INITIALISING_TOKEN : ProgressTracker.Step("Initialising TokenStates.")
        object ASSIGNING_OWNER : ProgressTracker.Step("Assigning token owner.")
        object INITIALISING_TX : ProgressTracker.Step("Initialising transaction.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying transaction.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with own key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering participant signatures.")

    }

    @Suspendable
    override fun call() {
        val currencyCode = money.currencyUnit.code
        val currencyAmount = money.amount.toLong()

        progressTracker.currentStep = INITIALISING_TOKEN

        val tokenType = IssuedTokenType(issuer, TokenType(currencyCode, 2))
        val createdToken = FungibleToken(Amount(currencyAmount, tokenType), receiver)

        val issuedTokens = IssueTokens(listOf(createdToken))
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}

package flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import oracle.ExchangeRateOracle

object ExchangeRateFlow {

    @InitiatingFlow
    class RequestFlow(
        private val oracle: Party,
        private val fromCurrency: String,
        private val toCurrency: String
    ) : FlowLogic<Double>() {
        @Suspendable override fun call() = initiateFlow(oracle)
            .sendAndReceive<Double>(Pair(fromCurrency, toCurrency)).unwrap { it }
    }

    @InitiatedBy(RequestFlow::class)
    open class RequestHandlerFlow(private val session: FlowSession) : FlowLogic<Unit>() {

        companion object {
            object RECEIVING : ProgressTracker.Step("Receiving query request.")
            object CALCULATING : ProgressTracker.Step("Calculating exchange rate.")
            object SENDING : ProgressTracker.Step("Sending query response.")
        }

        override val progressTracker = ProgressTracker(RECEIVING, CALCULATING, SENDING)

        open fun exchangeRateOracle() = serviceHub.cordaService(ExchangeRateOracle::class.java)

        @Suspendable
        override fun call() {
            progressTracker.currentStep = RECEIVING
            val request = session.receive<Pair<String, String>>().unwrap { it }

            progressTracker.currentStep = CALCULATING
            val response = try {
                exchangeRateOracle().query(request.first, request.second)
            } catch (e: Exception) {
                // Re-throw the exception as a FlowException so its propagated to the querying node.
                throw FlowException(e)
            }

            progressTracker.currentStep = SENDING
            session.send(response)
        }
    }

    @InitiatingFlow
    class SignFlow(
        private val oracle: Party,
        private val ftx: FilteredTransaction
    ) : FlowLogic<TransactionSignature>() {
        @Suspendable override fun call() = initiateFlow(oracle)
            .sendAndReceive<TransactionSignature>(ftx).unwrap { it }
    }

    @InitiatedBy(SignFlow::class)
    open class SignHandlerFlow(val session: FlowSession) : FlowLogic<Unit>() {
        companion object {
            object RECEIVING : ProgressTracker.Step("Receiving sign request.")
            object SIGNING : ProgressTracker.Step("Signing filtered transaction.")
            object SENDING : ProgressTracker.Step("Sending sign response.")
        }

        override val progressTracker = ProgressTracker(RECEIVING, SIGNING, SENDING)

        open fun exchangeRateOracle() = serviceHub.cordaService(ExchangeRateOracle::class.java)

        @Suspendable
        override fun call() {
            progressTracker.currentStep = RECEIVING
            val request = session.receive<FilteredTransaction>().unwrap { it }

            progressTracker.currentStep = SIGNING
            val response = try {
                exchangeRateOracle().sign(request)
            } catch (e: Exception) {
                throw FlowException(e)
            }

            progressTracker.currentStep = SENDING
            session.send(response)
        }
    }
}




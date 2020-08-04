package oracle

import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import states.ExchangeRateCommand
import java.security.PublicKey
import java.util.function.Predicate

@CordaService
class ExchangeRateOracle(private val service: ServiceHub): SingletonSerializeAsToken() {
    private val myKey = service.myInfo.legalIdentities.first().owningKey

    companion object {
        private fun filterPredicate(oracle: PublicKey): Predicate<Any> = Predicate {
            it is Command<*> && it.value is ExchangeRateCommand && oracle in it.signers
        }

        // Asking the oracle to sign the transaction
        // For privacy reasons, we only want to expose to the oracle any commands of type `ExchangeRateCommand`
        // that require its signature.
        fun SignedTransaction.filterForOracle(oracle: PublicKey): FilteredTransaction =
            this.buildFilteredTransaction(filterPredicate(oracle))
    }

    private fun exchangeRateService() = service.cordaService(ExchangeRateService::class.java)

    fun query(fromCurrencyCode: String, toCurrencyCode: String): Double {
        return exchangeRateService().getExchangeRate(fromCurrencyCode, toCurrencyCode)
    }

    private fun rateIsMatching(command: ExchangeRateCommand) : Boolean {
        return (command.rate == query(command.fromCurrency, command.toCurrency))
    }

    private fun FilteredTransaction.agreesWithExchangeRateOracle(): Boolean = this.checkWithFun {
        it is Command<*>
                && myKey in it.signers
                && it.value is ExchangeRateCommand
                && rateIsMatching(it.value as ExchangeRateCommand)
    }

    fun sign(ftx: FilteredTransaction): TransactionSignature {
        // Check the partial Merkle tree is valid.
        ftx.verify()

        if (ftx.agreesWithExchangeRateOracle()) {
            return service.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}
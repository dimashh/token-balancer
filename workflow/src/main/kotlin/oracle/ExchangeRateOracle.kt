package oracle

import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction

@CordaService
class ExchangeRateOracle(val service: ServiceHub): SingletonSerializeAsToken() {
    private val myKey = service.myInfo.legalIdentities.first().owningKey

    open fun exchangeRateService() = service.cordaService(ExchangeRateService::class.java)

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
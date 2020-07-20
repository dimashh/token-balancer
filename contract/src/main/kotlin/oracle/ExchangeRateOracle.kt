package oracle

import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction

@CordaService
class ExchangeRateOracle(val services: ServiceHub): SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey

    private fun FilteredTransaction.agreesWithExchangeRateOracle(): Boolean = this.checkWithFun {
        it is Command<*>
                && myKey in it.signers
                && it.value is ExchangeRateCommand
    }

    fun sign(ftx: FilteredTransaction): TransactionSignature {
        // Check the partial Merkle tree is valid.
        ftx.verify()

        if (ftx.agreesWithExchangeRateOracle()) {
            return services.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}
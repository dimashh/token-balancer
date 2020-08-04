package oracle

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
open class ExchangeRateService(val service: ServiceHub): SingletonSerializeAsToken() {

    companion object {
        private const val API_URL = "https://api.exchangeratesapi.io/latest"
    }

    open fun getExchangeRate(fromCurrency: String, toCurrency: String): Double {
        val url = "$API_URL?symbols=$fromCurrency,$toCurrency"

        val response = khttp.get(url)

        try {
            return  response.jsonObject.optJSONObject("rates").getDouble(toCurrency)
        } catch (e: Exception) {
            throw IllegalArgumentException()
        }

    }
}
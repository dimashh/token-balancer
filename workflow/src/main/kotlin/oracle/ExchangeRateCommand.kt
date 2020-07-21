package oracle

import java.util.*

open class ExchangeRateCommand(
    val walletId: UUID,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double
)
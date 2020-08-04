package states

open class ExchangeRateCommand(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double
)
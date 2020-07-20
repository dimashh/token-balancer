package oracle

import java.util.*

open class ExchangeRateCommand(
    val walletId: UUID,
    val rate: Long
)
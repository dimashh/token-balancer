package states

import contracts.TransactionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import java.time.ZonedDateTime

@BelongsToContract(TransactionContract::class)
data class TransactionState(
   val transactionId: String,
   val exchangeRate: Long,
   val total: Long,
   val date: ZonedDateTime,
   override val participants: List<AbstractParty>,
   override val linearId: UniqueIdentifier
): LinearState {

}
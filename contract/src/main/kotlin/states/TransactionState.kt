package states

import contracts.TransactionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.time.ZonedDateTime
import java.util.*

@CordaSerializable
enum class TransactionStatus { COMPLETED }

@BelongsToContract(TransactionContract::class)
data class TransactionState(
    val transactionId: UUID,
    val amountIn: Long,
    val amountOut: Long,
    val total: Long,
    val date: ZonedDateTime,
    val status: TransactionStatus,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {

}
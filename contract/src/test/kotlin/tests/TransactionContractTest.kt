package tests

import ContractTest
import contracts.TransactionContract
import net.corda.testing.node.ledger
import contracts.WalletContract.Commands.Issue
import org.junit.jupiter.api.Test
import states.TransactionState
import states.TransactionStatus
import java.time.ZonedDateTime
import java.util.*

class TransactionContractTest : ContractTest() {

    private val transactionState = TransactionState(UUID.randomUUID(), 10, 0, 10, ZonedDateTime.now(), TransactionStatus.COMPLETED, listOf(IDENTITY_A.party, IDENTITY_B.party))

    @Test
    fun `Requires a command`() {
        services.ledger {
            transaction {

                output(TransactionContract::class.java.name, transactionState)
                command(IDENTITY_A.publicKey, Issue())
                failsWith("Required contracts.TransactionContract.Commands command")
            }
        }
    }
}
package tests

import ContractTest
import contracts.TransactionContract
import net.corda.testing.node.ledger
import contracts.TransactionContract.Commands.Create
import contracts.WalletContract.Commands.Issue
import org.junit.jupiter.api.Test
import states.TransactionState
import states.TransactionStatus
import java.time.ZonedDateTime
import java.util.*

class TransactionContractTest : ContractTest() {

    private val transactionState = TransactionState(UUID.randomUUID(), 10, 0, 10, ZonedDateTime.now(), TransactionStatus.COMPLETED, listOf(IDENTITY_A.party, IDENTITY_B.party))

    @Test
    fun `Create - Requires a command`() {
        services.ledger {
            transaction {

                output(TransactionContract::class.java.name, transactionState)
                command(IDENTITY_A.publicKey, Issue())
                failsWith("Required contracts.TransactionContract.Commands command")
            }
        }
    }

    @Test
    fun `Create - Requires no input state`() {
        services.ledger {
            transaction {
                input(TransactionContract::class.java.name, transactionState)
                command(keysOf(IDENTITY_A), Create())
                failsWith("There should be no input transaction state")
            }
        }
    }

    @Test
    fun `Create - Requires one output state`() {
        services.ledger {
            transaction {
                output(TransactionContract::class.java.name, transactionState)
                command(keysOf(IDENTITY_A), Create())
                verifies()
            }
        }
    }

    @Test
    fun `Create - Total must add up`() {
        services.ledger {
            transaction {
                val withInvalidTotal = transactionState.copy(amountIn = 5)
                output(TransactionContract::class.java.name, withInvalidTotal)
                command(keysOf(IDENTITY_A), Create())
                failsWith("Transaction total must be 10, but was 5")
            }
        }
    }
}
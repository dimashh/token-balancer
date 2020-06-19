package tests

import ContractTest
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import contracts.WalletContract
import contracts.WalletContract.Commands.Issue
import contracts.WalletContract.Commands.Update
import states.WalletState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test
import states.TransactionState
import states.TransactionStatus
import states.WalletStatus
import java.time.ZonedDateTime
import java.util.*

class WalletContractTest : ContractTest() {

    private val issuedTokenType = GBP issuedBy IDENTITY_A.party
    private val fiatToken: FungibleToken = 10 of issuedTokenType heldBy IDENTITY_B.party
    private val walletState = WalletState(UUID.randomUUID(), fiatToken, IDENTITY_B.party, 10, listOf(),
        WalletStatus.OPEN, listOf(IDENTITY_A.party, IDENTITY_B.party))
    private val transactionState = TransactionState(UUID.randomUUID(), 10, 0, 10,
        ZonedDateTime.now(), TransactionStatus.COMPLETED, listOf(IDENTITY_A.party, IDENTITY_B.party))


    @Test
    fun `Issue - Requires a command`() {
        services.ledger {
            transaction {

                output(WalletContract::class.java.name, walletState)
                command(IDENTITY_A.publicKey, IssueTokenCommand(issuedTokenType))
                failsWith("Required contracts.WalletContract.Commands command")
            }
        }
    }

    @Test
    fun `Issue - Requires no input state`() {
        services.ledger {
            transaction {
                input(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                failsWith("There is exactly one output wallet state")
            }
        }
    }

    @Test
    fun `Issue - Requires one output state`() {
        services.ledger {
            transaction {
                output(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                verifies()
            }
        }
    }

    @Test
    fun `Issue - Requires same wallet and token owner`() {
        services.ledger {
            transaction {
                val withDifferentOwner = walletState.copy(owner = IDENTITY_A.party)
                output(WalletContract::class.java.name, withDifferentOwner)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                failsWith("Owner of the wallet [O=PartyA, L=London, C=GB] " +
                        "must be the owner of the tokens [O=PartyB, L=New York, C=US]")
            }
        }
    }

    @Test
    fun `Issue - Requires positive balance`() {
        services.ledger {
            transaction {
                val withNegativeBalance = walletState.copy(balance = -10)
                output(WalletContract::class.java.name, withNegativeBalance)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                failsWith("Wallet balance cannot be negative")
            }
        }
    }

    @Test
    fun `Update - Requires one input state & one output state`() {
        services.ledger {
            transaction {
                output(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Update())
                failsWith("There is exactly one input wallet state")
            }

            transaction {
                input(WalletContract::class.java.name, walletState)
                output(WalletContract::class.java.name, walletState)
                output(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Update())
                failsWith("There is exactly one output wallet state")
            }
        }
    }

    @Test
    fun `Update - Requires transactions`() {
        services.ledger {
            transaction {
                input(WalletContract::class.java.name, walletState)
                output(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Update())
                failsWith("Updated wallet state must include new transaction(s)")
            }
        }
    }

    @Test
    fun `Update - Requires one transaction per update`() {
        services.ledger {
            transaction {
                val withTransaction = walletState.copy(transactions = listOf(transactionState))
                val newTransaction = transactionState.copy(transactionId = UUID.randomUUID())
                val outputState = withTransaction.copy(walletId = UUID.randomUUID(),
                    transactions = withTransaction.transactions + newTransaction)

                input(WalletContract::class.java.name, withTransaction)
                output(WalletContract::class.java.name, outputState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Update())
                verifies()
            }
        }
    }
}
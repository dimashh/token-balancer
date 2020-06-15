package tests

import ContractTest
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import contracts.WalletContract
import contracts.TransactionContract
import contracts.TransactionContract.Commands.Create
import contracts.WalletContract.Commands.Issue
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
    private val walletState = WalletState(UUID.randomUUID(), fiatToken, IDENTITY_B.party, 10, listOf(), WalletStatus.OPEN, listOf(IDENTITY_A.party, IDENTITY_B.party))
    private val transactionState = TransactionState(UUID.randomUUID(), 10, 0, 10, ZonedDateTime.now(), TransactionStatus.COMPLETED, listOf(IDENTITY_A.party, IDENTITY_B.party))


    @Test
    fun `Requires a command`() {
        services.ledger {
            transaction {

                output(WalletContract::class.java.name, walletState)
                command(IDENTITY_A.publicKey, IssueTokenCommand(issuedTokenType))
                failsWith("Required contracts.WalletContract.Commands command")
            }
        }
    }

    @Test
    fun `Requires output states`() {
        services.ledger {
            transaction {

                command(keysOf(IDENTITY_A, IDENTITY_B), Create())
                output(TransactionContract::class.java.name, transactionState)

                output(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())

                verifies()
            }
        }
    }

}
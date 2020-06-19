package tests

import ContractTest
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import contracts.WalletContract
import contracts.WalletContract.Commands.Issue
import states.WalletState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test
import states.WalletStatus
import java.util.*

class WalletContractTest : ContractTest() {

    private val issuedTokenType = GBP issuedBy IDENTITY_A.party
    private val fiatToken: FungibleToken = 10 of issuedTokenType heldBy IDENTITY_B.party
    private val walletState = WalletState(UUID.randomUUID(), fiatToken, IDENTITY_B.party, 10, listOf(),
        WalletStatus.OPEN, listOf(IDENTITY_A.party, IDENTITY_B.party))

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
    fun `Requires no input state`() {
        services.ledger {
            transaction {
                input(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                failsWith("There is exactly one output wallet state")
            }
        }
    }

    @Test
    fun `Requires one output state`() {
        services.ledger {
            transaction {
                output(WalletContract::class.java.name, walletState)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                verifies()
            }
        }
    }

    @Test
    fun `Requires same wallet and token owner`() {
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
    fun `Requires positive balance`() {
        services.ledger {
            transaction {
                val withNegativeBalance = walletState.copy(balance = -10)
                output(WalletContract::class.java.name, withNegativeBalance)
                command(keysOf(IDENTITY_A, IDENTITY_B), Issue())
                failsWith("Wallet balance cannot be negative")
            }
        }
    }

}
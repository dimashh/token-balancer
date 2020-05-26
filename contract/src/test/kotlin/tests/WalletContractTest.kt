package tests

import ContractTest
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import contracts.WalletContract
import states.WalletState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test
import java.util.*

class WalletContractTest : ContractTest() {

    @Test
    fun `Wallet Contract - fails`() {
        services.ledger {
            transaction {

                val issuedTokenType = GBP issuedBy IDENTITY_A.party
                val fiatToken: FungibleToken = 10 of issuedTokenType heldBy IDENTITY_B.party

                command(IDENTITY_A.publicKey, IssueTokenCommand(issuedTokenType))
                output(WalletContract::class.java.name, WalletState(UUID.randomUUID(), fiatToken, IDENTITY_B.party, listOf() ,listOf(IDENTITY_A.party, IDENTITY_B.party)))
                fails()
            }
        }
    }

}
package tests

import ContractTest
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.GBP
import contracts.TradingAccountContract
import net.corda.testing.node.ledger
import contracts.WalletContract.Commands.Issue
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.Test
import states.AccountStatus
import states.TradingAccountState
import java.util.*

class TradingAccountContractTest : ContractTest() {

    private val issuedTokenType = GBP issuedBy IDENTITY_A.party
    private val tradingAccountState = TradingAccountState(
        UUID.randomUUID(),
        Amount(10.toLong(), issuedTokenType),
        IDENTITY_A.party,
        listOf(),
        listOf(),
        AccountStatus.ACTIVE,
        listOf(IDENTITY_A.party))

    @Test
    fun `Requires a command`() {
        services.ledger {
            transaction {

                output(TradingAccountContract::class.java.name, tradingAccountState)
                command(IDENTITY_A.publicKey, Issue())
                failsWith("Required contracts.TradingAccountContract.Commands command")
            }
        }
    }
}


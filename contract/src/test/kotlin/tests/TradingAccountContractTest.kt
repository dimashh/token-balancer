package tests

import ContractTest
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.GBP
import contracts.TradingAccountContract
import net.corda.testing.node.ledger
import contracts.TradingAccountContract.Commands.Create
import contracts.TradingAccountContract.Commands.Update
import contracts.WalletContract.Commands.Issue
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import states.*
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
    private val order = Order(UUID.randomUUID(), OrderAction.EXCHANGE, AssetType.CURRENCY, null, OrderStatus.WORKING, mapOf())

    @Test
    fun `Create - Requires a command`() {
        services.ledger {
            transaction {
                output(TradingAccountContract::class.java.name, tradingAccountState)
                command(IDENTITY_A.publicKey, Issue())
                failsWith("Required contracts.TradingAccountContract.Commands command")
            }
        }
    }

    @Test
    fun `Create - Requires no input state`() {
        services.ledger {
            transaction {
                input(TradingAccountContract::class.java.name, tradingAccountState)
                command(IDENTITY_A.publicKey, Create())
                failsWith("There should be no input trading account state")
            }
        }
    }

    @Test
    fun `Create - Requires one output state`() {
        services.ledger {
            transaction {
                output(TradingAccountContract::class.java.name, tradingAccountState)
                command(IDENTITY_A.publicKey, Create())
                verifies()
            }
        }
    }

    @Test
    fun `Create - Requires a positive balance`() {
        assertThrows<IllegalArgumentException>("Negative amounts are not allowed: -10") {
            services.ledger {
                transaction {
                    val withNegativeBalance = tradingAccountState.copy(balance = (tradingAccountState.balance * -1))
                    output(TradingAccountContract::class.java.name, withNegativeBalance)
                    command(IDENTITY_A.publicKey, Create())
                    verifies()
                }
            }
        }
    }

    @Test
    fun `Update - Requires one input state & one output state`() {
        services.ledger {
            transaction {
                output(TradingAccountContract::class.java.name, tradingAccountState)
                command(IDENTITY_A.publicKey, Update())
                failsWith("There is exactly one input trading account state")
            }

            transaction {
                input(TradingAccountContract::class.java.name, tradingAccountState)
                output(TradingAccountContract::class.java.name, tradingAccountState)
                output(TradingAccountContract::class.java.name, tradingAccountState)
                command(IDENTITY_A.publicKey, Update())
                failsWith("There is exactly one output trading account state")
            }
        }
    }

    @Test
    fun `Update - order must be valid`() {
        services.ledger {
            transaction {
                input(TradingAccountContract::class.java.name, tradingAccountState)
                output(TradingAccountContract::class.java.name, tradingAccountState.copy(orders = listOf(order)))
                command(IDENTITY_A.publicKey, Update())
                failsWith("Order must have a status of completed but was WORKING")
            }

            transaction {
                val orders = listOf(order.copy(status = OrderStatus.COMPLETED), order)
                input(TradingAccountContract::class.java.name, tradingAccountState)
                output(TradingAccountContract::class.java.name, tradingAccountState.copy(orders = orders))
                command(IDENTITY_A.publicKey, Update())
                failsWith("Trading Account update can only add one order at a time")
            }
        }
    }
}


package contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import states.TradingAccountState

class TradingAccountContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction) {
        requireThat {
            val tradingAccountState = tx.outputsOfType<TradingAccountState>().single()

            "There is exactly one output wallet state" using (tx.outputsOfType<TradingAccountState>().size == 1)
            "Account balance cannot be negative" using (tradingAccountState.balance.quantity > 0)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        //class Withdraw : Commands
    }
}
package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import states.TradingAccountState

class TransactionContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction) {
        requireThat {
            val transactionState = tx.outputsOfType<TradingAccountState>().single()

            "There is exactly one output wallet state" using (tx.outputsOfType<TradingAccountState>().size == 1)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}
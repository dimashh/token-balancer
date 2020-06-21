package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import states.TransactionState
import kotlin.math.abs

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
            "There should be no input transaction state" using (tx.inputsOfType<TransactionState>().isEmpty())
            "There is exactly one output transaction state" using (tx.outputsOfType<TransactionState>().size == 1)

            val transactionStates = tx.outputsOfType<TransactionState>()

            transactionStates.map {
                val total = abs(it.amountIn + it.amountOut)
                "Transaction total must be ${it.total}, but was $total" using (total == it.total)
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}
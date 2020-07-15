package contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import states.OrderStatus
import states.TradingAccountState

class TradingAccountContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx)
            is Commands.Update -> verifyUpdate(tx)
            is Commands.Withdraw -> verifyWithdraw(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction) {
        requireThat {
            "There should be no input trading account state" using (tx.inputsOfType<TradingAccountState>().isEmpty())
            val output = tx.outputsOfType<TradingAccountState>()
            "There is exactly one output trading account state" using (output.size == 1)
            "Trading account balance cannot be negative" using (output.single().balance >= 0)
        }
    }

    private fun verifyUpdate(tx: LedgerTransaction) {
        requireThat {
            "There is exactly one input trading account state" using (tx.inputsOfType<TradingAccountState>().size == 1)
            "There is exactly one output trading account state" using (tx.outputsOfType<TradingAccountState>().size == 1)

            val inputTradingAccount = tx.inputsOfType<TradingAccountState>().single()
            val outputTradingAccount = tx.outputsOfType<TradingAccountState>().single()
            val latestOrder = outputTradingAccount.orders - inputTradingAccount.orders
            "Trading Account update can only add one order at a time" using (latestOrder.size == 1)
            "Order must have a status of completed but was ${latestOrder.single().status}" using
                    (latestOrder.single().status == OrderStatus.COMPLETED)
        }
    }

    private fun verifyWithdraw(tx: LedgerTransaction) {
        requireThat {

        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
        class Withdraw : Commands
    }
}
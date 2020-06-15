package contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import states.OrderAction
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
            val tradingAccountState = tx.outputsOfType<TradingAccountState>().single()

            "There is exactly one output wallet state" using (tx.outputsOfType<TradingAccountState>().size == 1)
            "Account balance cannot be negative" using (tradingAccountState.balance.quantity > 0)
        }
    }

    private fun verifyUpdate(tx: LedgerTransaction) {
        requireThat {
            val inputTradingAccount = tx.inputsOfType<TradingAccountState>().single()
            val outputTradingAccount = tx.outputsOfType<TradingAccountState>().single()
            val latestOrder = outputTradingAccount.orders - inputTradingAccount.orders
            val order = latestOrder.single()

            "Order must have a status of completed but was ${order.status}" using (order.status == OrderStatus.COMPLETED)
            "Buy order must have an action of buy but was ${order.action}" using (order.action == OrderAction.BUY)

            "There is exactly one input wallet state" using (tx.inputsOfType<TradingAccountState>().size == 1)
            "There is exactly one output wallet state" using (tx.outputsOfType<TradingAccountState>().size == 1)

            "Trading Account update can only add one order at a time" using (latestOrder.size == 1)
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
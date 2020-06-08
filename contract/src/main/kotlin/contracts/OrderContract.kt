package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import states.OrderAction
import states.OrderStatus
import states.TradingAccountState

class OrderContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Buy -> verifyBuy(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyBuy(tx: LedgerTransaction) = requireThat {
        val inputAccount = tx.inputsOfType<TradingAccountState>().single()
        val outputAccount = tx.outputsOfType<TradingAccountState>().single()
        val newOrder = outputAccount.orders.last()

        "Order must have a status of completed but was ${newOrder.status}" using (newOrder.status == OrderStatus.COMPLETED)
        "Buy order must have an action of buy but was ${newOrder.action}" using (newOrder.action == OrderAction.BUY)
        "New order must not already exist" using (!inputAccount.orders.contains(newOrder))
        "There must one input trading account state" using (tx.inputsOfType<TradingAccountState>().size == 1)
        "There must one output trading account state" using (tx.outputsOfType<TradingAccountState>().size == 1)
    }

    interface Commands : CommandData {
        class Buy : Commands
        class Sell : Commands
    }
}
package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class OrderContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Buy -> verifyBuy(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyBuy(tx: LedgerTransaction) = requireThat {
        // TODO
    }

    interface Commands : CommandData {
        class Buy : Commands
        class Sell : Commands
    }
}
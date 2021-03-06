package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import states.ExchangeRateCommand
import states.OrderStatus
import states.WalletState

class WalletContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Issue -> verifyIssue(tx)
            is Commands.Update -> verifyUpdate(tx)
            is Commands.Withdraw -> verifyWithdraw(tx)
            is Commands.Exchange -> verifyExchange(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyIssue(tx: LedgerTransaction) = requireThat {
        "There should be no input wallet state" using (tx.inputsOfType<WalletState>().isEmpty())
        "There is exactly one output wallet state" using (tx.outputsOfType<WalletState>().size == 1)

        val walletState = tx.outputsOfType<WalletState>().single()
        "Owner of the wallet [${walletState.owner}] must be the owner of the tokens" using
                (walletState.tokens.all { it.holder == walletState.owner })
        "Wallet balance cannot be negative" using (walletState.balance >= 0)
    }

    private fun verifyUpdate(tx: LedgerTransaction) = requireThat {
        "There is exactly one input wallet state" using (tx.inputsOfType<WalletState>().size == 1)
        "There is exactly one output wallet state" using (tx.outputsOfType<WalletState>().size == 1)

        val input = tx.inputsOfType<WalletState>().single()
        val output = tx.outputsOfType<WalletState>().single()
        val latestTransaction = output.transactions - input.transactions

        "Updated wallet state must include new transaction(s)" using latestTransaction.isNotEmpty()
    }

    private fun verifyWithdraw(tx: LedgerTransaction) = requireThat {
        val inputs = tx.inputsOfType<WalletState>()
        val outputs = tx.outputsOfType<WalletState>()
    }

    private fun verifyExchange(tx: LedgerTransaction) = requireThat {
        "There is exactly one input wallet state" using (tx.inputsOfType<WalletState>().size == 1)
        "There is exactly one output wallet state" using (tx.outputsOfType<WalletState>().size == 1)

        val inputWalletState = tx.inputsOfType<WalletState>().single()
        val outputWalletState = tx.outputsOfType<WalletState>().single()

        inputWalletState.tokens.map { inputToken ->
            "Output wallet state must contain the same token" using (inputToken in outputWalletState.tokens)
        }

        val latestOrder = outputWalletState.orders.last()
        "Output wallet state must have its latest order completed" using (latestOrder.status == OrderStatus.COMPLETED)
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Update : Commands
        class Exchange(fromCurrency: String, toCurrency: String, rate: Double) : ExchangeRateCommand(fromCurrency, toCurrency, rate), Commands
        class Withdraw : Commands
    }
}
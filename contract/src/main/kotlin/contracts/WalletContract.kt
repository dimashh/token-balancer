package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import states.WalletState

class WalletContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Issue -> verifyIssue(tx)
            is Commands.Update -> verifyUpdate(tx)
            is Commands.Withdraw -> verifyWithdraw(tx)
            else -> throw IllegalArgumentException("Command not found.")
        }
    }

    private fun verifyIssue(tx: LedgerTransaction) = requireThat {
        val walletState = tx.outputsOfType<WalletState>().single()

        "Owner of the wallet ${walletState.owner} must be the owner of the tokens ${walletState.fiatToken.holder}" using (walletState.owner == walletState.fiatToken.holder)

        "Wallet balance cannot be negative" using (walletState.getBalance() > 0)
    }

    private fun verifyUpdate(tx: LedgerTransaction) = requireThat {
        val inputs = tx.inputsOfType<WalletState>()
        val outputs = tx.outputsOfType<WalletState>()
    }

    private fun verifyWithdraw(tx: LedgerTransaction) = requireThat {
        val inputs = tx.inputsOfType<WalletState>()
        val outputs = tx.outputsOfType<WalletState>()

    }

    interface Commands : CommandData {
        class Issue : Commands
        class Update : Commands
        class Withdraw : Commands
    }
}
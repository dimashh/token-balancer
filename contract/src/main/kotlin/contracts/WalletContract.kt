package contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import states.WalletState

class WalletContract : Contract {

    companion object {
        @JvmStatic
        val ID = "WalletContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val inputs = tx.inputsOfType<WalletState>()
        val outputs = tx.outputsOfType<WalletState>()
        val commands = tx.commands

        requireThat {
            "Owner of the wallet must be the owner of the tokens" using (outputs.all { it.owner != it.fiatToken.holder })

            "Wallet balance cannot be negative" using (outputs.all { it.getBalance() < 0 })
        }
    }

    interface Commands : CommandData {
        class Deposit : Commands
        class Transfer : Commands
        class Withdraw : Commands
    }
}
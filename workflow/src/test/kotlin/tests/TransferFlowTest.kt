//package tests
//
//import FlowTest
//import org.joda.money.CurrencyUnit
//import org.joda.money.Money
//import org.junit.jupiter.api.Test
//import states.AccountAction
//import states.WalletState
//import workflow.CreateWalletFlow
//import workflow.IssueFlow
//import workflow.TransferFlow
//
///**
// *  Add -ea -javaagent:../lib/quasar.jar to VM options
// **/
//
//class TransferFlowTest : FlowTest() {
//
//    private val currencyUnit = CurrencyUnit.GBP
//    private val testMoney = Money.of(currencyUnit, 10.toBigDecimal())
//
//    private val afterCreateWalletFlow by lazy {
//        runNetwork {
//            nodeB.startFlow(CreateWalletFlow.Initiator(currencyUnit.toCurrency(), null, partyB, listOf(partyA, partyB)))
//        }
//    }
//
//    private val walletStateWithTokens by lazy {
//        val wallet = afterCreateWalletFlow.get().tx.outputsOfType<WalletState>().single()
//        runNetwork {
//            nodeB.startFlow(IssueFlow.Initiator(testMoney, wallet.walletId, receiver = partyB, issuer = partyA))
//        }.get().tx.outputsOfType<WalletState>().single()
//    }
//
//    @Test
//    fun `flow to transfer tokens from waller to trading account`() {
//        runNetwork {
//            nodeB.startFlow(
//                TransferFlow.Initiator(
//                    walletStateWithTokens.tokens.last(),
//                    walletStateWithTokens.walletId,
//                    null,
//                    AccountAction.ISSUE
//                )
//            )
//        }
//    }
//
//}
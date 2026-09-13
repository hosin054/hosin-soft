package com.example.data.model

data class TrialBalanceAccount(
    val code: String,
    val name: String,
    val category: String, // أصول, خصوم, حقوق الملكية, إيرادات, مصروفات
    val debitTotal: Double,
    val creditTotal: Double,
    val debitBalance: Double,
    val creditBalance: Double
)

data class TrialBalanceData(
    val accounts: List<TrialBalanceAccount>,
    val totalDebitSum: Double,
    val totalCreditSum: Double,
    val totalDebitBalance: Double,
    val totalCreditBalance: Double,
    val isBalanced: Boolean,
    val difference: Double
)

data class IncomeStatementData(
    val grossSales: Double,
    val salesDiscountsAndReturns: Double,
    val netSales: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val grossProfitMargin: Double,
    val expensesByCategory: List<Pair<String, Double>>,
    val totalOperatingExpenses: Double,
    val otherRevenues: Double,
    val otherExpenses: Double,
    val netOperatingIncome: Double,
    val netProfit: Double,
    val netProfitMargin: Double
)

data class BalanceSheetData(
    val cashOnHand: Double,
    val bankAndCards: Double,
    val accountsReceivable: Double,
    val inventoryEndingValue: Double,
    val fixedAssets: Double,
    val otherAssets: Double,
    val totalCurrentAssets: Double,
    val totalAssets: Double,
    val accountsPayable: Double,
    val otherLiabilities: Double,
    val totalLiabilities: Double,
    val capital: Double,
    val ownerDrawings: Double,
    val netProfitCurrentPeriod: Double,
    val retainedEarnings: Double,
    val totalEquity: Double,
    val totalLiabilitiesAndEquity: Double,
    val difference: Double,
    val isBalanced: Boolean
)

data class TradingAccountData(
    val beginningInventory: Double,
    val purchasesTotal: Double,
    val purchaseExpenses: Double,
    val salesTotal: Double,
    val endingInventory: Double,
    val grossProfit: Double,
    val totalTradingDebit: Double,
    val totalTradingCredit: Double
)

data class ClosingAccountsBundle(
    val trialBalance: TrialBalanceData,
    val incomeStatement: IncomeStatementData,
    val balanceSheet: BalanceSheetData,
    val tradingAccount: TradingAccountData,
    val generatedDate: Long = System.currentTimeMillis()
)

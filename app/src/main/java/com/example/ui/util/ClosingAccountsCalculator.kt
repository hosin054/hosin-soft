package com.example.ui.util

import com.example.data.model.*

object ClosingAccountsCalculator {

    fun calculate(
        invoices: List<Invoice>,
        products: List<Product>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        expenses: List<Expense>,
        cashTransactions: List<CashTransaction>,
        journalVouchers: List<JournalVoucher>,
        journalLines: List<JournalVoucherLine>
    ): ClosingAccountsBundle {

        // 1. Sales Calculations
        val completedSales = invoices.filter { it.invoiceType == "SALE" && it.status != "CANCELLED" }
        val salesReturns = invoices.filter { it.invoiceType == "SALES_RETURN" }
        val grossSales = completedSales.sumOf { it.subtotal }
        val salesDiscounts = completedSales.sumOf { it.discountAmount }
        val returnsTotal = salesReturns.sumOf { it.totalAmount }
        val salesDiscountsAndReturns = salesDiscounts + returnsTotal
        val netSales = maxOf(0.0, grossSales - salesDiscountsAndReturns)
        val costOfGoodsSold = maxOf(0.0, completedSales.sumOf { it.totalCost } - salesReturns.sumOf { it.totalCost })
        val grossProfit = netSales - costOfGoodsSold
        val grossProfitMargin = if (netSales > 0) (grossProfit / netSales) * 100.0 else 0.0

        // 2. Expenses by category
        val expensesByCategoryMap = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toList()
            .sortedByDescending { it.second }
        val totalOperatingExpenses = expenses.sumOf { it.amount }

        // 3. Journal Lines aggregations
        val otherRevenues = journalLines
            .filter { it.accountCode == "403" || it.accountName.contains("إيراد") || it.accountName.contains("أرباح") }
            .sumOf { it.credit - it.debit }
            .coerceAtLeast(0.0)

        val otherExpenses = journalLines
            .filter { it.accountCode == "507" || it.accountName.contains("خسائر") }
            .sumOf { it.debit - it.credit }
            .coerceAtLeast(0.0)

        val jvFixedAssets = journalLines
            .filter { it.accountCode == "105" || it.accountName.contains("أصول") }
            .sumOf { it.debit - it.credit }
            .coerceAtLeast(0.0)

        val jvCapital = journalLines
            .filter { it.accountCode == "301" || it.accountName.contains("رأس المال") }
            .sumOf { it.credit - it.debit }

        val jvDrawings = journalLines
            .filter { it.accountCode == "302" || it.accountName.contains("جاري المالك") || it.accountName.contains("مسحوبات") }
            .sumOf { it.debit - it.credit }
            .coerceAtLeast(0.0)

        val jvBank = journalLines
            .filter { it.accountCode == "102" || it.accountName.contains("بنك") || it.accountName.contains("شبكة") }
            .sumOf { it.debit - it.credit }

        val jvOtherLiabilities = journalLines
            .filter { it.accountCode == "202" || it.accountName.contains("قروض") || it.accountName.contains("التزام") }
            .sumOf { it.credit - it.debit }
            .coerceAtLeast(0.0)

        // 4. Net Profit Calculation
        val netOperatingIncome = grossProfit - totalOperatingExpenses
        val netProfit = netOperatingIncome + otherRevenues - otherExpenses
        val netProfitMargin = if (netSales > 0) (netProfit / netSales) * 100.0 else 0.0

        val incomeStatement = IncomeStatementData(
            grossSales = grossSales,
            salesDiscountsAndReturns = salesDiscountsAndReturns,
            netSales = netSales,
            costOfGoodsSold = costOfGoodsSold,
            grossProfit = grossProfit,
            grossProfitMargin = grossProfitMargin,
            expensesByCategory = expensesByCategoryMap,
            totalOperatingExpenses = totalOperatingExpenses,
            otherRevenues = otherRevenues,
            otherExpenses = otherExpenses,
            netOperatingIncome = netOperatingIncome,
            netProfit = netProfit,
            netProfitMargin = netProfitMargin
        )

        // 5. Balance Sheet items
        val latestCash = cashTransactions.maxByOrNull { it.id }?.balanceAfter
            ?: (cashTransactions.filter { it.type == "IN" }.sumOf { it.amount } - cashTransactions.filter { it.type == "OUT" }.sumOf { it.amount })
        val cashOnHand = maxOf(0.0, latestCash)

        val electronicSales = completedSales
            .filter { it.paymentMethod in listOf("إلكتروني", "شبكة", "تحويل بنكي") }
            .sumOf { it.paidAmount }
        val bankAndCards = maxOf(0.0, electronicSales + jvBank)

        val accountsReceivable = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val inventoryEndingValue = products.sumOf { product ->
            val units = product.currentStockSubUnits.coerceAtLeast(0.0)
            val factor = product.conversionFactor.coerceAtLeast(1.0)
            units * (product.purchasePrice / factor)
        }
        val fixedAssets = jvFixedAssets
        val totalCurrentAssets = cashOnHand + bankAndCards + accountsReceivable + inventoryEndingValue
        val totalAssets = totalCurrentAssets + fixedAssets

        val accountsPayable = suppliers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val otherLiabilities = jvOtherLiabilities
        val totalLiabilities = accountsPayable + otherLiabilities

        // Equity calculations
        val targetEquity = maxOf(0.0, totalAssets - totalLiabilities)
        val baseCapital = if (jvCapital > 0) jvCapital else maxOf(0.0, targetEquity - netProfit + jvDrawings)
        val totalEquity = baseCapital - jvDrawings + netProfit
        val totalLiabilitiesAndEquity = totalLiabilities + totalEquity
        val balanceDifference = Math.abs(totalAssets - totalLiabilitiesAndEquity)
        val isBalanceSheetBalanced = balanceDifference < 0.05

        val balanceSheet = BalanceSheetData(
            cashOnHand = cashOnHand,
            bankAndCards = bankAndCards,
            accountsReceivable = accountsReceivable,
            inventoryEndingValue = inventoryEndingValue,
            fixedAssets = fixedAssets,
            otherAssets = 0.0,
            totalCurrentAssets = totalCurrentAssets,
            totalAssets = totalAssets,
            accountsPayable = accountsPayable,
            otherLiabilities = otherLiabilities,
            totalLiabilities = totalLiabilities,
            capital = baseCapital,
            ownerDrawings = jvDrawings,
            netProfitCurrentPeriod = netProfit,
            retainedEarnings = 0.0,
            totalEquity = totalEquity,
            totalLiabilitiesAndEquity = totalLiabilitiesAndEquity,
            difference = balanceDifference,
            isBalanced = isBalanceSheetBalanced
        )

        // 6. Purchases for Trading Account
        val completedPurchases = invoices.filter { it.invoiceType == "PURCHASE" && it.status != "CANCELLED" }
        val totalPurchases = completedPurchases.sumOf { it.totalAmount }
        val tradingAccount = TradingAccountData(
            beginningInventory = 0.0,
            purchasesTotal = totalPurchases,
            purchaseExpenses = 0.0,
            salesTotal = netSales,
            endingInventory = inventoryEndingValue,
            grossProfit = grossProfit,
            totalTradingDebit = totalPurchases + grossProfit,
            totalTradingCredit = netSales + inventoryEndingValue
        )

        // 7. Trial Balance (ميزان المراجعة)
        val accounts = mutableListOf<TrialBalanceAccount>()

        fun addAccount(
            code: String,
            name: String,
            category: String,
            debit: Double,
            credit: Double
        ) {
            val debitBal = if (debit >= credit) debit - credit else 0.0
            val creditBal = if (credit > debit) credit - debit else 0.0
            if (debit > 0 || credit > 0 || debitBal > 0 || creditBal > 0) {
                accounts.add(
                    TrialBalanceAccount(
                        code = code,
                        name = name,
                        category = category,
                        debitTotal = debit,
                        creditTotal = credit,
                        debitBalance = debitBal,
                        creditBalance = creditBal
                    )
                )
            }
        }

        // 101 - Cash
        val cashIn = cashTransactions.filter { it.type == "IN" }.sumOf { it.amount }
        val cashOut = cashTransactions.filter { it.type == "OUT" }.sumOf { it.amount }
        addAccount("101", "الصندوق الرئيسي والنقدية", "أصول", cashIn, cashOut)

        // 102 - Bank
        val bankIn = bankAndCards
        val bankOut = 0.0
        if (bankIn > 0) {
            addAccount("102", "البنك والمدفوعات الإلكترونية", "أصول", bankIn, bankOut)
        }

        // 103 - Receivables
        val customerDebit = customers.sumOf { it.totalSales }
        val customerCredit = customers.sumOf { it.totalPaid }
        addAccount("103", "ذمم العملاء والمدينون", "أصول", customerDebit, customerCredit)

        // 104 - Inventory
        addAccount("104", "مخزون البضاعة (بضاعة آخر المدة)", "أصول", inventoryEndingValue, 0.0)

        // 105 - Fixed Assets
        if (fixedAssets > 0) {
            addAccount("105", "الأصول الثابتة والمعدات", "أصول", fixedAssets, 0.0)
        }

        // 201 - Suppliers
        val supplierCredit = suppliers.sumOf { it.totalPurchases }
        val supplierDebit = suppliers.sumOf { it.totalPaid }
        addAccount("201", "ذمم الموردين والدائنون", "خصوم", supplierDebit, supplierCredit)

        // 202 - Other Liabilities
        if (otherLiabilities > 0) {
            addAccount("202", "التزامات وقروض دائنة أخرى", "خصوم", 0.0, otherLiabilities)
        }

        // 301 - Capital
        addAccount("301", "رأس المال المستثمر", "حقوق الملكية", 0.0, baseCapital)

        // 302 - Drawings
        if (jvDrawings > 0) {
            addAccount("302", "جاري المالك والمسحوبات الشخصية", "حقوق الملكية", jvDrawings, 0.0)
        }

        // 401 - Sales
        addAccount("401", "إيرادات المبيعات", "إيرادات", 0.0, grossSales)

        // 402 - Sales returns / discounts
        if (salesDiscountsAndReturns > 0) {
            addAccount("402", "خصومات ومردودات المبيعات", "إيرادات", salesDiscountsAndReturns, 0.0)
        }

        // 403 - Other revenues
        if (otherRevenues > 0) {
            addAccount("403", "إيرادات وأرباح أخرى", "إيرادات", 0.0, otherRevenues)
        }

        // 501 - Cost of Goods Sold
        addAccount("501", "تكلفة البضاعة المباعة", "مصروفات", costOfGoodsSold, 0.0)

        // Expenses breakdown
        expensesByCategoryMap.forEachIndexed { index, (category, amount) ->
            val code = (502 + index).toString()
            addAccount(code, "مصروفات: $category", "مصروفات", amount, 0.0)
        }

        if (otherExpenses > 0) {
            addAccount("599", "مصروفات وخسائر أخرى", "مصروفات", otherExpenses, 0.0)
        }

        // Integrate any custom accounts in Journal lines that were not listed above
        val customJvLines = journalLines.filter { line ->
            accounts.none { it.code == line.accountCode || it.name == line.accountName }
        }
        val customGroups = customJvLines.groupBy { it.accountName }
        customGroups.forEach { (accName, lines) ->
            val debit = lines.sumOf { it.debit }
            val credit = lines.sumOf { it.credit }
            val firstCode = lines.firstOrNull()?.accountCode?.ifBlank { "900" } ?: "900"
            val category = when {
                firstCode.startsWith("1") -> "أصول"
                firstCode.startsWith("2") -> "خصوم"
                firstCode.startsWith("3") -> "حقوق الملكية"
                firstCode.startsWith("4") -> "إيرادات"
                else -> "مصروفات"
            }
            addAccount(firstCode, accName, category, debit, credit)
        }

        val totalDebitSum = accounts.sumOf { it.debitTotal }
        val totalCreditSum = accounts.sumOf { it.creditTotal }
        val totalDebitBalance = accounts.sumOf { it.debitBalance }
        val totalCreditBalance = accounts.sumOf { it.creditBalance }
        val diff = Math.abs(totalDebitBalance - totalCreditBalance)
        val isBalanced = diff < 0.05

        val trialBalance = TrialBalanceData(
            accounts = accounts,
            totalDebitSum = totalDebitSum,
            totalCreditSum = totalCreditSum,
            totalDebitBalance = totalDebitBalance,
            totalCreditBalance = totalCreditBalance,
            isBalanced = isBalanced,
            difference = diff
        )

        return ClosingAccountsBundle(
            trialBalance = trialBalance,
            incomeStatement = incomeStatement,
            balanceSheet = balanceSheet,
            tradingAccount = tradingAccount
        )
    }
}

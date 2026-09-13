package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountingRepository(private val dao: AppDao) {

    // Settings
    val settings: Flow<StoreSettings?> = dao.getSettings()
    suspend fun getSettingsDirect(): StoreSettings? = dao.getSettingsDirect()
    suspend fun saveSettings(settings: StoreSettings) = dao.saveSettings(settings)

    // Users
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    suspend fun getUserByUsername(username: String): User? = dao.getUserByUsername(username)
    suspend fun insertUser(user: User): Long = dao.insertUser(user)
    suspend fun updateUser(user: User) = dao.updateUser(user)
    suspend fun deleteUser(user: User) = dao.deleteUser(user)
    suspend fun getUserCount(): Int = dao.getUserCount()

    // Categories
    val allCategories: Flow<List<Category>> = dao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = dao.insertCategory(category)

    // Products
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val activeProducts: Flow<List<Product>> = dao.getActiveProducts()
    val lowStockProducts: Flow<List<Product>> = dao.getLowStockProducts()
    val outOfStockProducts: Flow<List<Product>> = dao.getOutOfStockProducts()
    suspend fun getProductById(id: Long): Product? = dao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = dao.getProductByBarcode(barcode)
    fun searchProducts(query: String): Flow<List<Product>> = dao.searchProducts(query)
    suspend fun insertProduct(product: Product): Long = dao.insertProduct(product)
    suspend fun insertProducts(products: List<Product>): List<Long> = dao.insertProducts(products)
    suspend fun updateProduct(product: Product) = dao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = dao.deleteProduct(product)

    // Customers
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val debtorCustomers: Flow<List<Customer>> = dao.getDebtorCustomers()
    suspend fun getCustomerById(id: Long): Customer? = dao.getCustomerById(id)
    fun searchCustomers(query: String): Flow<List<Customer>> = dao.searchCustomers(query)
    suspend fun insertCustomer(customer: Customer): Long = dao.insertCustomer(customer)
    suspend fun insertCustomers(customers: List<Customer>): List<Long> = dao.insertCustomers(customers)
    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = dao.deleteCustomer(customer)

    // Suppliers
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()
    val creditorSuppliers: Flow<List<Supplier>> = dao.getCreditorSuppliers()
    suspend fun getSupplierById(id: Long): Supplier? = dao.getSupplierById(id)
    fun searchSuppliers(query: String): Flow<List<Supplier>> = dao.searchSuppliers(query)
    suspend fun insertSupplier(supplier: Supplier): Long = dao.insertSupplier(supplier)
    suspend fun insertSuppliers(suppliers: List<Supplier>): List<Long> = dao.insertSuppliers(suppliers)
    suspend fun updateSupplier(supplier: Supplier) = dao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = dao.deleteSupplier(supplier)

    // Invoices
    val allInvoices: Flow<List<Invoice>> = dao.getAllInvoices()
    fun getInvoicesByType(type: String): Flow<List<Invoice>> = dao.getInvoicesByType(type)
    suspend fun getInvoiceById(id: Long): Invoice? = dao.getInvoiceById(id)
    fun getInvoicesByParty(partyId: Long, type: String): Flow<List<Invoice>> = dao.getInvoicesByParty(partyId, type)
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>> = dao.getInvoiceItems(invoiceId)
    suspend fun getInvoiceItemsDirect(invoiceId: Long): List<InvoiceItem> = dao.getInvoiceItemsDirect(invoiceId)
    val allInvoiceItems: Flow<List<InvoiceItem>> = dao.getAllInvoiceItems()

    // Vouchers
    val allVouchers: Flow<List<Voucher>> = dao.getAllVouchers()
    fun getVouchersByType(type: String): Flow<List<Voucher>> = dao.getVouchersByType(type)
    fun getVouchersByParty(partyId: Long): Flow<List<Voucher>> = dao.getVouchersByParty(partyId)

    // Journal Vouchers (سندات القيد)
    val allJournalVouchers: Flow<List<JournalVoucher>> = dao.getAllJournalVouchers()
    val allJournalVoucherLines: Flow<List<JournalVoucherLine>> = dao.getAllJournalVoucherLines()
    fun getJournalLines(voucherId: Long): Flow<List<JournalVoucherLine>> = dao.getJournalVoucherLines(voucherId)
    suspend fun getJournalLinesDirect(voucherId: Long): List<JournalVoucherLine> = dao.getJournalVoucherLinesDirect(voucherId)

    // Expenses
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()
    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)

    // Cash
    val allCashTransactions: Flow<List<CashTransaction>> = dao.getAllCashTransactions()
    suspend fun getLatestCashBalance(): Double {
        return dao.getLatestCashTransaction()?.balanceAfter ?: 0.0
    }

    // Inventory
    val allInventoryTransactions: Flow<List<InventoryTransaction>> = dao.getAllInventoryTransactions()
    fun getProductInventoryTransactions(productId: Long): Flow<List<InventoryTransaction>> =
        dao.getProductInventoryTransactions(productId)

    // Audit
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs()
    suspend fun logAudit(action: String, details: String, user: String = "مدير النظام") {
        dao.insertAuditLog(AuditLog(action = action, details = details, userName = user))
    }

    // --- Core Accounting Transactions ---

    data class CartItem(
        val product: Product,
        val isMainUnit: Boolean,
        val quantity: Double,
        val unitPrice: Double,
        val unitCost: Double,
        val discount: Double = 0.0
    ) {
        val unitName: String
            get() = if (isMainUnit) product.mainUnit else product.subUnit

        val subUnitQuantity: Double
            get() = if (isMainUnit) quantity * product.conversionFactor else quantity

        val subtotal: Double
            get() = quantity * unitPrice

        val total: Double
            get() = maxOf(0.0, subtotal - discount)

        val totalCost: Double
            get() = subUnitQuantity * (product.purchasePrice / maxOf(1.0, product.conversionFactor))
    }

    suspend fun performSaleInvoice(
        customer: Customer?,
        cartItems: List<CartItem>,
        discountAmount: Double,
        taxAmount: Double,
        paidAmount: Double,
        paymentType: String, // CASH or CREDIT
        paymentMethod: String = "كاش", // كاش / نقداً, إلكتروني / شبكة, تحويل بنكي, آجل
        paidCurrency: String = "العملة الأساسية",
        paidCurrencyAmount: Double = 0.0,
        exchangeRate: Double = 1.0,
        notes: String,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val invoiceNumber = "INV-" + dateFormat.format(Date())

        val subtotal = cartItems.sumOf { it.total }
        val totalAmount = maxOf(0.0, subtotal - discountAmount + taxAmount)
        val remainingAmount = if (paymentType == "CASH") 0.0 else maxOf(0.0, totalAmount - paidAmount)
        val actualPaid = if (paymentType == "CASH") totalAmount else paidAmount
        val totalCost = cartItems.sumOf { it.totalCost }
        val profit = (totalAmount - taxAmount) - totalCost

        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            invoiceType = "SALE",
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            partyId = customer?.id,
            partyName = customer?.name ?: "عميل نقدي",
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            paidAmount = actualPaid,
            remainingAmount = remainingAmount,
            paidCurrency = paidCurrency,
            paidCurrencyAmount = if (paidCurrencyAmount > 0) paidCurrencyAmount else actualPaid,
            exchangeRate = exchangeRate,
            totalCost = totalCost,
            profit = profit,
            notes = notes,
            createdBy = currentUser
        )
        val invoiceId = dao.insertInvoice(invoice)

        // Save items & update stock
        val itemsToInsert = mutableListOf<InvoiceItem>()
        for (item in cartItems) {
            val invoiceItem = InvoiceItem(
                invoiceId = invoiceId,
                productId = item.product.id,
                productName = item.product.name,
                unitName = item.unitName,
                isMainUnit = item.isMainUnit,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                subtotal = item.subtotal,
                discount = item.discount,
                total = item.total,
                totalCost = item.totalCost
            )
            itemsToInsert.add(invoiceItem)

            // Deduct stock in subUnits
            val currentStock = item.product.currentStockSubUnits
            val newStock = currentStock - item.subUnitQuantity
            dao.updateProduct(item.product.copy(currentStockSubUnits = newStock))

            // Log inventory movement
            dao.insertInventoryTransaction(
                InventoryTransaction(
                    productId = item.product.id,
                    productName = item.product.name,
                    transactionType = "SALE",
                    quantitySubUnits = -item.subUnitQuantity,
                    balanceAfterSubUnits = newStock,
                    unitName = item.unitName,
                    referenceNumber = invoiceNumber,
                    notes = "بيع بفاتورة $invoiceNumber",
                    createdBy = currentUser
                )
            )
        }
        dao.insertInvoiceItems(itemsToInsert)

        // Update Cash register
        if (actualPaid > 0) {
            val currentCash = getLatestCashBalance()
            val methodTag = when (paymentMethod) {
                "إلكتروني / شبكة" -> "[دفع إلكتروني]"
                "تحويل بنكي" -> "[تحويل بنكي]"
                "آجل" -> "[دفعة مقدمة]"
                else -> "[نقداً]"
            }
            val currTag = if (paidCurrency.isNotBlank() && paidCurrency != "العملة الأساسية") {
                " ($paidCurrencyAmount $paidCurrency)"
            } else ""
            dao.insertCashTransaction(
                CashTransaction(
                    type = "IN",
                    source = "SALE",
                    referenceId = invoiceId,
                    referenceNumber = invoiceNumber,
                    amount = actualPaid,
                    balanceAfter = currentCash + actualPaid,
                    notes = "تحصيل مبيعات فاتورة $invoiceNumber $methodTag$currTag",
                    createdBy = currentUser
                )
            )
        }

        // Update Customer Balance if credit
        if (customer != null) {
            val updatedBalance = customer.currentBalance + remainingAmount
            val updatedSales = customer.totalSales + totalAmount
            val updatedPaid = customer.totalPaid + actualPaid
            dao.updateCustomer(
                customer.copy(
                    currentBalance = updatedBalance,
                    totalSales = updatedSales,
                    totalPaid = updatedPaid
                )
            )
        }

        logAudit(
            action = "فاتورة بيع",
            details = "إنشاء فاتورة بيع $invoiceNumber بقيمة $totalAmount للعميل ${customer?.name ?: "نقدي"}",
            user = currentUser
        )

        return invoiceId
    }

    suspend fun performPurchaseInvoice(
        supplier: Supplier?,
        cartItems: List<CartItem>,
        discountAmount: Double,
        taxAmount: Double,
        paidAmount: Double,
        paymentType: String, // CASH or CREDIT
        paymentMethod: String = "كاش",
        paidCurrency: String = "العملة الأساسية",
        paidCurrencyAmount: Double = 0.0,
        exchangeRate: Double = 1.0,
        referenceNumber: String,
        notes: String,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val invoiceNumber = if (referenceNumber.isNotBlank()) referenceNumber else "PUR-" + dateFormat.format(Date())

        val subtotal = cartItems.sumOf { it.total }
        val totalAmount = maxOf(0.0, subtotal - discountAmount + taxAmount)
        val remainingAmount = if (paymentType == "CASH") 0.0 else maxOf(0.0, totalAmount - paidAmount)
        val actualPaid = if (paymentType == "CASH") totalAmount else paidAmount

        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            invoiceType = "PURCHASE",
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            partyId = supplier?.id,
            partyName = supplier?.name ?: "مورد نقدي",
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            paidAmount = actualPaid,
            remainingAmount = remainingAmount,
            paidCurrency = paidCurrency,
            paidCurrencyAmount = paidCurrencyAmount,
            exchangeRate = exchangeRate,
            totalCost = totalAmount,
            profit = 0.0,
            notes = notes,
            createdBy = currentUser
        )
        val invoiceId = dao.insertInvoice(invoice)

        // Save items & increase stock
        val itemsToInsert = mutableListOf<InvoiceItem>()
        for (item in cartItems) {
            val invoiceItem = InvoiceItem(
                invoiceId = invoiceId,
                productId = item.product.id,
                productName = item.product.name,
                unitName = item.unitName,
                isMainUnit = item.isMainUnit,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                subtotal = item.subtotal,
                discount = item.discount,
                total = item.total,
                totalCost = item.totalCost
            )
            itemsToInsert.add(invoiceItem)

            // Add stock in subUnits
            val currentStock = item.product.currentStockSubUnits
            val newStock = currentStock + item.subUnitQuantity
            // Update purchase price
            val newPurchasePrice = if (item.isMainUnit) item.unitPrice else item.unitPrice * item.product.conversionFactor
            dao.updateProduct(
                item.product.copy(
                    currentStockSubUnits = newStock,
                    purchasePrice = newPurchasePrice
                )
            )

            // Log inventory movement
            dao.insertInventoryTransaction(
                InventoryTransaction(
                    productId = item.product.id,
                    productName = item.product.name,
                    transactionType = "PURCHASE",
                    quantitySubUnits = item.subUnitQuantity,
                    balanceAfterSubUnits = newStock,
                    unitName = item.unitName,
                    referenceNumber = invoiceNumber,
                    notes = "شراء بفاتورة $invoiceNumber",
                    createdBy = currentUser
                )
            )
        }
        dao.insertInvoiceItems(itemsToInsert)

        // Deduct from Cash register
        if (actualPaid > 0) {
            val currentCash = getLatestCashBalance()
            dao.insertCashTransaction(
                CashTransaction(
                    type = "OUT",
                    source = "PURCHASE",
                    referenceId = invoiceId,
                    referenceNumber = invoiceNumber,
                    amount = actualPaid,
                    balanceAfter = currentCash - actualPaid,
                    notes = "سداد مشتريات فاتورة $invoiceNumber",
                    createdBy = currentUser
                )
            )
        }

        // Update Supplier Balance if credit
        if (supplier != null) {
            val updatedBalance = supplier.currentBalance + remainingAmount
            val updatedPurchases = supplier.totalPurchases + totalAmount
            val updatedPaid = supplier.totalPaid + actualPaid
            dao.updateSupplier(
                supplier.copy(
                    currentBalance = updatedBalance,
                    totalPurchases = updatedPurchases,
                    totalPaid = updatedPaid
                )
            )
        }

        logAudit(
            action = "فاتورة شراء",
            details = "إنشاء فاتورة شراء $invoiceNumber بقيمة $totalAmount من المورد ${supplier?.name ?: "نقدي"}",
            user = currentUser
        )

        return invoiceId
    }

    suspend fun saveQuotation(
        customer: Customer?,
        cartItems: List<CartItem>,
        discountAmount: Double,
        taxAmount: Double,
        notes: String,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val quoteNumber = "QTE-" + dateFormat.format(Date())
        val subtotal = cartItems.sumOf { it.total }
        val totalAmount = maxOf(0.0, subtotal - discountAmount + taxAmount)
        val totalCost = cartItems.sumOf { it.totalCost }
        val profit = (totalAmount - taxAmount) - totalCost

        val quotation = Invoice(
            invoiceNumber = quoteNumber,
            invoiceType = "QUOTATION",
            paymentType = "CASH",
            partyId = customer?.id,
            partyName = customer?.name ?: "عميل عام",
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            paidAmount = 0.0,
            remainingAmount = totalAmount,
            totalCost = totalCost,
            profit = profit,
            notes = notes,
            createdBy = currentUser,
            status = "ACTIVE"
        )
        val quoteId = dao.insertInvoice(quotation)
        val itemsToInsert = cartItems.map { item ->
            InvoiceItem(
                invoiceId = quoteId,
                productId = item.product.id,
                productName = item.product.name,
                unitName = item.unitName,
                isMainUnit = item.isMainUnit,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                subtotal = item.subtotal,
                discount = item.discount,
                total = item.total,
                totalCost = item.totalCost
            )
        }
        dao.insertInvoiceItems(itemsToInsert)
        logAudit("عرض أسعار", "إنشاء عرض أسعار $quoteNumber بقيمة $totalAmount للعميل ${quotation.partyName}", currentUser)
        return quoteId
    }

    suspend fun performSalesReturn(
        originalInvoice: Invoice,
        returnedItems: List<InvoiceItem>,
        returnAmount: Double,
        refundMethod: String, // CASH or REDUCE_DEBT
        notes: String,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val returnNumber = "RET-" + dateFormat.format(Date())
        val totalCost = returnedItems.sumOf { it.totalCost }

        val returnInvoice = Invoice(
            invoiceNumber = returnNumber,
            invoiceType = "SALE_RETURN",
            paymentType = if (refundMethod == "CASH") "CASH" else "CREDIT",
            partyId = originalInvoice.partyId,
            partyName = originalInvoice.partyName,
            subtotal = returnAmount,
            discountAmount = 0.0,
            taxAmount = 0.0,
            totalAmount = returnAmount,
            paidAmount = if (refundMethod == "CASH") returnAmount else 0.0,
            remainingAmount = 0.0,
            totalCost = totalCost,
            profit = 0.0,
            notes = "مردودات للفاتورة ${originalInvoice.invoiceNumber}. $notes",
            createdBy = currentUser,
            status = "COMPLETED"
        )
        val returnId = dao.insertInvoice(returnInvoice)

        // Restore stock for returned items
        val itemsToInsert = mutableListOf<InvoiceItem>()
        for (item in returnedItems) {
            itemsToInsert.add(
                item.copy(id = 0, invoiceId = returnId)
            )
            val product = dao.getProductById(item.productId)
            if (product != null) {
                val subUnitsReturned = if (item.isMainUnit) item.quantity * product.conversionFactor else item.quantity
                val newStock = product.currentStockSubUnits + subUnitsReturned
                dao.updateProduct(product.copy(currentStockSubUnits = newStock))
                dao.insertInventoryTransaction(
                    InventoryTransaction(
                        productId = product.id,
                        productName = product.name,
                        transactionType = "RETURN",
                        quantitySubUnits = subUnitsReturned,
                        balanceAfterSubUnits = newStock,
                        unitName = item.unitName,
                        referenceNumber = returnNumber,
                        notes = "مردودات مبيعات بالفاتورة $returnNumber",
                        createdBy = currentUser
                    )
                )
            }
        }
        dao.insertInvoiceItems(itemsToInsert)

        // Handle Cash refund if cash
        if (refundMethod == "CASH" && returnAmount > 0) {
            val currentCash = getLatestCashBalance()
            dao.insertCashTransaction(
                CashTransaction(
                    type = "OUT",
                    source = "RETURN",
                    referenceId = returnId,
                    referenceNumber = returnNumber,
                    amount = returnAmount,
                    balanceAfter = currentCash - returnAmount,
                    notes = "صرف نقدي لمردودات فاتورة $returnNumber",
                    createdBy = currentUser
                )
            )
        } else if (originalInvoice.partyId != null && returnAmount > 0) {
            // Deduct from customer debt
            val customer = dao.getCustomerById(originalInvoice.partyId)
            if (customer != null) {
                dao.updateCustomer(
                    customer.copy(
                        currentBalance = maxOf(0.0, customer.currentBalance - returnAmount)
                    )
                )
            }
        }

        logAudit("مردود مبيعات", "تسجيل مردود مبيعات $returnNumber للفاتورة ${originalInvoice.invoiceNumber} بقيمة $returnAmount", currentUser)
        return returnId
    }

    suspend fun performReceiptVoucher(
        partyType: String, // CUSTOMER, OTHER
        partyId: Long?,
        partyName: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        notes: String,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val voucherNumber = "REC-" + dateFormat.format(Date())

        val voucher = Voucher(
            voucherNumber = voucherNumber,
            type = "RECEIPT",
            partyType = partyType,
            partyId = partyId,
            partyName = partyName,
            amount = amount,
            paymentMethod = paymentMethod,
            description = description,
            notes = notes,
            createdBy = currentUser
        )
        val voucherId = dao.insertVoucher(voucher)

        // Cash in
        val currentCash = getLatestCashBalance()
        dao.insertCashTransaction(
            CashTransaction(
                type = "IN",
                source = "RECEIPT",
                referenceId = voucherId,
                referenceNumber = voucherNumber,
                amount = amount,
                balanceAfter = currentCash + amount,
                notes = "سند قبض رقم $voucherNumber من $partyName: $description",
                createdBy = currentUser
            )
        )

        // If customer, reduce debt
        if (partyType == "CUSTOMER" && partyId != null) {
            val customer = dao.getCustomerById(partyId)
            if (customer != null) {
                dao.updateCustomer(
                    customer.copy(
                        currentBalance = customer.currentBalance - amount,
                        totalPaid = customer.totalPaid + amount
                    )
                )
            }
        }

        logAudit(
            action = "سند قبض",
            details = "قبض مبلغ $amount من $partyName بسند $voucherNumber",
            user = currentUser
        )

        return voucherId
    }

    suspend fun performPaymentVoucher(
        partyType: String, // SUPPLIER, OTHER
        partyId: Long?,
        partyName: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        notes: String,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val voucherNumber = "PAY-" + dateFormat.format(Date())

        val voucher = Voucher(
            voucherNumber = voucherNumber,
            type = "PAYMENT",
            partyType = partyType,
            partyId = partyId,
            partyName = partyName,
            amount = amount,
            paymentMethod = paymentMethod,
            description = description,
            notes = notes,
            createdBy = currentUser
        )
        val voucherId = dao.insertVoucher(voucher)

        // Cash out
        val currentCash = getLatestCashBalance()
        dao.insertCashTransaction(
            CashTransaction(
                type = "OUT",
                source = "PAYMENT",
                referenceId = voucherId,
                referenceNumber = voucherNumber,
                amount = amount,
                balanceAfter = currentCash - amount,
                notes = "سند صرف رقم $voucherNumber إلى $partyName: $description",
                createdBy = currentUser
            )
        )

        // If supplier, reduce debt owed to supplier
        if (partyType == "SUPPLIER" && partyId != null) {
            val supplier = dao.getSupplierById(partyId)
            if (supplier != null) {
                dao.updateSupplier(
                    supplier.copy(
                        currentBalance = supplier.currentBalance - amount,
                        totalPaid = supplier.totalPaid + amount
                    )
                )
            }
        }

        logAudit(
            action = "سند صرف",
            details = "صرف مبلغ $amount إلى $partyName بسند $voucherNumber",
            user = currentUser
        )

        return voucherId
    }

    suspend fun performExpense(
        category: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        notes: String,
        currentUser: String
    ): Long {
        val expense = Expense(
            category = category,
            amount = amount,
            paymentMethod = paymentMethod,
            description = description,
            notes = notes,
            createdBy = currentUser
        )
        val expenseId = dao.insertExpense(expense)

        // Cash out
        val currentCash = getLatestCashBalance()
        dao.insertCashTransaction(
            CashTransaction(
                type = "OUT",
                source = "EXPENSE",
                referenceId = expenseId,
                referenceNumber = "EXP-$expenseId",
                amount = amount,
                balanceAfter = currentCash - amount,
                notes = "مصروف $category: $description",
                createdBy = currentUser
            )
        )

        logAudit(
            action = "تسجيل مصروف",
            details = "تسجيل مصروف $category بقيمة $amount: $description",
            user = currentUser
        )

        return expenseId
    }

    suspend fun adjustInventory(
        product: Product,
        newStockSubUnits: Double,
        reason: String,
        currentUser: String
    ) {
        val diff = newStockSubUnits - product.currentStockSubUnits
        dao.updateProduct(product.copy(currentStockSubUnits = newStockSubUnits))

        dao.insertInventoryTransaction(
            InventoryTransaction(
                productId = product.id,
                productName = product.name,
                transactionType = "ADJUSTMENT",
                quantitySubUnits = diff,
                balanceAfterSubUnits = newStockSubUnits,
                unitName = product.subUnit,
                referenceNumber = "ADJ-" + System.currentTimeMillis(),
                notes = "تسوية مخزون: $reason (الفرق: $diff)",
                createdBy = currentUser
            )
        )

        logAudit(
            action = "تسوية مخزون",
            details = "تعديل مخزون ${product.name} من ${product.currentStockSubUnits} إلى $newStockSubUnits. السبب: $reason",
            user = currentUser
        )
    }

    suspend fun setOpeningCashBalance(amount: Double, currentUser: String) {
        dao.insertCashTransaction(
            CashTransaction(
                type = "IN",
                source = "OPENING",
                amount = amount,
                balanceAfter = amount,
                notes = "رصيد افتتاحي للصندوق",
                createdBy = currentUser
            )
        )
        logAudit(
            action = "رصيد افتتاحي",
            details = "تعيين رصيد افتتاحي للصندوق بقيمة $amount",
            user = currentUser
        )
    }

    suspend fun cancelInvoice(invoice: Invoice, currentUser: String) {
        if (invoice.status == "CANCELLED") return

        val items = dao.getInvoiceItemsDirect(invoice.id)

        if (invoice.invoiceType == "SALE") {
            // Revert stock (add back items)
            for (item in items) {
                val product = dao.getProductById(item.productId)
                if (product != null) {
                    val subUnits = if (item.isMainUnit) item.quantity * product.conversionFactor else item.quantity
                    val newStock = product.currentStockSubUnits + subUnits
                    dao.updateProduct(product.copy(currentStockSubUnits = newStock))

                    dao.insertInventoryTransaction(
                        InventoryTransaction(
                            productId = product.id,
                            productName = product.name,
                            transactionType = "IN",
                            quantitySubUnits = subUnits,
                            balanceAfterSubUnits = newStock,
                            unitName = item.unitName,
                            referenceNumber = "REV-${invoice.invoiceNumber}",
                            notes = "إلغاء فاتورة بيع ${invoice.invoiceNumber}",
                            createdBy = currentUser
                        )
                    )
                }
            }

            // Revert Cash
            if (invoice.paidAmount > 0) {
                val currentCash = getLatestCashBalance()
                dao.insertCashTransaction(
                    CashTransaction(
                        type = "OUT",
                        source = "SALE",
                        referenceId = invoice.id,
                        referenceNumber = "REV-${invoice.invoiceNumber}",
                        amount = invoice.paidAmount,
                        balanceAfter = currentCash - invoice.paidAmount,
                        notes = "عكس مبيعات فاتورة ملغاة ${invoice.invoiceNumber}",
                        createdBy = currentUser
                    )
                )
            }

            // Revert Customer balance
            if (invoice.partyId != null) {
                val customer = dao.getCustomerById(invoice.partyId)
                if (customer != null) {
                    dao.updateCustomer(
                        customer.copy(
                            currentBalance = customer.currentBalance - invoice.remainingAmount,
                            totalSales = customer.totalSales - invoice.totalAmount,
                            totalPaid = customer.totalPaid - invoice.paidAmount
                        )
                    )
                }
            }
        } else if (invoice.invoiceType == "PURCHASE") {
            // Revert stock (deduct items)
            for (item in items) {
                val product = dao.getProductById(item.productId)
                if (product != null) {
                    val subUnits = if (item.isMainUnit) item.quantity * product.conversionFactor else item.quantity
                    val newStock = product.currentStockSubUnits - subUnits
                    dao.updateProduct(product.copy(currentStockSubUnits = newStock))

                    dao.insertInventoryTransaction(
                        InventoryTransaction(
                            productId = product.id,
                            productName = product.name,
                            transactionType = "OUT",
                            quantitySubUnits = -subUnits,
                            balanceAfterSubUnits = newStock,
                            unitName = item.unitName,
                            referenceNumber = "REV-${invoice.invoiceNumber}",
                            notes = "إلغاء فاتورة شراء ${invoice.invoiceNumber}",
                            createdBy = currentUser
                        )
                    )
                }
            }

            // Revert Cash
            if (invoice.paidAmount > 0) {
                val currentCash = getLatestCashBalance()
                dao.insertCashTransaction(
                    CashTransaction(
                        type = "IN",
                        source = "PURCHASE",
                        referenceId = invoice.id,
                        referenceNumber = "REV-${invoice.invoiceNumber}",
                        amount = invoice.paidAmount,
                        balanceAfter = currentCash + invoice.paidAmount,
                        notes = "استرداد مدفوعات مشتريات ملغاة ${invoice.invoiceNumber}",
                        createdBy = currentUser
                    )
                )
            }

            // Revert Supplier balance
            if (invoice.partyId != null) {
                val supplier = dao.getSupplierById(invoice.partyId)
                if (supplier != null) {
                    dao.updateSupplier(
                        supplier.copy(
                            currentBalance = supplier.currentBalance - invoice.remainingAmount,
                            totalPurchases = supplier.totalPurchases - invoice.totalAmount,
                            totalPaid = supplier.totalPaid - invoice.paidAmount
                        )
                    )
                }
            }
        }

        // Mark invoice cancelled
        dao.updateInvoice(invoice.copy(status = "CANCELLED"))

        logAudit(
            action = "إلغاء فاتورة",
            details = "إلغاء الفاتورة ${invoice.invoiceNumber} وعكس كافة تأثيراتها المخزنية والمالية",
            user = currentUser
        )
    }

    // --- CSV/Excel Export & Import Utilities ---

    suspend fun exportProductsToCsv(): String {
        val products = allProducts.firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens Arabic correctly
        sb.append("\uFEFF")
        sb.append("رقم المنتج,الباركود,اسم المنتج,التصنيف,الوحدة الرئيسية,الوحدة الفرعية,معامل التحويل,سعر الشراء,سعر البيع النقدي,سعر البيع الآجل,الكمية الحالية,الحد الأدنى,المورد,الحالة\n")
        for (p in products) {
            val mainStock = if (p.conversionFactor > 0) p.currentStockSubUnits / p.conversionFactor else p.currentStockSubUnits
            sb.append("\"${p.sku}\",")
            sb.append("\"${p.barcode}\",")
            sb.append("\"${p.name}\",")
            sb.append("\"${p.category}\",")
            sb.append("\"${p.mainUnit}\",")
            sb.append("\"${p.subUnit}\",")
            sb.append("${p.conversionFactor},")
            sb.append("${p.purchasePrice},")
            sb.append("${p.cashSalePrice},")
            sb.append("${p.creditSalePrice},")
            sb.append("${mainStock},")
            sb.append("${p.minStockSubUnits},")
            sb.append("\"${p.supplierName}\",")
            sb.append(if (p.isActive) "نشط" else "غير نشط")
            sb.append("\n")
        }
        return sb.toString()
    }

    suspend fun exportCustomersToCsv(): String {
        val customers = allCustomers.firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم العميل,اسم العميل,الهاتف,العنوان,الحد الائتماني,الرصيد الحالي,إجمالي المبيعات,إجمالي المدفوعات\n")
        for (c in customers) {
            sb.append("${c.id},\"${c.name}\",\"${c.phone}\",\"${c.address}\",${c.creditLimit},${c.currentBalance},${c.totalSales},${c.totalPaid}\n")
        }
        return sb.toString()
    }

    suspend fun exportSuppliersToCsv(): String {
        val suppliers = allSuppliers.firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم المورد,اسم المورد,الهاتف,العنوان,الرصيد الحالي المستحق,إجمالي المشتريات,إجمالي المسدد\n")
        for (s in suppliers) {
            sb.append("${s.id},\"${s.name}\",\"${s.phone}\",\"${s.address}\",${s.currentBalance},${s.totalPurchases},${s.totalPaid}\n")
        }
        return sb.toString()
    }

    suspend fun exportInvoicesToCsv(): String {
        val invoices = allInvoices.firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم الفاتورة,النوع,طريقة الدفع,الطرف,الإجمالي قبل الضريبة والخصم,الخصم,الضريبة,الصافي النهائي,المدفوع,المتبقي,التكلفة,الربح,التاريخ,الحالة\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        for (inv in invoices) {
            sb.append("\"${inv.invoiceNumber}\",")
            sb.append("\"${if (inv.invoiceType == "SALE") "بيع" else "شراء"}\",")
            sb.append("\"${if (inv.paymentType == "CASH") "نقدي" else "آجل"}\",")
            sb.append("\"${inv.partyName}\",")
            sb.append("${inv.subtotal},")
            sb.append("${inv.discountAmount},")
            sb.append("${inv.taxAmount},")
            sb.append("${inv.totalAmount},")
            sb.append("${inv.paidAmount},")
            sb.append("${inv.remainingAmount},")
            sb.append("${inv.totalCost},")
            sb.append("${inv.profit},")
            sb.append("\"${dateFormat.format(Date(inv.createdAt))}\",")
            sb.append("\"${if (inv.status == "COMPLETED") "مكتملة" else "ملغاة"}\"\n")
        }
        return sb.toString()
    }

    suspend fun exportCashToCsv(): String {
        val list = allCashTransactions.firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم الحركة,النوع,المصدر,المرجع,المبلغ,الرصيد بعد الحركة,البيان,التاريخ\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        for (c in list) {
            sb.append("${c.id},")
            sb.append("\"${if (c.type == "IN") "وارد (+)" else "صادر (-)"}\",")
            sb.append("\"${c.source}\",")
            sb.append("\"${c.referenceNumber}\",")
            sb.append("${c.amount},")
            sb.append("${c.balanceAfter},")
            sb.append("\"${c.notes}\",")
            sb.append("\"${dateFormat.format(Date(c.createdAt))}\"\n")
        }
        return sb.toString()
    }

    // --- Currency Management ---
    val allCurrencyRates: Flow<List<CurrencyRate>> = dao.getAllCurrencyRates()

    suspend fun getAllCurrencyRatesDirect(): List<CurrencyRate> = dao.getAllCurrencyRatesDirect()

    suspend fun saveCurrencyRate(rate: CurrencyRate): Long {
        if (rate.isBase) {
            val all = dao.getAllCurrencyRatesDirect()
            for (c in all) {
                if (c.id != rate.id && c.isBase) {
                    dao.updateCurrencyRate(c.copy(isBase = false))
                }
            }
        }
        return if (rate.id == 0L) {
            dao.insertCurrencyRate(rate)
        } else {
            dao.updateCurrencyRate(rate)
            rate.id
        }
    }

    suspend fun deleteCurrencyRate(rate: CurrencyRate) {
        dao.deleteCurrencyRate(rate)
    }

    suspend fun initializeDefaultCurrenciesIfEmpty(baseSymbol: String = "ر.س") {
        val current = dao.getAllCurrencyRatesDirect()
        if (current.isEmpty()) {
            val defaults = listOf(
                CurrencyRate(code = "SAR", name = "ريال سعودي", symbol = "ر.س", rateToBase = 1.0, isBase = true),
                CurrencyRate(code = "USD", name = "دولار أمريكي", symbol = "$", rateToBase = 3.75, isBase = false),
                CurrencyRate(code = "YER", name = "ريال يمني", symbol = "ر.ي", rateToBase = 0.007, isBase = false),
                CurrencyRate(code = "AED", name = "درهم إماراتي", symbol = "د.إ", rateToBase = 1.02, isBase = false),
                CurrencyRate(code = "KWD", name = "دينار كويتي", symbol = "د.ك", rateToBase = 12.2, isBase = false)
            )
            dao.insertCurrencyRates(defaults)
        }
    }

    suspend fun initializeDefaultUsersIfEmpty() {
        if (dao.getUserCount() == 0) {
            val defaultAdmin = User(
                username = "admin",
                passwordHash = "1234",
                fullName = "المدير العام",
                role = "ADMIN",
                canSell = true,
                canPurchase = true,
                canViewProfits = true,
                canViewReports = true,
                canManageInventory = true,
                canManageSettings = true,
                canManageCustomers = true,
                canManageSuppliers = true,
                canManageExpenses = true,
                canGiveDiscount = true,
                canManageUsers = true,
                isActive = true
            )
            val defaultCashier = User(
                username = "cashier",
                passwordHash = "0000",
                fullName = "موظف الكاشير",
                role = "CASHIER",
                canSell = true,
                canPurchase = false,
                canViewProfits = false,
                canViewReports = false,
                canManageInventory = false,
                canManageSettings = false,
                canManageCustomers = true,
                canManageSuppliers = false,
                canManageExpenses = false,
                canGiveDiscount = false,
                canManageUsers = false,
                isActive = true
            )
            val defaultAccountant = User(
                username = "accountant",
                passwordHash = "1111",
                fullName = "محاسب المتجر",
                role = "ACCOUNTANT",
                canSell = true,
                canPurchase = true,
                canViewProfits = true,
                canViewReports = true,
                canManageInventory = true,
                canManageSettings = false,
                canManageCustomers = true,
                canManageSuppliers = true,
                canManageExpenses = true,
                canGiveDiscount = true,
                canManageUsers = false,
                isActive = true
            )
            dao.insertUser(defaultAdmin)
            dao.insertUser(defaultCashier)
            dao.insertUser(defaultAccountant)
        }
    }

    suspend fun performJournalVoucher(
        date: Long,
        reference: String,
        narration: String,
        lines: List<JournalVoucherLine>,
        currentUser: String
    ): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH)
        val voucherNumber = "JV-" + dateFormat.format(Date(date))

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }
        val isBalanced = Math.abs(totalDebit - totalCredit) < 0.001

        val voucher = JournalVoucher(
            voucherNumber = voucherNumber,
            voucherDate = date,
            reference = reference,
            narration = narration,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            isBalanced = isBalanced,
            status = "POSTED",
            createdBy = currentUser
        )
        val voucherId = dao.insertJournalVoucher(voucher)

        val linesWithId = lines.map { it.copy(voucherId = voucherId) }
        dao.insertJournalVoucherLines(linesWithId)

        // Update balances for Cash, Customers, and Suppliers
        for (line in linesWithId) {
            // Cash impact
            if (line.partyType == "CASH" || line.accountCode == "101" || line.accountName.contains("الصندوق")) {
                val netCashChange = line.debit - line.credit
                if (Math.abs(netCashChange) > 0.0001) {
                    val currentCash = getLatestCashBalance()
                    val type = if (netCashChange > 0) "IN" else "OUT"
                    val amount = Math.abs(netCashChange)
                    dao.insertCashTransaction(
                        CashTransaction(
                            type = type,
                            source = "JOURNAL_VOUCHER",
                            referenceId = voucherId,
                            referenceNumber = voucherNumber,
                            amount = amount,
                            balanceAfter = if (type == "IN") currentCash + amount else currentCash - amount,
                            notes = "سند قيد رقم $voucherNumber: ${line.description.ifBlank { narration }}",
                            createdBy = currentUser
                        )
                    )
                }
            }

            // Customer impact: In accounting, debiting customer increases receivable (debt), crediting reduces debt
            if (line.partyType == "CUSTOMER" && line.partyId != null) {
                val customer = dao.getCustomerById(line.partyId)
                if (customer != null) {
                    val balanceDelta = line.debit - line.credit
                    dao.updateCustomer(
                        customer.copy(
                            currentBalance = customer.currentBalance + balanceDelta
                        )
                    )
                }
            }

            // Supplier impact: In accounting, crediting supplier increases payable (owed to supplier), debiting reduces payable
            if (line.partyType == "SUPPLIER" && line.partyId != null) {
                val supplier = dao.getSupplierById(line.partyId)
                if (supplier != null) {
                    val balanceDelta = line.credit - line.debit
                    dao.updateSupplier(
                        supplier.copy(
                            currentBalance = supplier.currentBalance + balanceDelta
                        )
                    )
                }
            }
        }

        logAudit(
            action = "سند قيد",
            details = "إنشاء سند قيد $voucherNumber بمبلغ $totalDebit: $narration",
            user = currentUser
        )

        return voucherId
    }

    suspend fun deleteJournalVoucher(voucher: JournalVoucher, currentUser: String) {
        val lines = dao.getJournalVoucherLinesDirect(voucher.id)
        // Reverse impacts on customers / suppliers / cash
        for (line in lines) {
            if (line.partyType == "CUSTOMER" && line.partyId != null) {
                val customer = dao.getCustomerById(line.partyId)
                if (customer != null) {
                    val balanceDelta = line.debit - line.credit
                    dao.updateCustomer(customer.copy(currentBalance = customer.currentBalance - balanceDelta))
                }
            }
            if (line.partyType == "SUPPLIER" && line.partyId != null) {
                val supplier = dao.getSupplierById(line.partyId)
                if (supplier != null) {
                    val balanceDelta = line.credit - line.debit
                    dao.updateSupplier(supplier.copy(currentBalance = supplier.currentBalance - balanceDelta))
                }
            }
            if (line.partyType == "CASH" || line.accountCode == "101" || line.accountName.contains("الصندوق")) {
                val netCashChange = line.debit - line.credit
                if (Math.abs(netCashChange) > 0.0001) {
                    val currentCash = getLatestCashBalance()
                    val reverseType = if (netCashChange > 0) "OUT" else "IN"
                    val amount = Math.abs(netCashChange)
                    dao.insertCashTransaction(
                        CashTransaction(
                            type = reverseType,
                            source = "CANCEL_JOURNAL_VOUCHER",
                            referenceId = voucher.id,
                            referenceNumber = voucher.voucherNumber,
                            amount = amount,
                            balanceAfter = if (reverseType == "IN") currentCash + amount else currentCash - amount,
                            notes = "إلغاء أثر سند قيد رقم ${voucher.voucherNumber}",
                            createdBy = currentUser
                        )
                    )
                }
            }
        }
        dao.deleteJournalVoucherLinesByVoucherId(voucher.id)
        dao.deleteJournalVoucher(voucher)
        logAudit(
            action = "حذف سند قيد",
            details = "حذف سند قيد ${voucher.voucherNumber} بقيمة ${voucher.totalDebit}",
            user = currentUser
        )
    }

    // ==========================================
    // --- Chart of Accounts (دليل الحسابات الشجري) ---
    // ==========================================
    val allChartOfAccounts: Flow<List<ChartOfAccount>> = dao.getAllChartOfAccounts()

    suspend fun seedDefaultChartOfAccountsIfNeeded() {
        val existing = dao.getAllChartOfAccountsDirect()
        if (existing.isEmpty()) {
            val defaultAccounts = listOf(
                // 1: الأصول
                ChartOfAccount(code = "1", name = "الأصول", accountType = "ASSET", parentCode = "", level = 1),
                ChartOfAccount(code = "11", name = "الأصول المتداولة", accountType = "ASSET", parentCode = "1", level = 2),
                ChartOfAccount(code = "1101", name = "الصندوق الرئيسي (النقدية)", accountType = "ASSET", parentCode = "11", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1102", name = "البنك وحساب الشبكة / مدى", accountType = "ASSET", parentCode = "11", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1103", name = "العملاء (المدينون)", accountType = "ASSET", parentCode = "11", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1104", name = "مخزون بضاعة آخر المدة", accountType = "ASSET", parentCode = "11", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1105", name = "أوراق القبض وشيكات برسم التحصيل", accountType = "ASSET", parentCode = "11", level = 3, isSubAccount = true),
                ChartOfAccount(code = "12", name = "الأصول الثابتة", accountType = "ASSET", parentCode = "1", level = 2),
                ChartOfAccount(code = "1201", name = "أجهزة ومعدات ونقاط البيع", accountType = "ASSET", parentCode = "12", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1202", name = "أثاث وديكور المتجر", accountType = "ASSET", parentCode = "12", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1203", name = "سيارات ومركبات التوزيع", accountType = "ASSET", parentCode = "12", level = 3, isSubAccount = true),
                ChartOfAccount(code = "1204", name = "مجمع إهلاك الأصول الثابتة (-)", accountType = "ASSET", parentCode = "12", level = 3, isSubAccount = true),

                // 2: الخصوم والالتزامات
                ChartOfAccount(code = "2", name = "الخصوم والالتزامات", accountType = "LIABILITY", parentCode = "", level = 1),
                ChartOfAccount(code = "21", name = "الخصوم المتداولة", accountType = "LIABILITY", parentCode = "2", level = 2),
                ChartOfAccount(code = "2101", name = "الموردون (الدائنون)", accountType = "LIABILITY", parentCode = "21", level = 3, isSubAccount = true),
                ChartOfAccount(code = "2102", name = "أوراق دفع وشيكات صادرة آجلة", accountType = "LIABILITY", parentCode = "21", level = 3, isSubAccount = true),
                ChartOfAccount(code = "2103", name = "أمانات ضريبة القيمة المضافة (VAT)", accountType = "LIABILITY", parentCode = "21", level = 3, isSubAccount = true),
                ChartOfAccount(code = "2104", name = "مصروفات مستحقة الدفع", accountType = "LIABILITY", parentCode = "21", level = 3, isSubAccount = true),

                // 3: حقوق الملكية
                ChartOfAccount(code = "3", name = "حقوق الملكية", accountType = "EQUITY", parentCode = "", level = 1),
                ChartOfAccount(code = "31", name = "رأس المال والاحتياطيات", accountType = "EQUITY", parentCode = "3", level = 2),
                ChartOfAccount(code = "3101", name = "رأس مال المشروع", accountType = "EQUITY", parentCode = "31", level = 3, isSubAccount = true),
                ChartOfAccount(code = "3102", name = "جاري المالك والمسحوبات الشخصية", accountType = "EQUITY", parentCode = "31", level = 3, isSubAccount = true),
                ChartOfAccount(code = "3103", name = "الأرباح المدورة والمرحلة", accountType = "EQUITY", parentCode = "31", level = 3, isSubAccount = true),

                // 4: الإيرادات
                ChartOfAccount(code = "4", name = "الإيرادات", accountType = "REVENUE", parentCode = "", level = 1),
                ChartOfAccount(code = "4101", name = "إيرادات المبيعات", accountType = "REVENUE", parentCode = "4", level = 2, isSubAccount = true),
                ChartOfAccount(code = "4102", name = "مردودات ومسموحات المبيعات (-)", accountType = "REVENUE", parentCode = "4", level = 2, isSubAccount = true),
                ChartOfAccount(code = "4103", name = "خصم مكتسب من الموردين", accountType = "REVENUE", parentCode = "4", level = 2, isSubAccount = true),
                ChartOfAccount(code = "4104", name = "إيرادات أخرى وفروق عملة", accountType = "REVENUE", parentCode = "4", level = 2, isSubAccount = true),

                // 5: المصروفات والتكاليف
                ChartOfAccount(code = "5", name = "المصروفات والتكاليف", accountType = "EXPENSE", parentCode = "", level = 1),
                ChartOfAccount(code = "5101", name = "تكلفة البضاعة المباعة (المشتريات)", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5102", name = "خصم مسموح به للعملاء", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5103", name = "إيجار المحل والمستودعات", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5104", name = "رواتب ومستحقات الموظفين", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5105", name = "كهرباء ومياه وخدمات إنترنت", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5106", name = "مصروفات صيانة وتشغيل", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5107", name = "إهلاك الأصول الثابتة", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true),
                ChartOfAccount(code = "5108", name = "مصروفات حكومية وتراخيص ورسوم", accountType = "EXPENSE", parentCode = "5", level = 2, isSubAccount = true)
            )
            dao.insertChartOfAccounts(defaultAccounts)
        }
    }

    suspend fun insertChartOfAccount(account: ChartOfAccount): Long = dao.insertChartOfAccount(account)
    suspend fun updateChartOfAccount(account: ChartOfAccount) = dao.updateChartOfAccount(account)
    suspend fun deleteChartOfAccount(account: ChartOfAccount) = dao.deleteChartOfAccount(account)

    // ==========================================
    // --- Cost Centers (مراكز التكلفة) ---
    // ==========================================
    val allCostCenters: Flow<List<CostCenter>> = dao.getAllCostCenters()
    suspend fun insertCostCenter(costCenter: CostCenter): Long = dao.insertCostCenter(costCenter)
    suspend fun updateCostCenter(costCenter: CostCenter) = dao.updateCostCenter(costCenter)
    suspend fun deleteCostCenter(costCenter: CostCenter) = dao.deleteCostCenter(costCenter)

    // ==========================================
    // --- Fixed Assets & Depreciation (الأصول الثابتة والإهلاك) ---
    // ==========================================
    val allFixedAssets: Flow<List<FixedAsset>> = dao.getAllFixedAssets()
    suspend fun insertFixedAsset(asset: FixedAsset): Long {
        val depreciableBase = Math.max(0.0, asset.purchasePrice - asset.salvageValue)
        val months = Math.max(1, asset.usefulLifeYears * 12)
        val monthlyDep = depreciableBase / months
        val calculated = asset.copy(
            monthlyDepreciation = monthlyDep,
            bookValue = Math.max(asset.salvageValue, asset.purchasePrice - asset.accumulatedDepreciation)
        )
        return dao.insertFixedAsset(calculated)
    }
    suspend fun updateFixedAsset(asset: FixedAsset) = dao.updateFixedAsset(asset)
    suspend fun deleteFixedAsset(asset: FixedAsset) = dao.deleteFixedAsset(asset)

    suspend fun executeMonthlyDepreciation(asset: FixedAsset, currentUser: String): Boolean {
        val newAccumulated = asset.accumulatedDepreciation + asset.monthlyDepreciation
        val newBookValue = Math.max(asset.salvageValue, asset.purchasePrice - newAccumulated)
        val updated = asset.copy(accumulatedDepreciation = newAccumulated, bookValue = newBookValue)
        dao.updateFixedAsset(updated)

        // Create Journal Voucher for Depreciation
        val voucherNum = "JV-DEP-" + (System.currentTimeMillis() % 100000)
        val vId = dao.insertJournalVoucher(
            JournalVoucher(
                voucherNumber = voucherNum,
                narration = "إهلاك شهري للأصل: ${asset.name}",
                totalDebit = asset.monthlyDepreciation,
                totalCredit = asset.monthlyDepreciation,
                isBalanced = true,
                createdBy = currentUser
            )
        )
        dao.insertJournalVoucherLines(
            listOf(
                JournalVoucherLine(voucherId = vId, accountCode = "5107", accountName = "مصروف إهلاك أصول ثابتة", partyType = "EXPENSE", debit = asset.monthlyDepreciation, description = "إهلاك شهري ${asset.name}"),
                JournalVoucherLine(voucherId = vId, accountCode = "1204", accountName = "مجمع إهلاك ${asset.name}", partyType = "ASSET", credit = asset.monthlyDepreciation, description = "مجمع إهلاك ${asset.name}")
            )
        )
        logAudit("إهلاك أصل", "تسجيل إهلاك شهري للأصل ${asset.name} بقيمة ${asset.monthlyDepreciation}", currentUser)
        return true
    }

    // ==========================================
    // --- Cheques / PDC (أوراق القبض والدفع) ---
    // ==========================================
    val allCheques: Flow<List<Cheque>> = dao.getAllCheques()
    suspend fun insertCheque(cheque: Cheque): Long = dao.insertCheque(cheque)
    suspend fun updateCheque(cheque: Cheque) = dao.updateCheque(cheque)
    suspend fun deleteCheque(cheque: Cheque) = dao.deleteCheque(cheque)

    suspend fun updateChequeStatus(cheque: Cheque, newStatus: String, currentUser: String) {
        val oldStatus = cheque.status
        dao.updateCheque(cheque.copy(status = newStatus))
        if (newStatus == "COLLECTED" && oldStatus != "COLLECTED") {
            // When cheque is collected:
            if (cheque.chequeType == "RECEIVABLE") {
                // Cash In
                val currentCash = getLatestCashBalance()
                dao.insertCashTransaction(
                    CashTransaction(
                        type = "IN",
                        source = "CHEQUE_COLLECTED",
                        referenceId = cheque.id,
                        referenceNumber = cheque.chequeNumber,
                        amount = cheque.amount,
                        balanceAfter = currentCash + cheque.amount,
                        notes = "تحصيل شيك ورقة قبض رقم ${cheque.chequeNumber} من ${cheque.partyName}",
                        createdBy = currentUser
                    )
                )
            } else {
                // Payable Cheque paid
                val currentCash = getLatestCashBalance()
                dao.insertCashTransaction(
                    CashTransaction(
                        type = "OUT",
                        source = "CHEQUE_PAID",
                        referenceId = cheque.id,
                        referenceNumber = cheque.chequeNumber,
                        amount = cheque.amount,
                        balanceAfter = currentCash - cheque.amount,
                        notes = "صرف شيك ورقة دفع رقم ${cheque.chequeNumber} لـ ${cheque.partyName}",
                        createdBy = currentUser
                    )
                )
            }
        }
        logAudit("تحديث شيك", "تغيير حالة الشيك ${cheque.chequeNumber} إلى $newStatus", currentUser)
    }

    // ==========================================
    // --- Inter-Account Transfers (التحويل بين الصناديق والبنوك) ---
    // ==========================================
    val allAccountTransfers: Flow<List<AccountTransfer>> = dao.getAllAccountTransfers()

    suspend fun performAccountTransfer(
        fromAccount: String,
        toAccount: String,
        amount: Double,
        fee: Double,
        notes: String,
        currentUser: String
    ): Long {
        val transferNumber = "TRF-" + (System.currentTimeMillis() % 1000000)
        val transferId = dao.insertAccountTransfer(
            AccountTransfer(
                transferNumber = transferNumber,
                fromAccount = fromAccount,
                toAccount = toAccount,
                amount = amount,
                transferFee = fee,
                notes = notes,
                createdBy = currentUser
            )
        )

        // Cash effects
        val totalOut = amount + fee
        val currentCash = getLatestCashBalance()
        if (fromAccount.contains("الصندوق")) {
            dao.insertCashTransaction(
                CashTransaction(
                    type = "OUT",
                    source = "ACCOUNT_TRANSFER",
                    referenceId = transferId,
                    referenceNumber = transferNumber,
                    amount = totalOut,
                    balanceAfter = currentCash - totalOut,
                    notes = "تحويل من $fromAccount إلى $toAccount (رسوم: $fee)",
                    createdBy = currentUser
                )
            )
        }
        if (toAccount.contains("الصندوق")) {
            val updatedCash = getLatestCashBalance()
            dao.insertCashTransaction(
                CashTransaction(
                    type = "IN",
                    source = "ACCOUNT_TRANSFER",
                    referenceId = transferId,
                    referenceNumber = transferNumber,
                    amount = amount,
                    balanceAfter = updatedCash + amount,
                    notes = "استلام تحويل من $fromAccount إلى $toAccount",
                    createdBy = currentUser
                )
            )
        }

        // Generate matching Journal Voucher
        val vId = dao.insertJournalVoucher(
            JournalVoucher(
                voucherNumber = "JV-$transferNumber",
                narration = "تحويل مالي من $fromAccount إلى $toAccount: $notes",
                totalDebit = totalOut,
                totalCredit = totalOut,
                isBalanced = true,
                createdBy = currentUser
            )
        )
        val lines = mutableListOf(
            JournalVoucherLine(voucherId = vId, accountCode = "1102", accountName = toAccount, partyType = "BANK", debit = amount, description = "استلام تحويل"),
            JournalVoucherLine(voucherId = vId, accountCode = "1101", accountName = fromAccount, partyType = "CASH", credit = amount, description = "صرف تحويل")
        )
        if (fee > 0) {
            lines.add(JournalVoucherLine(voucherId = vId, accountCode = "5106", accountName = "عمولات ورسوم بنكية", partyType = "EXPENSE", debit = fee, description = "رسوم تحويل"))
            lines.add(JournalVoucherLine(voucherId = vId, accountCode = "1101", accountName = fromAccount, partyType = "CASH", credit = fee, description = "صرف رسوم التحويل"))
        }
        dao.insertJournalVoucherLines(lines)
        logAudit("تحويل مالي", "تحويل مبلغ $amount من $fromAccount إلى $toAccount", currentUser)
        return transferId
    }

    // ==========================================
    // --- Shift Settlement & Denominations Counter ---
    // ==========================================
    val allShifts: Flow<List<ShiftRecord>> = dao.getAllShifts()
    suspend fun getCurrentOpenShift(): ShiftRecord? = dao.getCurrentOpenShift()

    suspend fun openShift(userName: String, openingCash: Double): Long {
        val shiftNum = "SH-" + (System.currentTimeMillis() % 1000000)
        val shift = ShiftRecord(
            shiftNumber = shiftNum,
            openedBy = userName,
            openingCash = openingCash,
            expectedCash = openingCash,
            status = "OPEN"
        )
        val id = dao.insertShift(shift)
        logAudit("فتح وردية", "فتح وردية جديدة رقم $shiftNum بعهدة افتتاحية $openingCash", userName)
        return id
    }

    suspend fun closeShift(
        shift: ShiftRecord,
        counts: Map<Int, Int>,
        actualCash: Double,
        notes: String,
        currentUser: String
    ): ShiftRecord {
        val currentCash = getLatestCashBalance()
        val expected = shift.openingCash + currentCash
        val variance = actualCash - expected
        val updated = shift.copy(
            closedBy = currentUser,
            endTime = System.currentTimeMillis(),
            expectedCash = expected,
            actualCash = actualCash,
            variance = variance,
            count500 = counts[500] ?: 0,
            count200 = counts[200] ?: 0,
            count100 = counts[100] ?: 0,
            count50 = counts[50] ?: 0,
            count20 = counts[20] ?: 0,
            count10 = counts[10] ?: 0,
            count5 = counts[5] ?: 0,
            count1 = counts[1] ?: 0,
            status = "CLOSED",
            notes = notes
        )
        dao.updateShift(updated)
        logAudit("إقفال وردية", "إقفال الوردية ${shift.shiftNumber} - الفارق: $variance", currentUser)
        return updated
    }

    // ==========================================
    // --- Installments (أقساط الفواتير) ---
    // ==========================================
    val allInstallments: Flow<List<InvoiceInstallment>> = dao.getAllInstallments()
    fun getInstallmentsForInvoice(invoiceId: Long): Flow<List<InvoiceInstallment>> = dao.getInstallmentsForInvoice(invoiceId)

    suspend fun generateInstallmentsForInvoice(
        invoice: Invoice,
        installmentsCount: Int,
        firstDueDate: Long,
        intervalDays: Int = 30
    ) {
        val count = Math.max(1, installmentsCount)
        val eachAmount = invoice.totalAmount / count
        val list = mutableListOf<InvoiceInstallment>()
        for (i in 1..count) {
            val dueDate = firstDueDate + ((i - 1) * intervalDays * 24L * 60L * 60L * 1000L)
            list.add(
                InvoiceInstallment(
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    customerId = invoice.partyId,
                    customerName = invoice.partyName,
                    installmentNumber = i,
                    dueDate = dueDate,
                    amount = eachAmount,
                    paidAmount = 0.0,
                    status = "UNPAID"
                )
            )
        }
        dao.insertInstallments(list)
    }

    suspend fun payInstallment(installment: InvoiceInstallment, paidAmount: Double, currentUser: String) {
        val totalPaid = installment.paidAmount + paidAmount
        val newStatus = if (totalPaid >= installment.amount - 0.01) "PAID" else "PARTIAL"
        val updated = installment.copy(
            paidAmount = totalPaid,
            status = newStatus,
            paidDate = System.currentTimeMillis()
        )
        dao.updateInstallment(updated)

        // Customer balance reduction
        if (installment.customerId != null) {
            val customer = dao.getCustomerById(installment.customerId)
            if (customer != null) {
                dao.updateCustomer(customer.copy(
                    currentBalance = customer.currentBalance - paidAmount,
                    totalPaid = customer.totalPaid + paidAmount
                ))
            }
        }

        // Cash In
        val currentCash = getLatestCashBalance()
        dao.insertCashTransaction(
            CashTransaction(
                type = "IN",
                source = "INSTALLMENT_PAYMENT",
                referenceId = installment.id,
                referenceNumber = installment.invoiceNumber,
                amount = paidAmount,
                balanceAfter = currentCash + paidAmount,
                notes = "سداد قسط رقم ${installment.installmentNumber} للفاتورة ${installment.invoiceNumber}",
                createdBy = currentUser
            )
        )
        logAudit("سداد قسط", "سداد قسط ${installment.installmentNumber} بقيمة $paidAmount للعميل ${installment.customerName}", currentUser)
    }

    // ==========================================
    // --- Purchase Orders (أوامر الشراء) ---
    // ==========================================
    val allPurchaseOrders: Flow<List<PurchaseOrder>> = dao.getAllPurchaseOrders()
    suspend fun insertPurchaseOrder(order: PurchaseOrder): Long = dao.insertPurchaseOrder(order)
    suspend fun updatePurchaseOrder(order: PurchaseOrder) = dao.updatePurchaseOrder(order)
    suspend fun deletePurchaseOrder(order: PurchaseOrder) = dao.deletePurchaseOrder(order)
}

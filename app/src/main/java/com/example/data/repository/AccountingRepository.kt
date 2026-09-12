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
            partyId = customer?.id,
            partyName = customer?.name ?: "عميل نقدي",
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            paidAmount = actualPaid,
            remainingAmount = remainingAmount,
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
            dao.insertCashTransaction(
                CashTransaction(
                    type = "IN",
                    source = "SALE",
                    referenceId = invoiceId,
                    referenceNumber = invoiceNumber,
                    amount = actualPaid,
                    balanceAfter = currentCash + actualPaid,
                    notes = "تحصيل مبيعات فاتورة $invoiceNumber",
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
            partyId = supplier?.id,
            partyName = supplier?.name ?: "مورد نقدي",
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            paidAmount = actualPaid,
            remainingAmount = remainingAmount,
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
}

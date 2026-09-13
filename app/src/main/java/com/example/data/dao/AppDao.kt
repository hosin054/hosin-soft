package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Settings ---
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<StoreSettings?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): StoreSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: StoreSettings)

    // --- Users ---
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE currentStockSubUnits <= minStockSubUnits AND isActive = 1")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE currentStockSubUnits <= 0 AND isActive = 1")
    fun getOutOfStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>): List<Long>

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE currentBalance > 0 ORDER BY currentBalance DESC")
    fun getDebtorCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>): List<Long>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // --- Suppliers ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): Supplier?

    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchSuppliers(query: String): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE currentBalance > 0 ORDER BY currentBalance DESC")
    fun getCreditorSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>): List<Long>

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    // --- Invoices ---
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type ORDER BY createdAt DESC")
    fun getInvoicesByType(type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE partyId = :partyId AND invoiceType = :type ORDER BY createdAt DESC")
    fun getInvoicesByParty(partyId: Long, type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    // --- Invoice Items ---
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItemsDirect(invoiceId: Long): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items ORDER BY id DESC")
    fun getAllInvoiceItems(): Flow<List<InvoiceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    // --- Vouchers (سند قبض وسند صرف) ---
    @Query("SELECT * FROM vouchers ORDER BY createdAt DESC")
    fun getAllVouchers(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE type = :type ORDER BY createdAt DESC")
    fun getVouchersByType(type: String): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE partyId = :partyId ORDER BY createdAt DESC")
    fun getVouchersByParty(partyId: Long): Flow<List<Voucher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: Voucher): Long

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // --- Cash Transactions ---
    @Query("SELECT * FROM cash_transactions ORDER BY createdAt DESC")
    fun getAllCashTransactions(): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions ORDER BY id DESC LIMIT 1")
    suspend fun getLatestCashTransaction(): CashTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashTransaction(transaction: CashTransaction): Long

    // --- Inventory Transactions ---
    @Query("SELECT * FROM inventory_transactions ORDER BY createdAt DESC")
    fun getAllInventoryTransactions(): Flow<List<InventoryTransaction>>

    @Query("SELECT * FROM inventory_transactions WHERE productId = :productId ORDER BY createdAt DESC")
    fun getProductInventoryTransactions(productId: Long): Flow<List<InventoryTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryTransaction(transaction: InventoryTransaction): Long

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    // --- Currency Rates ---
    @Query("SELECT * FROM currency_rates ORDER BY isBase DESC, id ASC")
    fun getAllCurrencyRates(): Flow<List<CurrencyRate>>

    @Query("SELECT * FROM currency_rates")
    suspend fun getAllCurrencyRatesDirect(): List<CurrencyRate>

    @Query("SELECT * FROM currency_rates WHERE isBase = 1 LIMIT 1")
    suspend fun getBaseCurrency(): CurrencyRate?

    @Query("SELECT * FROM currency_rates WHERE code = :code LIMIT 1")
    suspend fun getCurrencyByCode(code: String): CurrencyRate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencyRate(currencyRate: CurrencyRate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencyRates(currencyRates: List<CurrencyRate>)

    @Update
    suspend fun updateCurrencyRate(currencyRate: CurrencyRate)

    @Delete
    suspend fun deleteCurrencyRate(currencyRate: CurrencyRate)

    // --- Journal Vouchers (سندات القيد) ---
    @Query("SELECT * FROM journal_vouchers ORDER BY voucherDate DESC, id DESC")
    fun getAllJournalVouchers(): Flow<List<JournalVoucher>>

    @Query("SELECT * FROM journal_vouchers WHERE id = :id LIMIT 1")
    suspend fun getJournalVoucherById(id: Long): JournalVoucher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalVoucher(voucher: JournalVoucher): Long

    @Delete
    suspend fun deleteJournalVoucher(voucher: JournalVoucher)

    // --- Journal Voucher Lines ---
    @Query("SELECT * FROM journal_voucher_lines WHERE voucherId = :voucherId ORDER BY id ASC")
    fun getJournalVoucherLines(voucherId: Long): Flow<List<JournalVoucherLine>>

    @Query("SELECT * FROM journal_voucher_lines WHERE voucherId = :voucherId ORDER BY id ASC")
    suspend fun getJournalVoucherLinesDirect(voucherId: Long): List<JournalVoucherLine>

    @Query("SELECT * FROM journal_voucher_lines ORDER BY id ASC")
    fun getAllJournalVoucherLines(): Flow<List<JournalVoucherLine>>

    @Query("SELECT * FROM journal_voucher_lines ORDER BY id ASC")
    suspend fun getAllJournalVoucherLinesDirect(): List<JournalVoucherLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalVoucherLines(lines: List<JournalVoucherLine>)

    @Query("DELETE FROM journal_voucher_lines WHERE voucherId = :voucherId")
    suspend fun deleteJournalVoucherLinesByVoucherId(voucherId: Long)

    // --- Chart of Accounts (دليل الحسابات الشجري) ---
    @Query("SELECT * FROM chart_of_accounts ORDER BY code ASC")
    fun getAllChartOfAccounts(): Flow<List<ChartOfAccount>>

    @Query("SELECT * FROM chart_of_accounts ORDER BY code ASC")
    suspend fun getAllChartOfAccountsDirect(): List<ChartOfAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChartOfAccount(account: ChartOfAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChartOfAccounts(accounts: List<ChartOfAccount>)

    @Update
    suspend fun updateChartOfAccount(account: ChartOfAccount)

    @Delete
    suspend fun deleteChartOfAccount(account: ChartOfAccount)

    // --- Cost Centers (مراكز التكلفة) ---
    @Query("SELECT * FROM cost_centers ORDER BY code ASC")
    fun getAllCostCenters(): Flow<List<CostCenter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostCenter(costCenter: CostCenter): Long

    @Update
    suspend fun updateCostCenter(costCenter: CostCenter)

    @Delete
    suspend fun deleteCostCenter(costCenter: CostCenter)

    // --- Fixed Assets (الأصول الثابتة والإهلاك) ---
    @Query("SELECT * FROM fixed_assets ORDER BY purchaseDate DESC")
    fun getAllFixedAssets(): Flow<List<FixedAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedAsset(asset: FixedAsset): Long

    @Update
    suspend fun updateFixedAsset(asset: FixedAsset)

    @Delete
    suspend fun deleteFixedAsset(asset: FixedAsset)

    // --- Cheques / PDC (أوراق القبض والدفع والشيكات) ---
    @Query("SELECT * FROM cheques ORDER BY dueDate ASC")
    fun getAllCheques(): Flow<List<Cheque>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheque(cheque: Cheque): Long

    @Update
    suspend fun updateCheque(cheque: Cheque)

    @Delete
    suspend fun deleteCheque(cheque: Cheque)

    // --- Account Transfers (التحويل بين الصناديق والبنوك) ---
    @Query("SELECT * FROM account_transfers ORDER BY transferDate DESC")
    fun getAllAccountTransfers(): Flow<List<AccountTransfer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountTransfer(transfer: AccountTransfer): Long

    // --- Shift Records (إقفال الوردية وعد النقدية) ---
    @Query("SELECT * FROM shift_records ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<ShiftRecord>>

    @Query("SELECT * FROM shift_records WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentOpenShift(): ShiftRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftRecord): Long

    @Update
    suspend fun updateShift(shift: ShiftRecord)

    // --- Invoice Installments (أقساط الفواتير) ---
    @Query("SELECT * FROM invoice_installments ORDER BY dueDate ASC")
    fun getAllInstallments(): Flow<List<InvoiceInstallment>>

    @Query("SELECT * FROM invoice_installments WHERE invoiceId = :invoiceId ORDER BY installmentNumber ASC")
    fun getInstallmentsForInvoice(invoiceId: Long): Flow<List<InvoiceInstallment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InvoiceInstallment>)

    @Update
    suspend fun updateInstallment(installment: InvoiceInstallment)

    // --- Purchase Orders (أوامر الشراء) ---
    @Query("SELECT * FROM purchase_orders ORDER BY orderDate DESC")
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrder(order: PurchaseOrder): Long

    @Update
    suspend fun updatePurchaseOrder(order: PurchaseOrder)

    @Delete
    suspend fun deletePurchaseOrder(order: PurchaseOrder)
}

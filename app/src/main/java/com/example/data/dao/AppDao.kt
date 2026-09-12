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
}

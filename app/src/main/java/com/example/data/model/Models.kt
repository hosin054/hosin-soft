package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "متجر حسين",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val currencyName: String = "ريال",
    val currencySymbol: String = "ر.س",
    val decimalPlaces: Int = 2,
    val taxEnabled: Boolean = false,
    val taxRate: Double = 15.0,
    val securityPin: String = "",
    val isConfigured: Boolean = false
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val role: String = "ADMIN", // ADMIN, ACCOUNTANT, CASHIER
    val canSell: Boolean = true,
    val canPurchase: Boolean = true,
    val canViewProfits: Boolean = true,
    val canViewReports: Boolean = true,
    val canManageInventory: Boolean = true,
    val canManageSettings: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = ""
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String = "",
    val barcode: String = "",
    val name: String,
    val description: String = "",
    val category: String = "عام",
    val brand: String = "",
    val mainUnit: String = "كرتون",
    val subUnit: String = "حبة",
    val conversionFactor: Double = 1.0, // 1 mainUnit = conversionFactor subUnits
    val purchasePrice: Double = 0.0, // Cost per main unit
    val cashSalePrice: Double = 0.0, // Cash sale price per main unit
    val creditSalePrice: Double = 0.0, // Credit sale price per main unit
    val currentStockSubUnits: Double = 0.0, // Stored in subUnits for exact precision
    val minStockSubUnits: Double = 5.0,
    val supplierId: Long? = null,
    val supplierName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val previousBalance: Double = 0.0,
    val currentBalance: Double = 0.0, // Positive: Customer owes store
    val totalSales: Double = 0.0,
    val totalPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val currentBalance: Double = 0.0, // Positive: Store owes supplier
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "currency_rates")
data class CurrencyRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // SAR, USD, YER, AED, etc.
    val name: String, // ريال سعودي, دولار أمريكي, ريال يمني, إلخ
    val symbol: String, // ر.س, $, ر.ي, إلخ
    val rateToBase: Double = 1.0, // كم وحدة من العملة الأساسية تساوي 1 وحدة من هذه العملة
    val isBase: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String, // SALE, PURCHASE
    val paymentType: String, // CASH, CREDIT
    val paymentMethod: String = "كاش", // كاش / نقداً, إلكتروني / شبكة, تحويل بنكي, آجل
    val partyId: Long? = null, // customerId or supplierId
    val partyName: String = "",
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paidCurrency: String = "العملة الأساسية",
    val paidCurrencyAmount: Double = 0.0,
    val exchangeRate: Double = 1.0,
    val totalCost: Double = 0.0,
    val profit: Double = 0.0, // (totalAmount - taxAmount) - totalCost
    val dueDate: Long? = null,
    val notes: String = "",
    val createdBy: String = "مدير النظام",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED" // COMPLETED, CANCELLED
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val isMainUnit: Boolean = true,
    val quantity: Double,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val totalCost: Double
)

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNumber: String,
    val type: String, // RECEIPT (سند قبض), PAYMENT (سند صرف)
    val partyType: String, // CUSTOMER, SUPPLIER, GENERAL
    val partyId: Long? = null,
    val partyName: String,
    val amount: Double,
    val paymentMethod: String = "نقداً", // نقداً, شيك, تحويل بنكي
    val description: String = "",
    val notes: String = "",
    val createdBy: String = "مدير النظام",
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    val voucherType: String = type
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // إيجار, رواتب, كهرباء, نقل, صيانة, أخرى
    val amount: Double,
    val paymentMethod: String = "نقداً",
    val description: String = "",
    val notes: String = "",
    val createdBy: String = "مدير النظام",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_transactions")
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // IN, OUT
    val source: String, // SALE, PURCHASE, RECEIPT, PAYMENT, EXPENSE, OPENING, ADJUSTMENT
    val referenceId: Long? = null,
    val referenceNumber: String = "",
    val amount: Double,
    val balanceAfter: Double = 0.0,
    val notes: String = "",
    val createdBy: String = "مدير النظام",
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    val transactionType: String = type
    @Ignore
    val description: String = notes
}

@Entity(tableName = "inventory_transactions")
data class InventoryTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val transactionType: String, // SALE, PURCHASE, IN, OUT, ADJUSTMENT
    val quantitySubUnits: Double, // positive or negative
    val balanceAfterSubUnits: Double,
    val unitName: String,
    val referenceNumber: String = "",
    val notes: String = "",
    val createdBy: String = "مدير النظام",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val details: String,
    val userName: String = "مدير النظام",
    val timestamp: Long = System.currentTimeMillis()
) {
    @Ignore
    val actionType: String = action
    @Ignore
    val performedBy: String = userName
}

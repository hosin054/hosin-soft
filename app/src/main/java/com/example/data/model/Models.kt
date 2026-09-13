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
    val passwordHash: String = "", // PIN or password
    val fullName: String,
    val role: String = "ADMIN", // ADMIN, CASHIER, ACCOUNTANT, INVENTORY_MANAGER, CUSTOM
    val canSell: Boolean = true,
    val canPurchase: Boolean = true,
    val canViewProfits: Boolean = true,
    val canViewReports: Boolean = true,
    val canManageInventory: Boolean = true,
    val canManageSettings: Boolean = true,
    val canManageCustomers: Boolean = true,
    val canManageSuppliers: Boolean = true,
    val canManageExpenses: Boolean = true,
    val canGiveDiscount: Boolean = true,
    val canManageUsers: Boolean = true,
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
    val wholesalePrice: Double = 0.0, // Wholesale price per main unit
    val distributorPrice: Double = 0.0, // Distributor special price
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

@Entity(tableName = "journal_vouchers")
data class JournalVoucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNumber: String,
    val voucherDate: Long = System.currentTimeMillis(),
    val reference: String = "",
    val narration: String = "",
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val isBalanced: Boolean = true,
    val status: String = "POSTED", // POSTED, DRAFT, CANCELLED
    val createdBy: String = "مدير النظام",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_voucher_lines")
data class JournalVoucherLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherId: Long,
    val accountCode: String = "",
    val accountName: String,
    val partyType: String = "GENERAL", // CUSTOMER, SUPPLIER, CASH, BANK, CAPITAL, EXPENSE, REVENUE, GENERAL
    val partyId: Long? = null,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val description: String = ""
)

@Entity(tableName = "chart_of_accounts")
data class ChartOfAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g. "1", "11", "1101"
    val name: String,
    val accountType: String, // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    val parentCode: String = "",
    val level: Int = 1,
    val isSubAccount: Boolean = false,
    val debitBalance: Double = 0.0,
    val creditBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "cost_centers")
data class CostCenter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val description: String = "",
    val totalExpenses: Double = 0.0,
    val totalRevenues: Double = 0.0,
    val isActive: Boolean = true
)

@Entity(tableName = "fixed_assets")
data class FixedAsset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String = "",
    val name: String,
    val category: String = "أجهزة ومعدات",
    val purchaseDate: Long = System.currentTimeMillis(),
    val purchasePrice: Double,
    val salvageValue: Double = 0.0, // قيمة الخردة
    val usefulLifeYears: Int = 5,
    val accumulatedDepreciation: Double = 0.0, // مجمع الإهلاك
    val bookValue: Double = purchasePrice, // القيمة الدفترية
    val monthlyDepreciation: Double = 0.0,
    val location: String = "المتجر الرئيسي",
    val notes: String = ""
)

@Entity(tableName = "cheques")
data class Cheque(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chequeNumber: String,
    val chequeType: String, // RECEIVABLE (ورقة قبض), PAYABLE (ورقة دفع)
    val partyType: String = "CUSTOMER", // CUSTOMER, SUPPLIER, OTHER
    val partyId: Long? = null,
    val partyName: String,
    val bankName: String,
    val amount: Double,
    val dueDate: Long,
    val issueDate: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING (في الحافظة), COLLECTED (محصل), BOUNCED (مرتد), CANCELLED (ملغي)
    val notes: String = ""
)

@Entity(tableName = "account_transfers")
data class AccountTransfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transferNumber: String,
    val fromAccount: String, // الصندوق الرئيسي, البنك الأهلي, إلخ
    val toAccount: String,
    val amount: Double,
    val transferFee: Double = 0.0,
    val transferDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdBy: String = "مدير النظام"
)

@Entity(tableName = "shift_records")
data class ShiftRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftNumber: String,
    val openedBy: String,
    val closedBy: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val openingCash: Double = 0.0,
    val systemSalesCash: Double = 0.0,
    val systemPaymentsCash: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val variance: Double = 0.0, // الفارق (عجز أو زيادة)
    val count500: Int = 0,
    val count200: Int = 0,
    val count100: Int = 0,
    val count50: Int = 0,
    val count20: Int = 0,
    val count10: Int = 0,
    val count5: Int = 0,
    val count1: Int = 0,
    val status: String = "OPEN", // OPEN, CLOSED
    val notes: String = ""
)

@Entity(tableName = "invoice_installments")
data class InvoiceInstallment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val invoiceNumber: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val installmentNumber: Int,
    val dueDate: Long,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val status: String = "UNPAID", // UNPAID, PARTIAL, PAID
    val paidDate: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "purchase_orders")
data class PurchaseOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val supplierId: Long,
    val supplierName: String,
    val orderDate: Long = System.currentTimeMillis(),
    val expectedDeliveryDate: Long? = null,
    val totalAmount: Double = 0.0,
    val status: String = "PENDING", // PENDING, APPROVED, RECEIVED, CANCELLED
    val notes: String = ""
)

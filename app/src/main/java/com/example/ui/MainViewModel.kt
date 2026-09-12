package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AccountingRepository(db.appDao())

    // App Settings
    val settings: StateFlow<StoreSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCurrencyRates: StateFlow<List<CurrencyRate>> = repository.allCurrencyRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultCurrenciesIfEmpty()
        }
    }

    // Current User
    private val _currentUser = MutableStateFlow(
        User(
            id = 1,
            username = "admin",
            passwordHash = "",
            fullName = "المدير العام",
            role = "ADMIN"
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    // PIN lock state
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlockApp(pin: String): Boolean {
        val currentPin = settings.value?.securityPin ?: ""
        if (currentPin.isBlank() || currentPin == pin) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    // Products
    val allProducts = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outOfStockProducts = repository.outOfStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customers & Suppliers
    val allCustomers = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoices & Items
    val allInvoices = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoiceItems = repository.allInvoiceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vouchers & Expenses
    val allVouchers = repository.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cash & Inventory
    val allCashTransactions = repository.allCashTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInventoryTransactions = repository.allInventoryTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit Logs
    val allAuditLogs = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Users
    val allUsers = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- POS Cart State ---
    private val _cartItems = MutableStateFlow<List<AccountingRepository.CartItem>>(emptyList())
    val cartItems: StateFlow<List<AccountingRepository.CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _posDiscount = MutableStateFlow(0.0)
    val posDiscount: StateFlow<Double> = _posDiscount.asStateFlow()

    private val _posTax = MutableStateFlow(0.0)
    val posTax: StateFlow<Double> = _posTax.asStateFlow()

    // UI feedback / messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Cart operations
    fun addToCart(product: Product, isMainUnit: Boolean = true, quantity: Double = 1.0) {
        val existingIndex = _cartItems.value.indexOfFirst {
            it.product.id == product.id && it.isMainUnit == isMainUnit
        }
        val unitPrice = if (isMainUnit) product.cashSalePrice else {
            if (product.conversionFactor > 0) product.cashSalePrice / product.conversionFactor else product.cashSalePrice
        }
        val unitCost = if (isMainUnit) product.purchasePrice else {
            if (product.conversionFactor > 0) product.purchasePrice / product.conversionFactor else product.purchasePrice
        }

        val updated = _cartItems.value.toMutableList()
        if (existingIndex >= 0) {
            val current = updated[existingIndex]
            updated[existingIndex] = current.copy(quantity = current.quantity + quantity)
        } else {
            updated.add(
                AccountingRepository.CartItem(
                    product = product,
                    isMainUnit = isMainUnit,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    unitCost = unitCost
                )
            )
        }
        _cartItems.value = updated
    }

    fun updateCartItemQuantity(index: Int, newQty: Double) {
        if (index in _cartItems.value.indices) {
            val updated = _cartItems.value.toMutableList()
            if (newQty <= 0) {
                updated.removeAt(index)
            } else {
                updated[index] = updated[index].copy(quantity = newQty)
            }
            _cartItems.value = updated
        }
    }

    fun updateCartItemPrice(index: Int, newPrice: Double) {
        if (index in _cartItems.value.indices && newPrice >= 0) {
            val updated = _cartItems.value.toMutableList()
            updated[index] = updated[index].copy(unitPrice = newPrice)
            _cartItems.value = updated
        }
    }

    fun toggleCartItemUnit(index: Int) {
        if (index in _cartItems.value.indices) {
            val item = _cartItems.value[index]
            val newIsMain = !item.isMainUnit
            val newPrice = if (newIsMain) item.product.cashSalePrice else {
                if (item.product.conversionFactor > 0) item.product.cashSalePrice / item.product.conversionFactor else item.product.cashSalePrice
            }
            val newCost = if (newIsMain) item.product.purchasePrice else {
                if (item.product.conversionFactor > 0) item.product.purchasePrice / item.product.conversionFactor else item.product.purchasePrice
            }
            val updated = _cartItems.value.toMutableList()
            updated[index] = item.copy(isMainUnit = newIsMain, unitPrice = newPrice, unitCost = newCost)
            _cartItems.value = updated
        }
    }

    fun removeFromCart(index: Int) {
        if (index in _cartItems.value.indices) {
            val updated = _cartItems.value.toMutableList()
            updated.removeAt(index)
            _cartItems.value = updated
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _posDiscount.value = 0.0
        _posTax.value = 0.0
        _selectedCustomer.value = null
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setPosDiscount(discount: Double) {
        _posDiscount.value = maxOf(0.0, discount)
    }

    fun setPosTax(tax: Double) {
        _posTax.value = maxOf(0.0, tax)
    }

    fun completeSale(
        paymentType: String,
        paymentMethod: String = "كاش",
        paidCurrency: String = "العملة الأساسية",
        paidCurrencyAmount: Double = 0.0,
        exchangeRate: Double = 1.0,
        paidAmount: Double,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            if (_cartItems.value.isEmpty()) {
                showMessage("السلة فارغة، يرجى إضافة منتجات للبيع")
                return@launch
            }
            // Check credit limit if credit sale
            val customer = _selectedCustomer.value
            val subtotal = _cartItems.value.sumOf { it.total }
            val total = maxOf(0.0, subtotal - _posDiscount.value + _posTax.value)
            val remaining = if (paymentType == "CASH") 0.0 else maxOf(0.0, total - paidAmount)

            if (paymentType == "CREDIT") {
                if (customer == null) {
                    showMessage("يجب اختيار عميل عند البيع الآجل")
                    return@launch
                }
                if (customer.creditLimit > 0 && (customer.currentBalance + remaining) > customer.creditLimit) {
                    showMessage("تجاوز العميل للحد الائتماني المسموح به (${customer.creditLimit})")
                    return@launch
                }
            }

            try {
                val invoiceId = repository.performSaleInvoice(
                    customer = customer,
                    cartItems = _cartItems.value,
                    discountAmount = _posDiscount.value,
                    taxAmount = _posTax.value,
                    paidAmount = paidAmount,
                    paymentType = paymentType,
                    paymentMethod = paymentMethod,
                    paidCurrency = paidCurrency,
                    paidCurrencyAmount = paidCurrencyAmount,
                    exchangeRate = exchangeRate,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                clearCart()
                showMessage("تم حفظ الفاتورة بنجاح")
                onSuccess(invoiceId)
            } catch (e: Exception) {
                showMessage("خطأ أثناء إتمام الفاتورة: ${e.message}")
            }
        }
    }

    // --- Currency Exchange Rate Management ---
    fun saveCurrencyRate(rate: CurrencyRate, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveCurrencyRate(rate)
                showMessage("تم حفظ بيانات وسعر صرف العملة بنجاح")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ أثناء حفظ العملة: ${e.message}")
            }
        }
    }

    fun deleteCurrencyRate(rate: CurrencyRate) {
        viewModelScope.launch {
            try {
                if (rate.isBase) {
                    showMessage("لا يمكن حذف العملة الأساسية للنظام")
                    return@launch
                }
                repository.deleteCurrencyRate(rate)
                showMessage("تم حذف العملة")
            } catch (e: Exception) {
                showMessage("خطأ أثناء حذف العملة: ${e.message}")
            }
        }
    }

    fun resetCurrenciesToDefault() {
        viewModelScope.launch {
            try {
                val defaults = listOf(
                    CurrencyRate(code = "SAR", name = "ريال سعودي", symbol = "ر.س", rateToBase = 1.0, isBase = true),
                    CurrencyRate(code = "USD", name = "دولار أمريكي", symbol = "$", rateToBase = 3.75, isBase = false),
                    CurrencyRate(code = "YER", name = "ريال يمني", symbol = "ر.ي", rateToBase = 0.007, isBase = false),
                    CurrencyRate(code = "AED", name = "درهم إماراتي", symbol = "د.إ", rateToBase = 1.02, isBase = false),
                    CurrencyRate(code = "KWD", name = "دينار كويتي", symbol = "د.ك", rateToBase = 12.2, isBase = false)
                )
                for (r in defaults) {
                    repository.saveCurrencyRate(r)
                }
                showMessage("تم تحديث واستعادة العملات الافتراضية بنجاح")
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    fun saveQuotation(notes: String, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            if (_cartItems.value.isEmpty()) {
                showMessage("السلة فارغة، يرجى إضافة أصناف أولاً")
                return@launch
            }
            try {
                val qteId = repository.saveQuotation(
                    customer = _selectedCustomer.value,
                    cartItems = _cartItems.value,
                    discountAmount = _posDiscount.value,
                    taxAmount = _posTax.value,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                clearCart()
                showMessage("تم حفظ عرض الأسعار بنجاح")
                onSuccess(qteId)
            } catch (e: Exception) {
                showMessage("خطأ أثناء حفظ عرض الأسعار: ${e.message}")
            }
        }
    }

    fun loadQuotationToCart(quotationId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val quotation = allInvoices.value.find { it.id == quotationId }
                if (quotation == null) {
                    showMessage("عرض الأسعار غير موجود")
                    return@launch
                }
                val items = allInvoiceItems.value.filter { it.invoiceId == quotationId }
                val products = allProducts.value
                val newCart = mutableListOf<AccountingRepository.CartItem>()
                for (it in items) {
                    val prod = products.find { p -> p.id == it.productId }
                    if (prod != null) {
                        newCart.add(
                            AccountingRepository.CartItem(
                                product = prod,
                                isMainUnit = it.isMainUnit,
                                quantity = it.quantity,
                                unitPrice = it.unitPrice,
                                unitCost = it.unitCost,
                                discount = it.discount
                            )
                        )
                    }
                }
                _cartItems.value = newCart
                _posDiscount.value = quotation.discountAmount
                _posTax.value = quotation.taxAmount
                if (quotation.partyId != null) {
                    _selectedCustomer.value = allCustomers.value.find { it.id == quotation.partyId }
                } else {
                    _selectedCustomer.value = null
                }
                showMessage("تم تحميل أصناف عرض الأسعار إلى سلة البيع")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ أثناء تحميل عرض الأسعار: ${e.message}")
            }
        }
    }

    fun performSalesReturn(
        originalInvoice: Invoice,
        returnedItems: List<InvoiceItem>,
        returnAmount: Double,
        refundMethod: String,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            if (returnedItems.isEmpty() || returnAmount <= 0) {
                showMessage("يرجى تحديد الأصناف المراد إرجاعها")
                return@launch
            }
            try {
                val retId = repository.performSalesReturn(
                    originalInvoice = originalInvoice,
                    returnedItems = returnedItems,
                    returnAmount = returnAmount,
                    refundMethod = refundMethod,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                showMessage("تم تسجيل مردود المبيعات وتحديث المخزون")
                onSuccess(retId)
            } catch (e: Exception) {
                showMessage("خطأ أثناء تسجيل المردود: ${e.message}")
            }
        }
    }

    // --- Purchases Cart State ---
    private val _purchaseItems = MutableStateFlow<List<AccountingRepository.CartItem>>(emptyList())
    val purchaseItems: StateFlow<List<AccountingRepository.CartItem>> = _purchaseItems.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<Supplier?>(null)
    val selectedSupplier: StateFlow<Supplier?> = _selectedSupplier.asStateFlow()

    fun addToPurchase(product: Product, isMainUnit: Boolean = true, quantity: Double = 1.0, costPrice: Double = product.purchasePrice) {
        val updated = _purchaseItems.value.toMutableList()
        updated.add(
            AccountingRepository.CartItem(
                product = product,
                isMainUnit = isMainUnit,
                quantity = quantity,
                unitPrice = costPrice,
                unitCost = costPrice
            )
        )
        _purchaseItems.value = updated
    }

    fun removePurchaseItem(index: Int) {
        if (index in _purchaseItems.value.indices) {
            val updated = _purchaseItems.value.toMutableList()
            updated.removeAt(index)
            _purchaseItems.value = updated
        }
    }

    fun clearPurchaseCart() {
        _purchaseItems.value = emptyList()
        _selectedSupplier.value = null
    }

    fun selectSupplier(supplier: Supplier?) {
        _selectedSupplier.value = supplier
    }

    fun completePurchase(
        paymentType: String,
        paidAmount: Double,
        referenceNumber: String,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            if (_purchaseItems.value.isEmpty()) {
                showMessage("يرجى إضافة منتجات لفاتورة الشراء")
                return@launch
            }
            val supplier = _selectedSupplier.value
            if (paymentType == "CREDIT" && supplier == null) {
                showMessage("يجب تحديد المورد عند الشراء الآجل")
                return@launch
            }
            try {
                val invoiceId = repository.performPurchaseInvoice(
                    supplier = supplier,
                    cartItems = _purchaseItems.value,
                    discountAmount = 0.0,
                    taxAmount = 0.0,
                    paidAmount = paidAmount,
                    paymentType = paymentType,
                    referenceNumber = referenceNumber,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                clearPurchaseCart()
                showMessage("تم حفظ فاتورة المشتريات وتحديث المخزون بنجاح")
                onSuccess(invoiceId)
            } catch (e: Exception) {
                showMessage("خطأ أثناء حفظ المشتريات: ${e.message}")
            }
        }
    }

    // --- Initial Setup & Settings ---
    fun saveStoreSettings(settings: StoreSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings.copy(isConfigured = true))
            _isUnlocked.value = true
            showMessage("تم حفظ إعدادات المتجر بنجاح")
        }
    }

    // --- Products CRUD ---
    fun saveProduct(product: Product, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (product.name.isBlank()) {
                    showMessage("اسم المنتج مطلوب")
                    return@launch
                }
                if (product.id == 0L) {
                    repository.insertProduct(product)
                    repository.logAudit("إضافة منتج", "إضافة منتج جديد: ${product.name}")
                } else {
                    repository.updateProduct(product)
                    repository.logAudit("تعديل منتج", "تعديل بيانات المنتج: ${product.name}")
                }
                showMessage("تم حفظ المنتج بنجاح")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            repository.logAudit("حذف منتج", "حذف المنتج: ${product.name}")
            showMessage("تم حذف المنتج")
        }
    }

    fun adjustStock(product: Product, newStockSubUnits: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustInventory(product, newStockSubUnits, reason, _currentUser.value.fullName)
            showMessage("تمت تسوية المخزون بنجاح")
        }
    }

    // --- Customers CRUD ---
    fun saveCustomer(customer: Customer, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (customer.name.isBlank()) {
                    showMessage("اسم العميل مطلوب")
                    return@launch
                }
                if (customer.id == 0L) {
                    repository.insertCustomer(customer)
                    repository.logAudit("إضافة عميل", "إضافة عميل جديد: ${customer.name}")
                } else {
                    repository.updateCustomer(customer)
                    repository.logAudit("تعديل عميل", "تعديل بيانات العميل: ${customer.name}")
                }
                showMessage("تم حفظ بيانات العميل")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            // Check if customer has active transactions
            if (customer.currentBalance != 0.0) {
                showMessage("لا يمكن حذف العميل لأن عليه رصيد مالي (${customer.currentBalance})")
                return@launch
            }
            repository.deleteCustomer(customer)
            repository.logAudit("حذف عميل", "حذف العميل: ${customer.name}")
            showMessage("تم حذف العميل")
        }
    }

    // --- Suppliers CRUD ---
    fun saveSupplier(supplier: Supplier, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (supplier.name.isBlank()) {
                    showMessage("اسم المورد مطلوب")
                    return@launch
                }
                if (supplier.id == 0L) {
                    repository.insertSupplier(supplier)
                    repository.logAudit("إضافة مورد", "إضافة مورد جديد: ${supplier.name}")
                } else {
                    repository.updateSupplier(supplier)
                    repository.logAudit("تعديل مورد", "تعديل بيانات المورد: ${supplier.name}")
                }
                showMessage("تم حفظ بيانات المورد")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            if (supplier.currentBalance != 0.0) {
                showMessage("لا يمكن حذف المورد لأن لديه رصيد مستحق (${supplier.currentBalance})")
                return@launch
            }
            repository.deleteSupplier(supplier)
            repository.logAudit("حذف مورد", "حذف المورد: ${supplier.name}")
            showMessage("تم حذف المورد")
        }
    }

    // --- Vouchers (سند قبض وسند صرف) ---
    fun createReceiptVoucher(
        partyType: String,
        partyId: Long?,
        partyName: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                showMessage("يجب إدخال مبلغ صحيح أكبر من الصفر")
                return@launch
            }
            try {
                repository.performReceiptVoucher(
                    partyType = partyType,
                    partyId = partyId,
                    partyName = partyName,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    description = description,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                showMessage("تم إصدار سند القبض بنجاح")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    fun createPaymentVoucher(
        partyType: String,
        partyId: Long?,
        partyName: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                showMessage("يجب إدخال مبلغ صحيح أكبر من الصفر")
                return@launch
            }
            try {
                repository.performPaymentVoucher(
                    partyType = partyType,
                    partyId = partyId,
                    partyName = partyName,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    description = description,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                showMessage("تم إصدار سند الصرف بنجاح")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    // --- Expenses ---
    fun addExpense(
        category: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                showMessage("المبلغ غير صالح")
                return@launch
            }
            try {
                repository.performExpense(
                    category = category,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    description = description,
                    notes = notes,
                    currentUser = _currentUser.value.fullName
                )
                showMessage("تم تسجيل المصروف بنجاح")
                onSuccess()
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    // --- Cancel Invoice (Safe Reversal) ---
    fun cancelInvoice(invoice: Invoice) {
        viewModelScope.launch {
            try {
                repository.cancelInvoice(invoice, _currentUser.value.fullName)
                showMessage("تم إلغاء الفاتورة وعكس تأثيراتها المالية والمخزنية")
            } catch (e: Exception) {
                showMessage("خطأ أثناء إلغاء الفاتورة: ${e.message}")
            }
        }
    }

    // --- Opening Cash ---
    fun setOpeningCash(amount: Double) {
        viewModelScope.launch {
            repository.setOpeningCashBalance(amount, _currentUser.value.fullName)
            showMessage("تم تحديث الرصيد الافتتاحي للصندوق")
        }
    }

    // --- Load Demo Data (Optional for testing) ---
    fun loadDemoData(onFinished: () -> Unit) {
        viewModelScope.launch {
            try {
                // Products
                val demoProducts = listOf(
                    Product(
                        sku = "PRD-001",
                        barcode = "6281001001",
                        name = "حليب الممتاز",
                        category = "ألبان وأجبان",
                        mainUnit = "كرتون",
                        subUnit = "علبة",
                        conversionFactor = 24.0,
                        purchasePrice = 120.0,
                        cashSalePrice = 144.0,
                        creditSalePrice = 150.0,
                        currentStockSubUnits = 240.0,
                        minStockSubUnits = 48.0,
                        supplierName = "شركة النخبة للمواد الغذائية"
                    ),
                    Product(
                        sku = "PRD-002",
                        barcode = "6281001002",
                        name = "سكر الأسرة 5 كجم",
                        category = "تموينات",
                        mainUnit = "كيس كبير",
                        subUnit = "كيس",
                        conversionFactor = 10.0,
                        purchasePrice = 180.0,
                        cashSalePrice = 210.0,
                        creditSalePrice = 220.0,
                        currentStockSubUnits = 50.0,
                        minStockSubUnits = 10.0,
                        supplierName = "مؤسسة التموين الحديث"
                    ),
                    Product(
                        sku = "PRD-003",
                        barcode = "6281001003",
                        name = "زيت عافية ذرة 1.5 لتر",
                        category = "زيوت",
                        mainUnit = "كرتون",
                        subUnit = "حبة",
                        conversionFactor = 6.0,
                        purchasePrice = 90.0,
                        cashSalePrice = 108.0,
                        creditSalePrice = 115.0,
                        currentStockSubUnits = 36.0,
                        minStockSubUnits = 12.0,
                        supplierName = "شركة النخبة للمواد الغذائية"
                    ),
                    Product(
                        sku = "PRD-004",
                        barcode = "6281001004",
                        name = "أرز الشعلان 10 كجم",
                        category = "تموينات",
                        mainUnit = "كيس",
                        subUnit = "كيس",
                        conversionFactor = 1.0,
                        purchasePrice = 65.0,
                        cashSalePrice = 78.0,
                        creditSalePrice = 82.0,
                        currentStockSubUnits = 15.0,
                        minStockSubUnits = 5.0,
                        supplierName = "مؤسسة التموين الحديث"
                    ),
                    Product(
                        sku = "PRD-005",
                        barcode = "6281001005",
                        name = "شاي ربيع أوراق 400 جم",
                        category = "مشروبات ساخنة",
                        mainUnit = "كرتون",
                        subUnit = "حبة",
                        conversionFactor = 12.0,
                        purchasePrice = 160.0,
                        cashSalePrice = 192.0,
                        creditSalePrice = 200.0,
                        currentStockSubUnits = 4.0, // Low stock!
                        minStockSubUnits = 12.0,
                        supplierName = "شركة النخبة للمواد الغذائية"
                    )
                )
                repository.insertProducts(demoProducts)

                // Customers
                val demoCustomers = listOf(
                    Customer(
                        name = "سوبرماركت البركة",
                        phone = "0501112233",
                        address = "شارع الملك فهد",
                        creditLimit = 5000.0,
                        currentBalance = 1200.0,
                        totalSales = 3500.0,
                        totalPaid = 2300.0
                    ),
                    Customer(
                        name = "مخبز الأمل",
                        phone = "0554443322",
                        address = "حي السلام",
                        creditLimit = 3000.0,
                        currentBalance = 450.0,
                        totalSales = 2100.0,
                        totalPaid = 1650.0
                    ),
                    Customer(
                        name = "مطعم السعادة",
                        phone = "0567778899",
                        address = "طريق المطار",
                        creditLimit = 10000.0,
                        currentBalance = 0.0,
                        totalSales = 8900.0,
                        totalPaid = 8900.0
                    )
                )
                repository.insertCustomers(demoCustomers)

                // Suppliers
                val demoSuppliers = listOf(
                    Supplier(
                        name = "شركة النخبة للمواد الغذائية",
                        phone = "0112233445",
                        address = "المنطقة الصناعية",
                        currentBalance = 2400.0,
                        totalPurchases = 15000.0,
                        totalPaid = 12600.0
                    ),
                    Supplier(
                        name = "مؤسسة التموين الحديث",
                        phone = "0118899001",
                        address = "سوق الجملة",
                        currentBalance = 850.0,
                        totalPurchases = 6200.0,
                        totalPaid = 5350.0
                    )
                )
                repository.insertSuppliers(demoSuppliers)

                // Opening Cash
                repository.setOpeningCashBalance(10000.0, "المدير العام")

                showMessage("تم تحميل البيانات التجريبية بنجاح")
                onFinished()
            } catch (e: Exception) {
                showMessage("خطأ أثناء تحميل البيانات: ${e.message}")
            }
        }
    }
}

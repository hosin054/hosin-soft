package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Setup : Screen("setup", "الإعداد الأولي")
    object Lock : Screen("lock", "قفل الأمان")
    object Dashboard : Screen("dashboard", "الرئيسية")
    object Pos : Screen("pos", "نقطة البيع")
    object Products : Screen("products", "المنتجات")
    object ProductForm : Screen("product_form?productId={productId}", "إدارة منتج") {
        fun createRoute(productId: Long = 0L) = "product_form?productId=$productId"
    }
    object Inventory : Screen("inventory", "المخزون والجرد")
    object Parties : Screen("parties", "العملاء والموردين")
    object CustomerDetail : Screen("customer_detail/{customerId}", "كشف حساب عميل") {
        fun createRoute(customerId: Long) = "customer_detail/$customerId"
    }
    object SupplierDetail : Screen("supplier_detail/{supplierId}", "كشف حساب مورد") {
        fun createRoute(supplierId: Long) = "supplier_detail/$supplierId"
    }
    object Purchases : Screen("purchases", "المشتريات")
    object PurchaseForm : Screen("purchase_form", "فاتورة شراء جديدة")
    object Vouchers : Screen("vouchers", "السندات المالية")
    object Expenses : Screen("expenses", "المصروفات")
    object Cash : Screen("cash", "حركة الصندوق")
    object Reports : Screen("reports", "التقارير المالية")
    object ImportExport : Screen("import_export", "استيراد وتصدير إكسل")
    object Settings : Screen("settings", "الإعدادات")
    object InvoiceDetail : Screen("invoice_detail/{invoiceId}", "تفاصيل الفاتورة") {
        fun createRoute(invoiceId: Long) = "invoice_detail/$invoiceId"
    }
    object AuditLogs : Screen("audit_logs", "سجل العمليات")
    object Quotations : Screen("quotations", "عروض الأسعار")
    object BarcodeLabels : Screen("barcode_labels?productId={productId}", "طباعة ملصقات الباركود والأسعار") {
        fun createRoute(productId: Long = 0L) = "barcode_labels?productId=$productId"
    }
}

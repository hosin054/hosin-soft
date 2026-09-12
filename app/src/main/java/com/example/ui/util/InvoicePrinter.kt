package com.example.ui.util

import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.StoreSettings

object InvoicePrinter {

    fun generateReceiptText(
        invoice: Invoice,
        items: List<InvoiceItem>,
        settings: StoreSettings?
    ): String {
        val storeName = settings?.storeName ?: "حسين سوفت"
        val phone = settings?.phone ?: ""
        val address = settings?.address ?: ""
        val currency = settings?.currencySymbol ?: "ر.س"

        val sb = StringBuilder()
        sb.append("================================\n")
        sb.append("         $storeName             \n")
        if (phone.isNotBlank()) sb.append("        هاتف: $phone          \n")
        if (address.isNotBlank()) sb.append("       العنوان: $address       \n")
        sb.append("================================\n")
        sb.append("نوع الفاتورة: ${if (invoice.invoiceType == "SALE") "فاتورة مبيعات" else "فاتورة مشتريات"}\n")
        sb.append("طريقة الدفع: ${if (invoice.paymentType == "CASH") "نقداً" else "آجل"}\n")
        sb.append("رقم الفاتورة: ${invoice.invoiceNumber}\n")
        sb.append("التاريخ: ${Formatters.formatDate(invoice.createdAt)}\n")
        sb.append("الطرف: ${invoice.partyName}\n")
        sb.append("الموظف: ${invoice.createdBy}\n")
        sb.append("--------------------------------\n")
        sb.append(String.format("%-14s %4s %6s %7s\n", "الصنف", "الكمية", "السعر", "الإجمالي"))
        sb.append("--------------------------------\n")

        for (item in items) {
            val nameTruncated = if (item.productName.length > 14) item.productName.substring(0, 12) + ".." else item.productName
            sb.append(String.format("%-14s %4.1f %6.2f %7.2f\n", nameTruncated, item.quantity, item.unitPrice, item.total))
            if (item.unitName.isNotBlank()) {
                sb.append("  [الوحدة: ${item.unitName}]\n")
            }
        }

        sb.append("--------------------------------\n")
        sb.append("المجموع الفرعي:   ${Formatters.formatMoney(invoice.subtotal, settings)}\n")
        if (invoice.discountAmount > 0) {
            sb.append("الخصم:           ${Formatters.formatMoney(invoice.discountAmount, settings)}\n")
        }
        if (invoice.taxAmount > 0) {
            sb.append("ضريبة القيمة المضافة: ${Formatters.formatMoney(invoice.taxAmount, settings)}\n")
        }
        sb.append("================================\n")
        sb.append("الإجمالي النهائي: ${Formatters.formatMoney(invoice.totalAmount, settings)}\n")
        sb.append("المدفوع:         ${Formatters.formatMoney(invoice.paidAmount, settings)}\n")
        sb.append("المتبقي:         ${Formatters.formatMoney(invoice.remainingAmount, settings)}\n")
        sb.append("================================\n")
        if (invoice.notes.isNotBlank()) {
            sb.append("ملاحظات: ${invoice.notes}\n")
        }
        sb.append("      شكراً لتعاملكم معنا!       \n")
        sb.append("  نظام حسين سوفت لإدارة الحسابات \n")
        sb.append("================================\n")

        return sb.toString()
    }

    fun generateQuotationText(
        quotation: Invoice,
        items: List<InvoiceItem>,
        settings: StoreSettings?
    ): String {
        val storeName = settings?.storeName ?: "حسين سوفت"
        val phone = settings?.phone ?: ""
        val address = settings?.address ?: ""

        val sb = StringBuilder()
        sb.append("================================\n")
        sb.append("         $storeName             \n")
        sb.append("          عرض أسعار رسمي         \n")
        if (phone.isNotBlank()) sb.append("        هاتف: $phone          \n")
        if (address.isNotBlank()) sb.append("       العنوان: $address       \n")
        sb.append("================================\n")
        sb.append("رقم العرض: ${quotation.invoiceNumber}\n")
        sb.append("التاريخ: ${Formatters.formatDate(quotation.createdAt)}\n")
        sb.append("العميل: ${quotation.partyName}\n")
        sb.append("صالح لمدة: 15 يوماً من تاريخه\n")
        sb.append("المسؤول: ${quotation.createdBy}\n")
        sb.append("--------------------------------\n")
        sb.append(String.format("%-14s %4s %6s %7s\n", "الصنف", "الكمية", "السعر", "الإجمالي"))
        sb.append("--------------------------------\n")

        for (item in items) {
            val nameTruncated = if (item.productName.length > 14) item.productName.substring(0, 12) + ".." else item.productName
            sb.append(String.format("%-14s %4.1f %6.2f %7.2f\n", nameTruncated, item.quantity, item.unitPrice, item.total))
            if (item.unitName.isNotBlank()) {
                sb.append("  [الوحدة: ${item.unitName}]\n")
            }
        }

        sb.append("--------------------------------\n")
        sb.append("المجموع:         ${Formatters.formatMoney(quotation.subtotal, settings)}\n")
        if (quotation.discountAmount > 0) {
            sb.append("الخصم الممنوح:   ${Formatters.formatMoney(quotation.discountAmount, settings)}\n")
        }
        if (quotation.taxAmount > 0) {
            sb.append("الضريبة:         ${Formatters.formatMoney(quotation.taxAmount, settings)}\n")
        }
        sb.append("================================\n")
        sb.append("الإجمالي التقديري: ${Formatters.formatMoney(quotation.totalAmount, settings)}\n")
        sb.append("================================\n")
        if (quotation.notes.isNotBlank()) {
            sb.append("ملاحظات وشروط: ${quotation.notes}\n")
        }
        sb.append("يسعدنا دائماً خدمتكم وتقديم أفضل الأسعار\n")
        sb.append("================================\n")
        return sb.toString()
    }

    fun generateShelfLabelText(
        productName: String,
        barcode: String,
        price: Double,
        unitName: String,
        storeName: String,
        currency: String
    ): String {
        return """
            ================================
            $storeName
            --------------------------------
            الصنف: $productName
            الوحدة: $unitName
            السعر: $price $currency
            الباركود: $barcode
            التاريخ: ${Formatters.formatDate(System.currentTimeMillis())}
            ================================
        """.trimIndent()
    }
}

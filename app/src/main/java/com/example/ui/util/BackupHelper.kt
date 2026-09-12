package com.example.ui.util

import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    suspend fun createBackupJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val dao = db.appDao()
        val root = JSONObject()

        root.put("version", 1)
        root.put("appName", "HusseinSoft")
        root.put("timestamp", System.currentTimeMillis())

        // Settings
        val settings = dao.getSettingsDirect()
        if (settings != null) {
            val sObj = JSONObject().apply {
                put("storeName", settings.storeName)
                put("ownerName", settings.ownerName)
                put("phone", settings.phone)
                put("address", settings.address)
                put("currencyName", settings.currencyName)
                put("currencySymbol", settings.currencySymbol)
                put("decimalPlaces", settings.decimalPlaces)
                put("taxEnabled", settings.taxEnabled)
                put("taxRate", settings.taxRate)
                put("securityPin", settings.securityPin)
            }
            root.put("settings", sObj)
        }

        // Products
        val products = dao.getAllProducts().firstOrNull() ?: emptyList()
        val prodArray = JSONArray()
        for (p in products) {
            val pObj = JSONObject().apply {
                put("sku", p.sku)
                put("barcode", p.barcode)
                put("name", p.name)
                put("description", p.description)
                put("category", p.category)
                put("brand", p.brand)
                put("mainUnit", p.mainUnit)
                put("subUnit", p.subUnit)
                put("conversionFactor", p.conversionFactor)
                put("purchasePrice", p.purchasePrice)
                put("cashSalePrice", p.cashSalePrice)
                put("creditSalePrice", p.creditSalePrice)
                put("currentStockSubUnits", p.currentStockSubUnits)
                put("minStockSubUnits", p.minStockSubUnits)
                put("supplierName", p.supplierName)
                put("isActive", p.isActive)
            }
            prodArray.put(pObj)
        }
        root.put("products", prodArray)

        // Customers
        val customers = dao.getAllCustomers().firstOrNull() ?: emptyList()
        val custArray = JSONArray()
        for (c in customers) {
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("creditLimit", c.creditLimit)
                put("previousBalance", c.previousBalance)
                put("currentBalance", c.currentBalance)
                put("totalSales", c.totalSales)
                put("totalPaid", c.totalPaid)
            }
            custArray.put(cObj)
        }
        root.put("customers", custArray)

        // Suppliers
        val suppliers = dao.getAllSuppliers().firstOrNull() ?: emptyList()
        val suppArray = JSONArray()
        for (s in suppliers) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("phone", s.phone)
                put("address", s.address)
                put("currentBalance", s.currentBalance)
                put("totalPurchases", s.totalPurchases)
                put("totalPaid", s.totalPaid)
            }
            suppArray.put(sObj)
        }
        root.put("suppliers", suppArray)

        return@withContext root.toString(2)
    }

    suspend fun saveBackupToFile(context: Context, json: String): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
        val fileName = "hussein_soft_backup_${dateFormat.format(Date())}.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(json, Charsets.UTF_8)
        return@withContext file
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareToTelegram(context: Context, text: String, title: String = "إرسال إلى تلغرام") {
        try {
            // First attempt: direct telegram intent
            val telegramIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("org.telegram.messenger")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(telegramIntent)
        } catch (e: Exception) {
            // Second attempt: web share link or system chooser
            try {
                val encoded = java.net.URLEncoder.encode(text, "UTF-8")
                val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/share/url?url=&text=$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                shareText(context, text, title)
            }
        }
    }
}

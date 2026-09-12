package com.example.ui.util

import com.example.data.model.StoreSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    fun formatMoney(amount: Double, settings: StoreSettings?): String {
        val decimals = settings?.decimalPlaces ?: 2
        val symbol = settings?.currencySymbol ?: "ر.س"
        return String.format(Locale.ENGLISH, "%.${decimals}f %s", amount, symbol)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }
}

package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppDao
import com.example.data.model.*

@Database(
    entities = [
        StoreSettings::class,
        User::class,
        Category::class,
        Product::class,
        Customer::class,
        Supplier::class,
        CurrencyRate::class,
        Invoice::class,
        InvoiceItem::class,
        Voucher::class,
        Expense::class,
        CashTransaction::class,
        InventoryTransaction::class,
        AuditLog::class,
        JournalVoucher::class,
        JournalVoucherLine::class,
        ChartOfAccount::class,
        CostCenter::class,
        FixedAsset::class,
        Cheque::class,
        AccountTransfer::class,
        ShiftRecord::class,
        InvoiceInstallment::class,
        PurchaseOrder::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hussein_soft_accounting.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

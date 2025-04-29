package com.acagribahar.muscleandmindapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.acagribahar.muscleandmindapp.data.local.dao.TaskDao
import com.acagribahar.muscleandmindapp.data.local.entity.Task

@Database(
    entities = [Task::class], // Bu veritabanında hangi Entity'ler (tablolar) var?
    version = 1,              // Veritabanı şemasının sürümü (şema değişirse artırılır)
    exportSchema = false      // Şema export edilsin mi? (Migration için, şimdilik false)
)
abstract class AppDatabase : RoomDatabase() {

    // Room bu abstract fonksiyonu kullanarak DAO örneğini oluşturacak
    abstract fun taskDao(): TaskDao

    // Companion object ile Singleton pattern uygulayarak
    // veritabanı örneğinin tek bir yerden yönetilmesini sağlıyoruz.
    companion object {
        // @Volatile: Bu değişkenin değerinin farklı thread'ler tarafından anlık olarak görülebilir olmasını sağlar.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Eğer INSTANCE null ise (henüz oluşturulmadıysa), synchronized bloğuna gir.
            // synchronized: Aynı anda sadece bir thread'in bu bloğa girmesini sağlar (thread-safe).
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Uygulama Context'i
                    AppDatabase::class.java,    // Bizim Database sınıfımız
                    "mind_muscle_database"      // Veritabanı dosyasının adı
                )
                    // .fallbackToDestructiveMigration() // Migration stratejisi (şimdilik eklemeyelim)
                    .build() // Veritabanını oluştur/inşa et
                INSTANCE = instance // Oluşturulan örneği INSTANCE'a ata
                instance // Ve bu örneği döndür
            }
        }
    }
}
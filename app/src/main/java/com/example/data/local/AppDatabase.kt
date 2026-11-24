package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TriageSessionEntity::class,
        SymptomLogEntity::class,
        SymptomNoteEntity::class,
        DeveloperLogEntity::class,
        ChatMessageEntity::class,
        UserProfileEntity::class,
        WaterLogEntity::class,
        MedicineEntity::class,
        MedicationAdherenceEntity::class,
        ScannedReportEntity::class,
        StepRecordEntity::class,
        VitalLogEntity::class,
        FamilyMemberEntity::class,
        TreatmentPlanEntity::class,
        RiskAssessmentEntity::class,
        ClinicalNoteEntity::class,
        EhrRecordEntity::class,
        MentalHealthCheckInEntity::class,
        DrugCandidateStudyEntity::class,
        OutbreakAlertEntity::class,
        DoctorAppointmentEntity::class,
        UserAccountEntity::class,
        PublicDoctorEntity::class,
        DeveloperAppConfigEntity::class,
        SleepWellnessLogEntity::class,
        MealNutritionLogEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun triageDao(): TriageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun hasColumn(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            return try {
                db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex < 0) return false
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                            return true
                        }
                    }
                    false
                }
            } catch (_: Throwable) {
                false
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                if (!hasColumn(db, "user_profile", "pmjayCardNumber")) {
                    db.execSQL("ALTER TABLE user_profile ADD COLUMN pmjayCardNumber TEXT NOT NULL DEFAULT ''")
                }
                if (!hasColumn(db, "user_profile", "abhaCardNumber")) {
                    db.execSQL("ALTER TABLE user_profile ADD COLUMN abhaCardNumber TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sleep_wellness_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `sleepHours` REAL NOT NULL,
                        `sleepQuality` INTEGER NOT NULL,
                        `bedTime` TEXT NOT NULL,
                        `wakeTime` TEXT NOT NULL,
                        `weightKg` REAL NOT NULL,
                        `heightCm` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_wellness_logs_dateKey` ON `sleep_wellness_logs` (`dateKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_wellness_logs_timestamp` ON `sleep_wellness_logs` (`timestamp`)")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                fun addCol(col: String, sqlTypeAndDefault: String) {
                    if (!hasColumn(db, "developer_app_config", col)) {
                        db.execSQL("ALTER TABLE developer_app_config ADD COLUMN $col $sqlTypeAndDefault")
                    }
                }
                addCol("upgradePublished", "INTEGER NOT NULL DEFAULT 0")
                addCol("upgradeTargetVersion", "TEXT NOT NULL DEFAULT ''")
                addCol("upgradeTitle", "TEXT NOT NULL DEFAULT ''")
                addCol("upgradeMessage", "TEXT NOT NULL DEFAULT ''")
                addCol("upgradeDownloadUrl", "TEXT NOT NULL DEFAULT ''")
                addCol("upgradeMandatory", "INTEGER NOT NULL DEFAULT 0")
                addCol("ultraHubFeatureEnabled", "INTEGER NOT NULL DEFAULT 1")
                addCol("pmjayFinderEnabled", "INTEGER NOT NULL DEFAULT 1")
                addCol("pmjayPortalUrlOverride", "TEXT NOT NULL DEFAULT 'https://hem.nha.gov.in/search'")
                addCol("reportScannerFeatureEnabled", "INTEGER NOT NULL DEFAULT 1")
                addCol("splashDurationSeconds", "INTEGER NOT NULL DEFAULT 5")
                addCol("splashTopCreditText", "TEXT NOT NULL DEFAULT 'Created by Shailesh and Ruturaj'")
                addCol("splashSchoolNameText", "TEXT NOT NULL DEFAULT 'Model School Sayla'")
                addCol("maintenanceModeEnabled", "INTEGER NOT NULL DEFAULT 0")
                addCol("maintenanceMessage", "TEXT NOT NULL DEFAULT ''")
                addCol("supportContactPhone", "TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meal_nutrition_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `mealType` TEXT NOT NULL,
                        `foodName` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `proteinGrams` REAL NOT NULL,
                        `carbsGrams` REAL NOT NULL,
                        `fiberGrams` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_nutrition_logs_dateKey` ON `meal_nutrition_logs` (`dateKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_nutrition_logs_timestamp` ON `meal_nutrition_logs` (`timestamp`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mediassist_database"
                )
                    .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

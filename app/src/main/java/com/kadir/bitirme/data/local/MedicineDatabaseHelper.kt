package com.kadir.bitirme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kadir.bitirme.data.model.MedicineEntity

/**
 * SQLite veritabanı yöneticisi
 * İlaç bilgilerini yerel olarak saklar
 */
class MedicineDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_MEDICINES)
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICINES")
        onCreate(db)
    }

    /**
     * Başlangıç verilerini ekler (20+ yaygın Türk ilaçları)
     */
    private fun seedInitialData(db: SQLiteDatabase) {
        val medicines = listOf(
            MedicineEntity(
                name = "Aspirin",
                genericName = "Asetilsalisilik Asit",
                dosage = "500mg",
                form = "Tablet",
                usage = "Ağrı kesici ve ateş düşürücü. Günde 3-4 defa 1 tablet.",
                sideEffects = "Mide bulantısı, mide kanaması",
                warnings = "Mide rahatsızlığı olanlarda dikkatle kullanılmalı"
            ),
            MedicineEntity(
                name = "Parol",
                genericName = "Parasetamol",
                dosage = "500mg",
                form = "Tablet",
                usage = "Ağrı kesici ve ateş düşürücü. Günde 3 defa 1-2 tablet.",
                sideEffects = "Karaciğer hasarı (aşırı dozda)",
                warnings = "Günlük doz 4000mg'ı geçmemeli"
            ),
            MedicineEntity(
                name = "Major",
                genericName = "Parasetamol",
                dosage = "500mg",
                form = "Tablet",
                usage = "Ağrı ve ateş için. 4-6 saat arayla 1 tablet.",
                sideEffects = "Nadiren alerjik reaksiyon",
                warnings = "Karaciğer hastalığı olanlarda dikkat"
            ),
            MedicineEntity(
                name = "Aferin",
                genericName = "İbuprofen",
                dosage = "400mg",
                form = "Tablet",
                usage = "Ağrı ve iltihaplanma için. Günde 3 defa 1 tablet.",
                sideEffects = "Mide bulantısı, baş ağrısı",
                warnings = "Tok karnına alınmalı"
            ),
            MedicineEntity(
                name = "Nurofen",
                genericName = "İbuprofen",
                dosage = "200mg",
                form = "Tablet",
                usage = "Hafif-orta şiddetli ağrılar için. 4-6 saat arayla 1-2 tablet.",
                sideEffects = "Mide rahatsızlığı",
                warnings = "Hamilelerde kullanılmamalı"
            ),
            MedicineEntity(
                name = "Minoset",
                genericName = "Diklofenak Sodyum",
                dosage = "50mg",
                form = "Tablet",
                usage = "Ağrı ve iltihaplanma için. Günde 2-3 defa 1 tablet.",
                sideEffects = "Mide ağrısı, baş dönmesi",
                warnings = "Mide ülseri olanlarda kullanılmamalı"
            ),
            MedicineEntity(
                name = "Voltaren",
                genericName = "Diklofenak",
                dosage = "75mg",
                form = "Enjeksiyon",
                usage = "Kas içi enjeksiyon. Günde 1-2 defa.",
                sideEffects = "Enjeksiyon bölgesinde ağrı",
                warnings = "Sadece sağlık personeli tarafından uygulanmalı"
            ),
            MedicineEntity(
                name = "Coraspin",
                genericName = "Asetilsalisilik Asit",
                dosage = "100mg",
                form = "Tablet",
                usage = "Kalp-damar hastalıkları için. Günde 1 tablet.",
                sideEffects = "Kanama riski",
                warnings = "Ameliyat öncesi bırakılmalı"
            ),
            MedicineEntity(
                name = "Majezik",
                genericName = "Deksketoprofen",
                dosage = "25mg",
                form = "Tablet",
                usage = "Akut ağrı için. 4-6 saat arayla 1 tablet.",
                sideEffects = "Mide bulantısı, baş ağrısı",
                warnings = "Maksimum 3 gün kullanılmalı"
            ),
            MedicineEntity(
                name = "Calpol",
                genericName = "Parasetamol",
                dosage = "120mg/5ml",
                form = "Şurup",
                usage = "Çocuklarda ateş ve ağrı için. Dozaj kiloya göre ayarlanır.",
                sideEffects = "Nadiren alerjik reaksiyon",
                warnings = "Çocuk dozları kesin takip edilmeli"
            ),
            MedicineEntity(
                name = "Augmentin",
                genericName = "Amoksisilin + Klavulanik Asit",
                dosage = "1000mg",
                form = "Tablet",
                usage = "Bakteri enfeksiyonları için. Günde 2 defa 1 tablet.",
                sideEffects = "İshal, mide bulantısı",
                warnings = "Reçete ile satılır, kürü tamamlanmalı"
            ),
            MedicineEntity(
                name = "Cipro",
                genericName = "Siprofloksasin",
                dosage = "500mg",
                form = "Tablet",
                usage = "Enfeksiyonlar için. Günde 2 defa 1 tablet.",
                sideEffects = "Baş ağrısı, uykusuzluk",
                warnings = "18 yaş altında kullanılmamalı"
            ),
            MedicineEntity(
                name = "Deltacortril",
                genericName = "Prednizolon",
                dosage = "5mg",
                form = "Tablet",
                usage = "İltihap ve alerji için. Doktor önerisi ile.",
                sideEffects = "Kilo alımı, yüz şişmesi",
                warnings = "Ani kesilmemeli, kademeli azaltılmalı"
            ),
            MedicineEntity(
                name = "Delix",
                genericName = "Ramipril",
                dosage = "5mg",
                form = "Tablet",
                usage = "Yüksek tansiyon için. Günde 1 defa 1 tablet.",
                sideEffects = "Kuru öksürük, baş dönmesi",
                warnings = "Hamilelikte kullanılmamalı"
            ),
            MedicineEntity(
                name = "Concor",
                genericName = "Bisoprolol",
                dosage = "5mg",
                form = "Tablet",
                usage = "Kalp hastalıkları ve tansiyon için. Günde 1 defa.",
                sideEffects = "Yorgunluk, soğuk eller",
                warnings = "Astım hastalarında dikkatli kullanılmalı"
            ),
            MedicineEntity(
                name = "Nexium",
                genericName = "Esomeprazol",
                dosage = "20mg",
                form = "Kapsül",
                usage = "Mide asidi için. Günde 1 defa, sabah aç karnına.",
                sideEffects = "Baş ağrısı, kabızlık",
                warnings = "Uzun süreli kullanımda kemik erimesi riski"
            ),
            MedicineEntity(
                name = "Lansor",
                genericName = "Lansoprazol",
                dosage = "30mg",
                form = "Kapsül",
                usage = "Mide ve reflü için. Sabah aç karnına 1 kapsül.",
                sideEffects = "İshal, baş ağrısı",
                warnings = "Doktor kontrolü ile kullanılmalı"
            ),
            MedicineEntity(
                name = "Xanax",
                genericName = "Alprazolam",
                dosage = "0.5mg",
                form = "Tablet",
                usage = "Anksiyete için. Doktor önerisi kesinlikle gerekli.",
                sideEffects = "Uyuşukluk, bağımlılık",
                warnings = "Kırmızı reçete ile satılır, bağımlılık yapıcı"
            ),
            MedicineEntity(
                name = "Zoloft",
                genericName = "Sertralin",
                dosage = "50mg",
                form = "Tablet",
                usage = "Depresyon tedavisi. Günde 1 defa, sabah.",
                sideEffects = "Bulantı, uykusuzluk",
                warnings = "Tedavi ani bırakılmamalı"
            ),
            MedicineEntity(
                name = "Euthyrox",
                genericName = "Levotiroksin",
                dosage = "100mcg",
                form = "Tablet",
                usage = "Tiroid hastalığı için. Sabah aç karnına 1 tablet.",
                sideEffects = "Çarpıntı, kilo kaybı (yüksek dozda)",
                warnings = "Düzenli kan testi gerektirir"
            ),
            MedicineEntity(
                name = "Metformin",
                genericName = "Metformin HCl",
                dosage = "850mg",
                form = "Tablet",
                usage = "Diyabet için. Yemekle birlikte günde 2-3 defa.",
                sideEffects = "Mide bulantısı, ishal",
                warnings = "Böbrek fonksiyonları kontrol edilmeli"
            ),
            MedicineEntity(
                name = "Glifor",
                genericName = "Metformin",
                dosage = "1000mg",
                form = "Tablet",
                usage = "Tip 2 diyabet için. Yemekle birlikte 1-2 tablet.",
                sideEffects = "Gaz, ishal",
                warnings = "Karaciğer hastalığında kullanılmamalı"
            ),
            MedicineEntity(
                name = "Amaryl",
                genericName = "Glimepirid",
                dosage = "2mg",
                form = "Tablet",
                usage = "Diyabet tedavisi. Sabah kahvaltıdan önce 1 tablet.",
                sideEffects = "Hipoglisemi (düşük şeker)",
                warnings = "Düzenli kan şekeri takibi yapılmalı"
            ),
            MedicineEntity(
                name = "Lipitor",
                genericName = "Atorvastatin",
                dosage = "10mg",
                form = "Tablet",
                usage = "Yüksek kolesterol için. Günde 1 defa, akşam.",
                sideEffects = "Kas ağrısı, karaciğer enzim yükselmesi",
                warnings = "Düzenli karaciğer testi gerektirir"
            ),
            MedicineEntity(
                name = "Diovan",
                genericName = "Valsartan",
                dosage = "80mg",
                form = "Tablet",
                usage = "Hipertansiyon için. Günde 1 defa.",
                sideEffects = "Baş dönmesi, yorgunluk",
                warnings = "Hamilelikte kullanılmamalı"
            ),
            MedicineEntity(
                name = "Arveles",
                genericName = "Deksketoprofen",
                dosage = "25mg",
                form = "Tablet",
                usage = "Akut ağrı tedavisi için. 8 saat arayla 1 tablet.",
                sideEffects = "Mide bulantısı, baş ağrısı, uyuşukluk",
                warnings = "Maksimum 3 gün kullanılmalı, tok karnına alınmalı"
            )
        )

        medicines.forEachIndexed { index, medicine ->
            val values = ContentValues().apply {
                put(COLUMN_ID, index + 1)
                put(COLUMN_NAME, medicine.name)
                put(COLUMN_GENERIC_NAME, medicine.genericName)
                put(COLUMN_DOSAGE, medicine.dosage)
                put(COLUMN_FORM, medicine.form)
                put(COLUMN_USAGE, medicine.usage)
                put(COLUMN_SIDE_EFFECTS, medicine.sideEffects)
                put(COLUMN_WARNINGS, medicine.warnings)
            }
            db.insert(TABLE_MEDICINES, null, values)
        }
    }

    companion object {
        private const val DATABASE_NAME = "medicine.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_MEDICINES = "medicines"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_GENERIC_NAME = "generic_name"
        const val COLUMN_DOSAGE = "dosage"
        const val COLUMN_FORM = "form"
        const val COLUMN_USAGE = "usage"
        const val COLUMN_SIDE_EFFECTS = "side_effects"
        const val COLUMN_WARNINGS = "warnings"

        private const val CREATE_TABLE_MEDICINES = """
            CREATE TABLE $TABLE_MEDICINES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_GENERIC_NAME TEXT NOT NULL,
                $COLUMN_DOSAGE TEXT NOT NULL,
                $COLUMN_FORM TEXT NOT NULL,
                $COLUMN_USAGE TEXT NOT NULL,
                $COLUMN_SIDE_EFFECTS TEXT,
                $COLUMN_WARNINGS TEXT
            )
        """
    }
}

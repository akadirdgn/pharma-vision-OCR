package com.kadir.bitirme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kadir.bitirme.data.model.MedicineEntity

/**
 * SQLite veritabanı yöneticisi
 * İlaç bilgilerini, tarama geçmişini ve günlük dozları yerel olarak saklar
 */
class MedicineDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_MEDICINES)
        db.execSQL(CREATE_TABLE_SCAN_HISTORY)
        db.execSQL(CREATE_TABLE_DOSE_TRACKER)
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICINES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCAN_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOSE_TRACKER")
        onCreate(db)
    }

    /**
     * Başlangıç verilerini ekler (Kategorize edilmiş yaygın Türk ilaçları + Etkileşimler)
     */
    private fun seedInitialData(db: SQLiteDatabase) {
        val medicines = listOf(
            // Ağrı Kesici ve Ateş Düşürücüler
            MedicineEntity(name = "Aspirin", genericName = "Asetilsalisilik Asit", dosage = "500mg", form = "Tablet", usage = "Günde 3-4 defa 1 tablet.", sideEffects = "Mide bulantısı", warnings = "Mide rahatsızlığı olanlarda dikkat", category = "Ağrı Kesici", interactingDrugs = "İbuprofen,Klopidogrel,Diklofenak"),
            MedicineEntity(name = "Parol", genericName = "Parasetamol", dosage = "500mg", form = "Tablet", usage = "Günde 3 defa 1-2 tablet.", sideEffects = "Karaciğer hasarı (aşırı dozda)", warnings = "Günlük doz 4000mg'ı geçmemeli", category = "Ağrı Kesici"),
            MedicineEntity(name = "Major", genericName = "Parasetamol", dosage = "500mg", form = "Tablet", usage = "4-6 saat arayla 1 tablet.", sideEffects = "Nadiren alerjik reaksiyon", warnings = "Karaciğer hastalığında dikkat", category = "Ağrı Kesici"),
            MedicineEntity(name = "Aferin", genericName = "İbuprofen", dosage = "400mg", form = "Tablet", usage = "Günde 3 defa 1 tablet.", sideEffects = "Mide bulantısı", warnings = "Tok karnına alınmalı", category = "Ağrı Kesici", interactingDrugs = "Asetilsalisilik Asit,Klopidogrel,Diklofenak"),
            MedicineEntity(name = "Nurofen", genericName = "İbuprofen", dosage = "200mg", form = "Tablet", usage = "4-6 saat arayla 1-2 tablet.", sideEffects = "Mide rahatsızlığı", warnings = "Hamilelerde kullanılmamalı", category = "Ağrı Kesici", interactingDrugs = "Asetilsalisilik Asit,Klopidogrel"),
            MedicineEntity(name = "Minoset", genericName = "Diklofenak Sodyum", dosage = "50mg", form = "Tablet", usage = "Günde 2-3 defa 1 tablet.", sideEffects = "Mide ağrısı", warnings = "Mide ülseri olanlarda kullanılmamalı", category = "Ağrı Kesici", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),
            MedicineEntity(name = "Voltaren", genericName = "Diklofenak", dosage = "75mg", form = "Enjeksiyon", usage = "Günde 1-2 defa.", sideEffects = "Ağrı", warnings = "Sağlık personeli uygulamalı", category = "Ağrı Kesici", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),
            MedicineEntity(name = "Majezik", genericName = "Deksketoprofen", dosage = "25mg", form = "Tablet", usage = "4-6 saat arayla 1 tablet.", sideEffects = "Baş ağrısı", warnings = "Maksimum 3 gün", category = "Ağrı Kesici", interactingDrugs = "Asetilsalisilik Asit"),
            MedicineEntity(name = "Calpol", genericName = "Parasetamol", dosage = "120mg/5ml", form = "Şurup", usage = "Kiloya göre.", sideEffects = "Alerji", warnings = "Doz takibi önemli", category = "Ağrı Kesici"),
            MedicineEntity(name = "Arveles", genericName = "Deksketoprofen", dosage = "25mg", form = "Tablet", usage = "8 saat arayla 1 tablet.", sideEffects = "Mide bulantısı", warnings = "Tok karnına", category = "Ağrı Kesici"),
            MedicineEntity(name = "Vermidon", genericName = "Parasetamol + Kafein", dosage = "500mg/30mg", form = "Tablet", usage = "Günde 3 defa 1 tablet.", sideEffects = "Çarpıntı, uykusuzluk", warnings = "Aşırı kafein tüketiminden kaçının", category = "Ağrı Kesici"),
            MedicineEntity(name = "Novalgine", genericName = "Metamizol Sodyum", dosage = "500mg", form = "Tablet", usage = "Günde 3-4 defa 1 tablet.", sideEffects = "Tansiyon düşüklüğü", warnings = "Uzun süre kullanılmamalı", category = "Ağrı Kesici"),
            MedicineEntity(name = "Dolorex", genericName = "Diklofenak Potasyum", dosage = "50mg", form = "Draje", usage = "Günde 2-3 defa 1 draje.", sideEffects = "Mide şikayetleri", warnings = "Kalp hastalarında dikkatli olunmalı", category = "Ağrı Kesici", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),

            // Antibiyotikler
            MedicineEntity(name = "Augmentin", genericName = "Amoksisilin + Klavulanik Asit", dosage = "1000mg", form = "Tablet", usage = "Günde 2 defa 1 tablet.", sideEffects = "İshal", warnings = "Kürü tamamlanmalı", category = "Antibiyotik", interactingDrugs = "Metotreksat"),
            MedicineEntity(name = "Cipro", genericName = "Siprofloksasin", dosage = "500mg", form = "Tablet", usage = "Günde 2 defa 1 tablet.", sideEffects = "Baş ağrısı", warnings = "18 yaş altı kullanmamalı", category = "Antibiyotik", interactingDrugs = "Tizanidin,Teofilin"),
            MedicineEntity(name = "Macrol", genericName = "Klaritromisin", dosage = "500mg", form = "Tablet", usage = "Günde 2 defa 1 tablet.", sideEffects = "Mide bulantısı, tat alma bozukluğu", warnings = "Karaciğer hastalarında dikkatli olunmalı", category = "Antibiyotik", interactingDrugs = "Simvastatin,Atorvastatin"),
            MedicineEntity(name = "Klamoks", genericName = "Amoksisilin + Klavulanik Asit", dosage = "1000mg", form = "Tablet", usage = "Günde 2 defa 1 tablet.", sideEffects = "İshal", warnings = "Alerji öyküsü olanlar dikkat etmeli", category = "Antibiyotik", interactingDrugs = "Metotreksat"),
            MedicineEntity(name = "Aksef", genericName = "Sefuroksim", dosage = "500mg", form = "Tablet", usage = "Günde 2 defa 1 tablet.", sideEffects = "Mide rahatsızlığı", warnings = "Böbrek yetmezliğinde doz ayarı gerekir", category = "Antibiyotik"),
            MedicineEntity(name = "Monurol", genericName = "Fosfomisin", dosage = "3g", form = "Şase", usage = "Tek doz, gece yatmadan önce.", sideEffects = "İshal, baş dönmesi", warnings = "Aç karnına alınmalı", category = "Antibiyotik"),

            // Kalp ve Tansiyon
            MedicineEntity(name = "Coraspin", genericName = "Asetilsalisilik Asit", dosage = "100mg", form = "Tablet", usage = "Günde 1 tablet.", sideEffects = "Kanama riski", warnings = "Ameliyat öncesi bırakılmalı", category = "Kalp ve Tansiyon", interactingDrugs = "İbuprofen,Klopidogrel,Diklofenak"),
            MedicineEntity(name = "Delix", genericName = "Ramipril", dosage = "5mg", form = "Tablet", usage = "Günde 1 defa 1 tablet.", sideEffects = "Kuru öksürük", warnings = "Hamilelikte kullanılmamalı", category = "Kalp ve Tansiyon", interactingDrugs = "İbuprofen,Diklofenak"),
            MedicineEntity(name = "Concor", genericName = "Bisoprolol", dosage = "5mg", form = "Tablet", usage = "Günde 1 defa.", sideEffects = "Yorgunluk", warnings = "Astım hastalarında dikkatli", category = "Kalp ve Tansiyon"),
            MedicineEntity(name = "Diovan", genericName = "Valsartan", dosage = "80mg", form = "Tablet", usage = "Günde 1 defa.", sideEffects = "Baş dönmesi", warnings = "Hamilelikte kullanılmamalı", category = "Kalp ve Tansiyon"),
            MedicineEntity(name = "Lipitor", genericName = "Atorvastatin", dosage = "10mg", form = "Tablet", usage = "Günde 1 defa, akşam.", sideEffects = "Kas ağrısı", warnings = "Düzenli karaciğer testi", category = "Kalp ve Tansiyon", interactingDrugs = "Klaritromisin"),
            MedicineEntity(name = "Vasilip", genericName = "Simvastatin", dosage = "20mg", form = "Tablet", usage = "Akşamları 1 tablet.", sideEffects = "Kas ve eklem ağrısı", warnings = "Greyfurt suyu ile içilmemeli", category = "Kalp ve Tansiyon", interactingDrugs = "Klaritromisin"),
            MedicineEntity(name = "Beloc", genericName = "Metoprolol", dosage = "50mg", form = "Tablet", usage = "Günde 1 veya 2 defa.", sideEffects = "Kalp atışında yavaşlama", warnings = "İlaç aniden kesilmemeli", category = "Kalp ve Tansiyon"),
            MedicineEntity(name = "Plavix", genericName = "Klopidogrel", dosage = "75mg", form = "Tablet", usage = "Günde 1 tablet.", sideEffects = "Kanama eğilimi artışı", warnings = "Mide kanaması riskine dikkat", category = "Kalp ve Tansiyon", interactingDrugs = "Asetilsalisilik Asit,İbuprofen,Omeprazol"),

            // Mide ve Sindirim Sistemi
            MedicineEntity(name = "Nexium", genericName = "Esomeprazol", dosage = "20mg", form = "Kapsül", usage = "Günde 1 defa, sabah aç.", sideEffects = "Baş ağrısı", warnings = "Uzun süreli kullanımda kemik erimesi riski", category = "Mide", interactingDrugs = "Klopidogrel"),
            MedicineEntity(name = "Lansor", genericName = "Lansoprazol", dosage = "30mg", form = "Kapsül", usage = "Sabah aç karnına 1 kapsül.", sideEffects = "İshal", warnings = "Doktor kontrolü ile", category = "Mide"),
            MedicineEntity(name = "Rennie", genericName = "Kalsiyum Karbonat + Magnezyum Karbonat", dosage = "680mg/80mg", form = "Çiğneme Tableti", usage = "Yemekten 1 saat sonra 1-2 tablet.", sideEffects = "Nadiren kabızlık", warnings = "Böbrek hastalarında dikkatli", category = "Mide"),
            MedicineEntity(name = "Talcid", genericName = "Hidrotalsit", dosage = "500mg", form = "Çiğneme Tableti", usage = "Mide yanması olunca 1-2 tablet çiğnenir.", sideEffects = "Hafif ishal", warnings = "Diğer ilaçlarla 2 saat ara verilmeli", category = "Mide"),
            MedicineEntity(name = "Pantpas", genericName = "Pantoprazol", dosage = "40mg", form = "Tablet", usage = "Sabah aç karnına 1 tablet.", sideEffects = "Gaz, şişkinlik", warnings = "Uzun süre kullanımı B12 eksikliği yapabilir", category = "Mide"),
            MedicineEntity(name = "Meteospasmyl", genericName = "Alverin Sitrat + Simetikon", dosage = "60mg/300mg", form = "Kapsül", usage = "Yemeklerden önce günde 2-3 defa.", sideEffects = "Nadir alerjik reaksiyon", warnings = "Karaciğer fonksiyonlarında bozukluk yapabilir", category = "Mide"),

            // Diyabet ve Metabolizma
            MedicineEntity(name = "Metformin", genericName = "Metformin HCl", dosage = "850mg", form = "Tablet", usage = "Yemekle birlikte 2-3 defa.", sideEffects = "Mide bulantısı", warnings = "Böbrek fonksiyonları kontrol", category = "Diyabet"),
            MedicineEntity(name = "Glifor", genericName = "Metformin", dosage = "1000mg", form = "Tablet", usage = "Yemekle birlikte 1-2 tablet.", sideEffects = "Gaz", warnings = "Karaciğer hastalığında kullanılmamalı", category = "Diyabet"),
            MedicineEntity(name = "Amaryl", genericName = "Glimepirid", dosage = "2mg", form = "Tablet", usage = "Sabah kahvaltıdan önce 1 tablet.", sideEffects = "Hipoglisemi", warnings = "Düzenli şeker takibi", category = "Diyabet"),
            MedicineEntity(name = "Diamicron", genericName = "Gliklazid", dosage = "60mg", form = "Tablet", usage = "Sabah kahvaltısı ile 1 tablet.", sideEffects = "Kan şekeri düşüklüğü", warnings = "Öğün atlanmamalı", category = "Diyabet"),
            MedicineEntity(name = "Euthyrox", genericName = "Levotiroksin", dosage = "100mcg", form = "Tablet", usage = "Sabah aç karnına 1 tablet.", sideEffects = "Çarpıntı", warnings = "Düzenli kan testi", category = "Tiroid", interactingDrugs = "Kalsiyum Karbonat"),
            MedicineEntity(name = "Levotiron", genericName = "Levotiroksin Sodyum", dosage = "50mcg", form = "Tablet", usage = "Sabah aç karnına 1 tablet.", sideEffects = "Sinirlilik, terleme", warnings = "Dozaj doktor tarafından belirlenmelidir", category = "Tiroid", interactingDrugs = "Kalsiyum Karbonat"),

            // Psikiyatri ve Sinir Sistemi
            MedicineEntity(name = "Xanax", genericName = "Alprazolam", dosage = "0.5mg", form = "Tablet", usage = "Doktor önerisi ile.", sideEffects = "Uyuşukluk", warnings = "Bağımlılık yapıcı", category = "Psikiyatri"),
            MedicineEntity(name = "Zoloft", genericName = "Sertralin", dosage = "50mg", form = "Tablet", usage = "Günde 1 defa, sabah.", sideEffects = "Bulantı", warnings = "Ani bırakılmamalı", category = "Psikiyatri", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),
            MedicineEntity(name = "Prozac", genericName = "Fluoksetin", dosage = "20mg", form = "Kapsül", usage = "Sabahları 1 kapsül.", sideEffects = "Uykusuzluk, iştah kaybı", warnings = "Etkisini göstermesi haftalar sürebilir", category = "Psikiyatri", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),
            MedicineEntity(name = "Cipralex", genericName = "Essitalopram", dosage = "10mg", form = "Tablet", usage = "Günde 1 defa 1 tablet.", sideEffects = "Sersemlik, ağız kuruluğu", warnings = "Alkol ile alınmamalı", category = "Psikiyatri", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),
            MedicineEntity(name = "Lustral", genericName = "Sertralin", dosage = "50mg", form = "Tablet", usage = "Günde 1 defa 1 tablet.", sideEffects = "Mide bulantısı", warnings = "18 yaşından küçüklerde dikkatli olunmalı", category = "Psikiyatri"),

            // Solunum Sistemi ve Alerji
            MedicineEntity(name = "Deltacortril", genericName = "Prednizolon", dosage = "5mg", form = "Tablet", usage = "Doktor önerisi ile.", sideEffects = "Kilo alımı", warnings = "Ani kesilmemeli", category = "Alerji ve Astım", interactingDrugs = "Asetilsalisilik Asit,İbuprofen"),
            MedicineEntity(name = "Zyrtec", genericName = "Setirizin", dosage = "10mg", form = "Tablet", usage = "Günde 1 defa 1 tablet.", sideEffects = "Uyku hali, yorgunluk", warnings = "Araç kullanırken dikkat edilmeli", category = "Alerji ve Astım"),
            MedicineEntity(name = "Crebros", genericName = "Levosetirizin", dosage = "5mg", form = "Tablet", usage = "Günde 1 tablet.", sideEffects = "Ağız kuruluğu", warnings = "Hamilelikte doktor kontrolünde", category = "Alerji ve Astım"),
            MedicineEntity(name = "Desmont", genericName = "Desloratadin + Montelukast", dosage = "5mg/10mg", form = "Tablet", usage = "Akşamları 1 tablet.", sideEffects = "Baş ağrısı, uykusuzluk", warnings = "Aç veya tok alınabilir", category = "Alerji ve Astım"),
            MedicineEntity(name = "Ventolin", genericName = "Salbutamol", dosage = "100mcg", form = "İnhaler", usage = "Nefes darlığında 1-2 puf.", sideEffects = "Titreme, çarpıntı", warnings = "Göz ile temasından kaçının", category = "Alerji ve Astım")
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
                put(COLUMN_CATEGORY, medicine.category)
                put(COLUMN_INTERACTING_DRUGS, medicine.interactingDrugs)
            }
            db.insert(TABLE_MEDICINES, null, values)
        }
    }

    companion object {
        private const val DATABASE_NAME = "medicine.db"
        private const val DATABASE_VERSION = 5 // Updated for dose tracker and interactions

        const val TABLE_MEDICINES = "medicines"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_GENERIC_NAME = "generic_name"
        const val COLUMN_DOSAGE = "dosage"
        const val COLUMN_FORM = "form"
        const val COLUMN_USAGE = "usage"
        const val COLUMN_SIDE_EFFECTS = "side_effects"
        const val COLUMN_WARNINGS = "warnings"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_INTERACTING_DRUGS = "interacting_drugs"

        // Scan History Tablosu Kolonları
        const val TABLE_SCAN_HISTORY = "scan_history"
        const val HISTORY_COL_ID = "id"
        const val HISTORY_COL_MEDICINE_NAME = "medicine_name"
        const val HISTORY_COL_SCAN_DATE = "scan_date"
        const val HISTORY_COL_IS_SUCCESS = "is_success"
        const val HISTORY_COL_RAW_TEXT = "raw_text"
        const val HISTORY_COL_SPEECH_OUTPUT = "speech_output"
        
        // Dose Tracker Tablosu Kolonları
        const val TABLE_DOSE_TRACKER = "dose_tracker"
        const val DOSE_COL_ID = "id"
        const val DOSE_COL_MEDICINE_NAME = "medicine_name"
        const val DOSE_COL_DATE = "date_epoch"
        const val DOSE_COL_IS_TAKEN = "is_taken"

        private const val CREATE_TABLE_MEDICINES = """
            CREATE TABLE $TABLE_MEDICINES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_GENERIC_NAME TEXT NOT NULL,
                $COLUMN_DOSAGE TEXT NOT NULL,
                $COLUMN_FORM TEXT NOT NULL,
                $COLUMN_USAGE TEXT NOT NULL,
                $COLUMN_SIDE_EFFECTS TEXT,
                $COLUMN_WARNINGS TEXT,
                $COLUMN_CATEGORY TEXT NOT NULL DEFAULT '',
                $COLUMN_INTERACTING_DRUGS TEXT NOT NULL DEFAULT ''
            )
        """

        private const val CREATE_TABLE_SCAN_HISTORY = """
            CREATE TABLE $TABLE_SCAN_HISTORY (
                $HISTORY_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $HISTORY_COL_MEDICINE_NAME TEXT NOT NULL,
                $HISTORY_COL_SCAN_DATE INTEGER NOT NULL,
                $HISTORY_COL_IS_SUCCESS INTEGER NOT NULL,
                $HISTORY_COL_RAW_TEXT TEXT,
                $HISTORY_COL_SPEECH_OUTPUT TEXT
            )
        """
        
        private const val CREATE_TABLE_DOSE_TRACKER = """
            CREATE TABLE $TABLE_DOSE_TRACKER (
                $DOSE_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $DOSE_COL_MEDICINE_NAME TEXT NOT NULL,
                $DOSE_COL_DATE INTEGER NOT NULL,
                $DOSE_COL_IS_TAKEN INTEGER NOT NULL DEFAULT 0
            )
        """
    }
}

package com.kadir.bitirme.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kadir.bitirme.R
import com.kadir.bitirme.data.model.ScanHistoryEntity
import com.kadir.bitirme.data.repository.ScanHistoryRepository
import com.kadir.bitirme.utils.tts.TextToSpeechManager

class HistoryActivity : AppCompatActivity() {

    private lateinit var repository: ScanHistoryRepository
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClear: android.widget.ImageButton
    private lateinit var btnBack: android.widget.ImageButton
    
    private val historyAdapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        repository = ScanHistoryRepository(this)
        ttsManager = TextToSpeechManager(this)

        initViews()
        loadHistory()
        
        ttsManager.speak("Tarama geçmişi ekranı açıldı. Listeden önceki taramalarınızı dinleyebilirsiniz.")
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewHistory)
        tvEmpty = findViewById(R.id.tvEmptyHistory)
        btnClear = findViewById(R.id.btnClearHistory)
        btnBack = findViewById(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = historyAdapter

        btnClear.setOnClickListener {
            showClearDialog()
        }
        
        btnBack.setOnClickListener {
            ttsManager.speak("Ana ekrana dönülüyor.")
            finish()
        }
    }

    private fun loadHistory() {
        val scans = repository.getAllScans()
        if (scans.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            btnClear.isEnabled = false
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            historyAdapter.submitList(scans)
            btnClear.isEnabled = true
        }
    }

    private fun showClearDialog() {
        AlertDialog.Builder(this)
            .setTitle("Geçmişi Temizle")
            .setMessage("Tüm tarama geçmişi silinecek. Emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                repository.clearHistory()
                loadHistory()
                ttsManager.speak("Geçmiş temizlendi.")
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
        ttsManager.shutdown()
    }

    // --- Adapter ---
    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        private var scans: List<ScanHistoryEntity> = emptyList()

        fun submitList(list: List<ScanHistoryEntity>) {
            this.scans = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scan_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(scans[position])
        }

        override fun getItemCount(): Int = scans.size

        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvIcon: TextView = itemView.findViewById(R.id.tvHistoryIcon)
            private val tvName: TextView = itemView.findViewById(R.id.tvHistoryName)
            private val tvDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
            private val btnPlay: android.widget.ImageButton = itemView.findViewById(R.id.btnPlaySpeech)

            fun bind(scan: ScanHistoryEntity) {
                tvIcon.text = if (scan.isSuccess) "✅" else "❌"
                tvName.text = scan.medicineName
                tvDate.text = repository.formatDate(scan.scanDate)

                // Tekrar dinlet
                btnPlay.setOnClickListener {
                    ttsManager.speak(scan.speechOutput)
                }
                
                // Card'a tıklayınca raw text'i göster
                itemView.setOnClickListener {
                    if (scan.rawText.isNotEmpty()) {
                        AlertDialog.Builder(itemView.context)
                            .setTitle("Okunan Metin")
                            .setMessage(scan.rawText)
                            .setPositiveButton("Kapat", null)
                            .show()
                    }
                }
            }
        }
    }
}

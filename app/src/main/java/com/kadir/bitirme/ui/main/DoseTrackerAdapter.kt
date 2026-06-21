package com.kadir.bitirme.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kadir.bitirme.R
import com.kadir.bitirme.data.model.DoseTrackerEntity

class DoseTrackerAdapter(
    private var doses: List<DoseTrackerEntity>,
    private val onDoseTakenChanged: (DoseTrackerEntity, Boolean) -> Unit
) : RecyclerView.Adapter<DoseTrackerAdapter.DoseViewHolder>() {

    class DoseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMedicineName: TextView = view.findViewById(R.id.tvMedicineName)
        val cbTaken: CheckBox = view.findViewById(R.id.cbTaken)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dose_tracker, parent, false)
        return DoseViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoseViewHolder, position: Int) {
        val dose = doses[position]
        holder.tvMedicineName.text = dose.medicineName
        
        // Listener'ı geçici olarak kaldır ki geri dönüşte tetiklenmesin
        holder.cbTaken.setOnCheckedChangeListener(null)
        holder.cbTaken.isChecked = dose.isTaken
        
        holder.cbTaken.setOnCheckedChangeListener { _, isChecked ->
            onDoseTakenChanged(dose, isChecked)
        }
        
        // Erişilebilirlik için TalkBack metni
        val statusText = if (dose.isTaken) "Alındı" else "Alınmadı"
        holder.itemView.contentDescription = "${dose.medicineName}, $statusText. Durumu değiştirmek için dokunun."
        holder.cbTaken.contentDescription = "${dose.medicineName} alındı olarak işaretle"
    }

    override fun getItemCount() = doses.size

    fun updateData(newDoses: List<DoseTrackerEntity>) {
        doses = newDoses
        notifyDataSetChanged()
    }
}

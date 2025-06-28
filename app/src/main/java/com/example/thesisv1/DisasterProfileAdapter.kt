import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesisv1.R
import com.example.thesisv1.data.DisasterProfile

class DisasterProfileAdapter(
    private val profiles: List<DisasterProfile>,
    private val onItemClick: (DisasterProfile) -> Unit,
    private val onItemLongClick: (DisasterProfile) -> Unit // <-- NEW
) : RecyclerView.Adapter<DisasterProfileAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.txtDisasterName)
        val type: TextView = itemView.findViewById(R.id.txtDisasterType)
        val date: TextView = itemView.findViewById(R.id.txtDisasterDate)
        val municipality: TextView = itemView.findViewById(R.id.txtMunicipality)
        val barangay: TextView = itemView.findViewById(R.id.txtBarangay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_disaster_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]
        holder.name.text = profile.name
        holder.type.text = "Type: ${profile.type}"
        holder.date.text = "Date: ${profile.dateTime}"
        holder.municipality.text = "Municipality: ${profile.municipality}"
        holder.barangay.text = "Barangay: ${profile.barangay}"

        holder.itemView.setOnClickListener { onItemClick(profile) }

        // Long click
        holder.itemView.setOnLongClickListener {
            onItemLongClick(profile)
            true
        }
    }

    override fun getItemCount() = profiles.size
}

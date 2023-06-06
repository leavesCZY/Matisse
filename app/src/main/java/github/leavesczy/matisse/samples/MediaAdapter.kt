package github.leavesczy.matisse.samples

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import github.leavesczy.matisse.MediaResource

/**
 * @Author: CZY
 * @Date: 2022/6/2 15:12
 * @Desc:
 */
class MediaAdapter(private val mediaResources: List<MediaResource>) :
    RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mediaResources[position]
        holder.ivImage.load(item.uri)
        holder.tvDetail.text = item.toString()
    }

    override fun getItemCount(): Int = mediaResources.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        val tvDetail: TextView = itemView.findViewById(R.id.tvDetail)
    }

}
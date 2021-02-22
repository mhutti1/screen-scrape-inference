package xyz.hutt.meng

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ModelAdapter(
  private val dataset: Array<Processors>,
  val callback: (Processors, Boolean) -> Unit
) :
  RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

  class ModelViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
    val textView = LayoutInflater.from(parent.context)
      .inflate(R.layout.list_item, parent, false) as TextView

    return ModelViewHolder(textView)
  }

  override fun getItemCount(): Int {
    return dataset.size
  }

  override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
    holder.textView.text = dataset[position].getName()
    holder.textView.setOnClickListener {
      callback(dataset[position], false)
    }
    holder.textView.setOnLongClickListener {
      callback(dataset[position], true)
      true
    }
  }
}

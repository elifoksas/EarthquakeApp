package com.elifoksas.earthquake.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elifoksas.earthquake.data.entity.EmergencyItem
import com.elifoksas.earthquake.databinding.EmergencyItemBinding

    class EmergencyAdapter(var mContext: Context , var emergencyList : List<EmergencyItem>, private val listener:OnItemClickListener) :
    RecyclerView.Adapter<EmergencyAdapter.EmergencyFragmentItemHolder>() {

    inner class EmergencyFragmentItemHolder(var item: EmergencyItemBinding) : RecyclerView.ViewHolder(item.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyFragmentItemHolder {
        val binding = EmergencyItemBinding.inflate(LayoutInflater.from(mContext),parent,false)
        return EmergencyFragmentItemHolder(binding)
    }

    override fun onBindViewHolder(holder: EmergencyFragmentItemHolder, position: Int) {
        val binding = holder.item
        val item = emergencyList[position]

        binding.imageView.setImageResource(item.emergencyPicture)
        binding.textView.text = item.emergencyName

       holder.itemView.setOnClickListener{
           listener.onItemClick(item)
       }
    }

    override fun getItemCount(): Int {
        return emergencyList.size
    }
        interface OnItemClickListener {
            fun onItemClick(item: EmergencyItem)
        }


}
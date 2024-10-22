package com.techgeeksclub.earthquake.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techgeeksclub.earthquake.data.entity.InformationItem
import com.techgeeksclub.earthquake.databinding.InformationItemBinding

class InformationAdapter (var mContext: Context, var informationList : List<InformationItem>) : RecyclerView.Adapter<InformationAdapter.InformationFragmentViewHolder>(){

    inner class InformationFragmentViewHolder(var item: InformationItemBinding) : RecyclerView.ViewHolder(item.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InformationFragmentViewHolder {
        val binding = InformationItemBinding.inflate(LayoutInflater.from(mContext),parent,false)
        return InformationFragmentViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return  informationList.size
    }

    override fun onBindViewHolder(holder: InformationFragmentViewHolder, position: Int) {
        val binding = holder.item
        val  item = informationList[position]

        binding.informationImgView.setImageResource(item.informationPic)
        binding.informationTV.text = item.informationText
    }
}
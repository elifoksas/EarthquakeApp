package com.elifoksas.earthquake.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elifoksas.earthquake.data.entity.InformationItem
import com.elifoksas.earthquake.databinding.InformationItemBinding

class InformationAdapter(
    private val informationList: List<InformationItem>
) : RecyclerView.Adapter<InformationAdapter.InformationFragmentViewHolder>() {

    class InformationFragmentViewHolder(
        val binding: InformationItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InformationFragmentViewHolder {
        val binding = InformationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InformationFragmentViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return informationList.size
    }

    override fun onBindViewHolder(holder: InformationFragmentViewHolder, position: Int) {
        val item = informationList[position]

        with(holder.binding) {
            stepNumber.text = item.stepNumber.toString()
            informationImgView.setImageResource(item.informationPic)
            informationTitle.text = item.title
            informationDescription.text = item.description
        }
    }
}

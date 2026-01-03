package com.xboard.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xboard.R

class PlanFeatureAdapter(
    private var features: List<String> = emptyList()
) : RecyclerView.Adapter<PlanFeatureAdapter.FeatureViewHolder>() {

    fun updateFeatures(newFeatures: List<String>) {
        features = newFeatures
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.xboard.R.layout.item_plan_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val featureText: TextView =
            itemView.findViewById(R.id.tv_feature_text)

        fun bind(feature: String) {
            featureText.text = feature
        }
    }
}
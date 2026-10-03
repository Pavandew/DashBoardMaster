package com.restroone.app.manager_single_res_dash.settings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.restroone.app.databinding.ItemTermsClauseBinding

data class TermsClauseModel(
    val id: Int,
    val title: String,
    val content: String
)

class TermsClauseAdapter(
    private val clauses: List<TermsClauseModel>
) : RecyclerView.Adapter<TermsClauseAdapter.TermsViewHolder>() {

    inner class TermsViewHolder(val binding: ItemTermsClauseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(clause: TermsClauseModel) {
            binding.tvClauseTitle.text = clause.title
            binding.tvClauseContent.text = clause.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermsViewHolder {
        val binding = ItemTermsClauseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TermsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TermsViewHolder, position: Int) {
        holder.bind(clauses[position])
    }

    override fun getItemCount(): Int = clauses.size
}

package com.haoze.claudekeyboard.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.haoze.claudekeyboard.R

/**
 * RecyclerView adapter for the settings-style home page.
 * Supports two view types: section headers and clickable entries.
 */
class HomeAdapter(
    private val onEntryClick: (HomeItem.Entry) -> Unit
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(HomeItemDiffCallback()) {

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.Section -> TYPE_SECTION
            is HomeItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> {
                val view = inflater.inflate(R.layout.item_home_section, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_home_entry, parent, false)
                EntryViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeItem.Section -> (holder as SectionViewHolder).bind(item)
            is HomeItem.Entry -> (holder as EntryViewHolder).bind(item)
        }
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_section_title)

        fun bind(section: HomeItem.Section) {
            tvTitle.text = section.title
        }
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle)

        fun bind(entry: HomeItem.Entry) {
            ivIcon.setImageResource(entry.iconRes)
            tvTitle.text = entry.title
            tvSubtitle.text = entry.subtitle
            itemView.setOnClickListener { onEntryClick(entry) }
        }
    }

    /**
     * Update the subtitle of an entry by its id.
     */
    fun updateSubtitle(entryId: String, newSubtitle: String) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it is HomeItem.Entry && it.id == entryId }
        if (index >= 0) {
            val entry = currentList[index] as HomeItem.Entry
            currentList[index] = entry.copy(subtitle = newSubtitle)
            submitList(currentList)
        }
    }

    private class HomeItemDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return when {
                oldItem is HomeItem.Section && newItem is HomeItem.Section ->
                    oldItem.title == newItem.title
                oldItem is HomeItem.Entry && newItem is HomeItem.Entry ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }
}

package com.haoze.claudekeyboard.ui.macro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.macro.Macro

/**
 * Adapter for displaying macro buttons in a RecyclerView.
 */
class MacroButtonAdapter(
    private val onMacroClick: (Macro) -> Unit,
    private val onMacroLongClick: (Macro) -> Unit
) : ListAdapter<Macro, MacroButtonAdapter.MacroViewHolder>(MacroDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_macro_button, parent, false)
        return MacroViewHolder(view)
    }

    override fun onBindViewHolder(holder: MacroViewHolder, position: Int) {
        val macro = getItem(position)
        holder.bind(macro)
    }

    inner class MacroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: MaterialButton = itemView.findViewById(R.id.btn_macro)

        fun bind(macro: Macro) {
            button.text = macro.label

            button.setOnClickListener {
                onMacroClick(macro)
            }

            button.setOnLongClickListener {
                onMacroLongClick(macro)
                true
            }
        }
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates.
     */
    class MacroDiffCallback : DiffUtil.ItemCallback<Macro>() {
        override fun areItemsTheSame(oldItem: Macro, newItem: Macro): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Macro, newItem: Macro): Boolean {
            return oldItem == newItem
        }
    }
}

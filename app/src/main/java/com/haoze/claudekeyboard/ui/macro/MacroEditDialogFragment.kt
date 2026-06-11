package com.haoze.claudekeyboard.ui.macro

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.DialogFragment
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.macro.Macro

/**
 * Dialog fragment for editing/creating custom macros.
 */
class MacroEditDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_MACRO_ID = "macro_id"
        private const val ARG_MACRO_LABEL = "macro_label"
        private const val ARG_MACRO_COMMAND = "macro_command"
        private const val ARG_MACRO_SEND_ENTER = "macro_send_enter"
        private const val ARG_IS_EDIT = "is_edit"

        /**
         * Create a new instance for adding a macro.
         */
        fun newInstance(): MacroEditDialogFragment {
            return MacroEditDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EDIT, false)
                }
            }
        }

        /**
         * Create a new instance for editing an existing macro.
         */
        fun newInstance(macro: Macro): MacroEditDialogFragment {
            return MacroEditDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EDIT, true)
                    putString(ARG_MACRO_ID, macro.id)
                    putString(ARG_MACRO_LABEL, macro.label)
                    putString(ARG_MACRO_COMMAND, macro.command)
                    putBoolean(ARG_MACRO_SEND_ENTER, macro.sendEnter)
                }
            }
        }
    }

    private var onSaveListener: ((String?, String, String, Boolean) -> Unit)? = null
    private var onDeleteListener: ((String) -> Unit)? = null

    /**
     * Set the save listener.
     * @param listener Callback with (id?, label, command, sendEnter) - id is null for new macros
     */
    fun setOnSaveListener(listener: (String?, String, String, Boolean) -> Unit) {
        onSaveListener = listener
    }

    /**
     * Set the delete listener.
     * @param listener Callback with macro id
     */
    fun setOnDeleteListener(listener: (String) -> Unit) {
        onDeleteListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isEdit = arguments?.getBoolean(ARG_IS_EDIT) ?: false
        val macroId = arguments?.getString(ARG_MACRO_ID)
        val macroLabel = arguments?.getString(ARG_MACRO_LABEL) ?: ""
        val macroCommand = arguments?.getString(ARG_MACRO_COMMAND) ?: ""
        val macroSendEnter = arguments?.getBoolean(ARG_MACRO_SEND_ENTER) ?: false

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_macro_edit, null)

        val etLabel = view.findViewById<EditText>(R.id.et_macro_label)
        val etCommand = view.findViewById<EditText>(R.id.et_macro_command)
        val switchSendEnter = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_send_enter)

        // Pre-fill if editing
        if (isEdit) {
            etLabel.setText(macroLabel)
            etCommand.setText(macroCommand)
            switchSendEnter.isChecked = macroSendEnter
        }

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) R.string.dialog_edit_macro else R.string.dialog_add_macro)
            .setView(view)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val label = etLabel.text.toString().trim()
                val command = etCommand.text.toString().trim()
                val sendEnter = switchSendEnter.isChecked

                if (label.isNotEmpty() && command.isNotEmpty()) {
                    onSaveListener?.invoke(macroId, label, command, sendEnter)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)

        // Add delete button for editing existing custom macros
        if (isEdit && macroId != null) {
            dialogBuilder.setNeutralButton(R.string.dialog_delete) { _, _ ->
                onDeleteListener?.invoke(macroId)
            }
        }

        return dialogBuilder.create()
    }
}
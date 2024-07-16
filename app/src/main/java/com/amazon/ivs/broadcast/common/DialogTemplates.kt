package com.amazon.ivs.broadcast.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.children
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.databinding.DialogCameraOptionsBinding
import com.amazon.ivs.broadcast.databinding.ItemOptionBinding
import com.amazon.ivs.broadcast.models.ui.DeviceItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun View.showCameraDialog(
    title: String,
    devices: List<DeviceItem>?,
    onValueSet: (value: DeviceItem) -> Unit
) {
    var selectedDevice: DeviceItem? = null
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val dialogBinding = DialogCameraOptionsBinding.inflate(inflater)

    devices?.forEach { option ->
        val buttonBinding = ItemOptionBinding.inflate(inflater)
        val viewId = View.generateViewId()
        buttonBinding.root.id = viewId
        option.viewId = viewId
        buttonBinding.optionItem = option
        dialogBinding.radioGroup.addView(buttonBinding.root)
        if (option.isSelected) {
            dialogBinding.radioGroup.check(buttonBinding.root.id)
            selectedDevice = option
        }
    }

    dialogBinding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
        dialogBinding.radioGroup.children.forEach { radioButton ->
            if (radioButton.id == checkedId) {
                selectedDevice = devices?.first { it.viewId == radioButton.id }
            }
        }
    }

    MaterialAlertDialogBuilder(this.context, R.style.AlertDialog)
        .setView(dialogBinding.root)
        .setTitle(title)
        .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
            selectedDevice?.let { option ->
                onValueSet(option)
            }
        }
        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

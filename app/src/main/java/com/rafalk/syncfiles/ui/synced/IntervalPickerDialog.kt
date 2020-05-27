package com.rafalk.syncfiles.ui.synced

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment

import com.rafalk.syncfiles.R
import timber.log.Timber

/**
 * A simple [Fragment] subclass.
 * Use the [IntervalPickerDialog.newInstance] factory method to
 * create an instance of this fragment.
 */
class IntervalPickerDialog : DialogFragment() {
    internal lateinit var listener: IntervalPickerDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.fragment_interval_picker_dialog, null)
            view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                dialog.cancel()
            }
            view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
                val number = view.findViewById<EditText>(R.id.editText)
                if (number.text.toString() == "") {
                    number.error = "Wrong number!"
                } else {
                    var multiplier: Long = 1000
                    val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
                    when (radioGroup.checkedRadioButtonId) {
                        R.id.secondsRadioButton -> {
                            Timber.d("seconds")
                        }
                        R.id.minutesRadioButton -> {
                            Timber.d("minutes")
                            multiplier *= 60
                        }
                        R.id.hoursRadioButton -> {
                            Timber.d("hours")
                            multiplier *= 60 * 60
                        }
                        R.id.daysRadioButton -> {
                            Timber.d("days")
                            multiplier *= 24 * 60 * 60
                        }
                    }
                    listener.onConfirmation(number.text.toString().toLong() * multiplier)
                    dialog.cancel()
                }
            }


            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    interface IntervalPickerDialogListener {
        fun onConfirmation(interval: Long)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as IntervalPickerDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (context.toString() +
                        " must implement IntervalPickerDialogListener")
            )
        }
    }


}

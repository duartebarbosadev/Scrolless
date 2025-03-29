package com.scrolless.app.features.dialogs

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogHelpBinding
import timber.log.Timber
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint

/**
 * DialogFragment that explains how to use the app and troubleshoot common issues.
 */
@AndroidEntryPoint
class HelpDialogFragment : DialogFragment() {

    private var _binding: DialogHelpBinding? = null
    private val binding get() = _binding!!

    companion object {
        val TAG: String = HelpDialogFragment::class.java.simpleName

        /**
         * Factory method to create a new instance of this fragment.
         */
        fun newInstance(): HelpDialogFragment {
            return HelpDialogFragment()
        }

        /**
         * Helper method to show the dialog.
         *
         * @param fragmentManager The FragmentManager to use for displaying the dialog.
         */
        fun show(fragmentManager: FragmentManager) {
            val dialog = newInstance()
            dialog.show(fragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogHelpBinding.inflate(layoutInflater)

        // Setup interactive elements within the dialog content
        setupContentInteractions()
        // Setup the main action buttons
        setupActionButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()
    }

    override fun onStart() {
        super.onStart()

        // Make sure that the dialog takes up the full width of the screen
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    /**
     * Sets up click listeners for interactive elements within the main content area.
     */
    private fun setupContentInteractions() {
        binding.cardGithub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = getString(R.string.github_url).toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                requireContext().startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.e(e, "No application found to handle GitHub URL.")
            }
        }
    }

    /**
     * Sets up click listeners for the main action buttons at the bottom of the dialog.
     */
    private fun setupActionButtons() {

        binding.btnGoToAccessibility.setOnClickListener {
            try {
                val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                requireContext().startActivity(intent)
                dismiss()
            } catch (e: ActivityNotFoundException) {
                Timber.e(e, "Accessibility Settings screen not found.")
            }
        }

        binding.btnCloseHelp.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("$TAG onDestroyView: Binding cleared.")
    }
}
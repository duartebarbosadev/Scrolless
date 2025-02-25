/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.binding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

open class BindingFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null

    protected var isPaused = true

    val binding: VB
        get() =
            _binding
                ?: throw RuntimeException("Should only use binding after onCreateView and before onDestroyView")

    protected fun requireBinding(): VB = requireNotNull(_binding)

    open fun onCreateView() {}

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = getBinding(inflater, container)
        onCreateView()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        isPaused = true
        super.onPause()
    }

    override fun onResume() {
        isPaused = false
        super.onResume()
    }
}

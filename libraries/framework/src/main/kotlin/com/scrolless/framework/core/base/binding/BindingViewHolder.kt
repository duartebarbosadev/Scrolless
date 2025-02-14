/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.binding

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A Simple [BindingViewHolder] providing easier support for ViewBinding
 */
open class BindingViewHolder<VB : ViewBinding>(
    val binding: VB
) : RecyclerView.ViewHolder(binding.root) {
    val context: Context = binding.root.context
}

/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.adapter

import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.binding.BindingViewHolder

/**
 * A Simple [BasicViewHolder] providing easier support for ViewBinding
 */
class BasicViewHolder<VB : ViewBinding>(binding: VB) : BindingViewHolder<VB>(binding)

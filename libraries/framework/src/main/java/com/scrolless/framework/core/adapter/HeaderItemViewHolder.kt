/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.adapter

import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.binding.BindingViewHolder

sealed class HeaderItemViewHolder<VB : ViewBinding>(binding: VB) : BindingViewHolder<VB>(binding) {
    class HeaderViewHolder<HeaderBinding : ViewBinding>(
        binding: HeaderBinding
    ) : HeaderItemViewHolder<HeaderBinding>(binding)

    class ItemViewHolder<ItemBinding : ViewBinding>(
        binding: ItemBinding
    ) : HeaderItemViewHolder<ItemBinding>(binding)
}

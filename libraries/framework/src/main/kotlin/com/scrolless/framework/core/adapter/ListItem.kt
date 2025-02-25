/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.adapter

sealed class ListItem<out H, out I> {
    data class Header<H>(val data: H) : ListItem<H, Nothing>()
    data class Item<I>(val data: I) : ListItem<Nothing, I>()
}

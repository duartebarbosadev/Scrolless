/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.adapter.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil

abstract class BasePagingListAdapter<T : Any>(
    callback: DiffUtil.ItemCallback<T>,
    private val clickListener: (T) -> Unit
) : PagingDataAdapter<T, BasePagingViewHolder<T>>(callback) {

    abstract fun createViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): BasePagingViewHolder<T>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasePagingViewHolder<T> =
        createViewHolder(
            LayoutInflater.from(parent.context),
            parent,
            viewType,
        )

    override fun onBindViewHolder(holder: BasePagingViewHolder<T>, position: Int) {
        getItem(position)?.also { item ->
            holder.itemView.setOnClickListener { _ ->
                clickListener(item)
            }
            holder.bind(item)
        }
    }
}

/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

@SuppressWarnings("TooManyFunctions")
abstract class BasicRecyclerAdapter<T : Any, Binding : ViewBinding> :
    RecyclerView.Adapter<BasicViewHolder<Binding>>() {
    private var items: MutableList<T> = mutableListOf()

    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup): Binding

    abstract fun bindView(binding: Binding, position: Int, item: T)

    /** Generates a ViewHolder from this Item with the given ViewBinding */
    open fun getViewHolder(viewBinding: Binding): BasicViewHolder<Binding> =
        BasicViewHolder(viewBinding)

    /** Generates a ViewHolder from this Item with the given parent */
    private fun getViewHolder(parent: ViewGroup): BasicViewHolder<Binding> =
        getViewHolder(createBinding(LayoutInflater.from(parent.context), parent))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicViewHolder<Binding> =
        getViewHolder(parent)

    override fun onBindViewHolder(holder: BasicViewHolder<Binding>, position: Int) {
        bindView(holder.binding, position, item = getItem(position))
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): T = items[position]

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(list: List<T>) {
        this.items.clear()
        this.items.addAll(list)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUpdatedItems(list: MutableList<T>) {
        this.items = list
        notifyDataSetChanged()
    }

    fun insertItem(data: T, position: Int? = null) {
        if (position != null) {
            this.items.add(position, data)
            notifyItemInserted(position)
        } else {
            this.items.add(data)
            notifyItemInserted(this.items.size - 1)
        }
    }

    fun updateItem(data: T, position: Int) {
        this.items[position] = data
        notifyItemChanged(position)
    }

    fun updateItems(data: List<T>, startIndex: Int) {
        var start = startIndex
        for (i in data.indices) {
            if (start >= this.items.size) {
                this.items.add(data[i])
            } else {
                this.items[start] = data[i]
            }
            start++
        }
        notifyItemRangeChanged(startIndex, data.size)
    }

    fun insertItems(data: List<T>, position: Int? = null) {
        if (position != null) {
            this.items.addAll(position, data)
            notifyItemRangeInserted(position, data.size)
        } else {
            val index = this.items.size - 1
            this.items.addAll(data)
            notifyItemRangeInserted(index, this.items.size - 1)
        }
    }

    fun removeItems(startIndex: Int, endIndex: Int = this.items.size - 1) {
        val iterator = this.items.listIterator(startIndex)
        var end = endIndex
        while (iterator.hasNext()) {
            iterator.next()
            if (startIndex <= end) {
                iterator.remove()
                end--
            } else {
                break
            }
        }
        notifyItemRangeRemoved(startIndex, endIndex - startIndex)
    }

    fun removeItem(index: Int) {
        if (index < items.size) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateLastItem(data: T) {
        this.items[this.items.size - 1] = data
        notifyItemChanged(this.items.size - 1)
    }
}

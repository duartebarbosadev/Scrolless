/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.adapter.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scrolless.libraries.components.databinding.RowPagingLoadStateBinding

class PagingLoadStateAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    private val adapter: PagingDataAdapter<T, VH>
) : LoadStateAdapter<PagingLoadStateAdapter.PagingLoadStateViewHolder>() {

    class PagingLoadStateViewHolder(
        private val binding: RowPagingLoadStateBinding,
        private val retryCallback: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnRetry.setOnClickListener { retryCallback() }
        }

        fun bind(loadState: LoadState) {
            binding.pbLoading.isVisible = loadState is LoadState.Loading
            binding.btnRetry.isVisible = loadState is LoadState.Error
            binding.tvErrorMessage.isVisible =
                !(loadState as? LoadState.Error)?.error?.message.isNullOrBlank()
            binding.tvErrorMessage.text = (loadState as? LoadState.Error)?.error?.message
        }
    }

    override fun onBindViewHolder(holder: PagingLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): PagingLoadStateViewHolder =
        PagingLoadStateViewHolder(
            RowPagingLoadStateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        ) {
            adapter.retry()
        }
}

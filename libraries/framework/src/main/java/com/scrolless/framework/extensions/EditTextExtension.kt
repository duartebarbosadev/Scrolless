/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun EditText.setDebouncedTextWatcher(
    debounceTime: Long = 300L,
    lifecycleScope: LifecycleCoroutineScope,
    onDebouncedTextChange: (String) -> Unit
) {
    var searchDebounceJob: Job? = null

    this.addTextChangedListener(
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchDebounceJob?.cancel()
                searchDebounceJob = lifecycleScope.launch {
                    delay(debounceTime)
                    onDebouncedTextChange(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        },
    )
}

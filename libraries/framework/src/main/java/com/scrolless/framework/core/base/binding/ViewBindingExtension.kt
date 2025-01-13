/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.binding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

internal fun <V : ViewBinding> Class<*>.getBinding(layoutInflater: LayoutInflater): V =
    try {
        @Suppress("UNCHECKED_CAST")
        getMethod(
            "inflate",
            LayoutInflater::class.java,
        ).invoke(null, layoutInflater) as V
    } catch (ex: Exception) {
        throw RuntimeException("The ViewBinding inflate function has been changed.", ex)
    }

internal fun <V : ViewBinding> Class<*>.getBinding(
    layoutInflater: LayoutInflater,
    container: ViewGroup?
): V = try {
    @Suppress("UNCHECKED_CAST")
    getMethod(
        "inflate",
        LayoutInflater::class.java,
        ViewGroup::class.java,
        Boolean::class.java,
    ).invoke(null, layoutInflater, container, false) as V
} catch (ex: Exception) {
    throw RuntimeException("The ViewBinding inflate function has been changed.", ex)
}

internal fun Class<*>.checkMethod(): Boolean = try {
    getMethod(
        "inflate",
        LayoutInflater::class.java,
    )
    true
} catch (ex: Exception) {
    false
}

internal fun Any.findClass(): Class<*> {
    var javaClass: Class<*> = this.javaClass
    var result: Class<*>? = null
    while (result == null || !result.checkMethod()) {
        result = (javaClass.genericSuperclass as? ParameterizedType)
            ?.actualTypeArguments?.firstOrNull {
                if (it is Class<*>) {
                    it.checkMethod()
                } else {
                    false
                }
            } as? Class<*>
        javaClass = javaClass.superclass
    }
    return result
}

inline fun <reified V : ViewBinding> ViewGroup.toBinding(): V = V::class.java.getMethod(
    "inflate",
    LayoutInflater::class.java,
    ViewGroup::class.java,
    Boolean::class.java,
).invoke(null, LayoutInflater.from(context), this, false) as V

internal fun <V : ViewBinding> BindingActivity<V>.getBinding(): V =
    findClass().getBinding(layoutInflater)

internal fun <V : ViewBinding> BindingFragment<V>.getBinding(
    inflater: LayoutInflater,
    container: ViewGroup?
): V = findClass().getBinding(inflater, container)

internal fun <V : ViewBinding> BindingSheetDialog<V>.getBinding(
    inflater: LayoutInflater,
    container: ViewGroup?
): V = findClass().getBinding(inflater, container)

internal fun <V : ViewBinding> BindingComponent<V>.getBinding(
    inflater: LayoutInflater,
    container: ViewGroup?
): V = findClass().getBinding(inflater, container)

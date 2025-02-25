/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.navigation

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.scrolless.libraries.framework.R
import kotlin.reflect.KClass

fun FragmentActivity.showFragNav(builder: FragNav) {
    var fm = supportFragmentManager

    if (builder.manager != null) {
        fm = builder.manager
    }

    val ft = fm.beginTransaction()

    if (builder.clearBackStack) {
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    if (builder.dialogFragment != null) {
        builder.dialogFragment.show(fm, null)
    } else {
        val fragment = builder.fragment ?: return
        val tag = builder.tag
        val containerId = builder.viewId

        if (builder.animationType !== AnimationType.NO_ANIM) {
            val anim = AnimationType.getAnimation(builder.animationType)
            ft.setCustomAnimations(anim[0], anim[1], anim[2], anim[3])
        }

        if (builder.addToBackStack) {
            ft.addToBackStack(tag)
        }

        when (builder.transitionType) {
            TransitionType.ADD -> ft.add(containerId, fragment, tag)
            TransitionType.SHOW -> ft.show(fragment)
            TransitionType.HIDE -> ft.hide(fragment)
            else -> ft.replace(containerId, fragment, tag)
        }

        ft.commitAllowingStateLoss()
    }
}

fun FragmentActivity.navigateFragment(
    fragment: Fragment,
    addToBackStack: Boolean = true,
    fragmentManagerEnable: Boolean = false,
    clearBackStack: Boolean = false,
    @IdRes viewId: Int = R.id.nav_container,
    transitionType: TransitionType = TransitionType.REPLACE,
    animation: AnimationType = AnimationType.ENTER_FROM_RIGHT
) {
    val frag = FragNav.create {
        setFragment(fragment)
        setFragmentManager(if (fragmentManagerEnable) supportFragmentManager else null)
        setAddToBackStack(addToBackStack)
        setClearBackStack(clearBackStack)
        setViewId(viewId)
        setTransitionType(transitionType)
        setAnimation(animation)
    }

    showFragNav(frag)
}

fun Fragment.navigateFragment(
    fragment: Fragment,
    addToBackStack: Boolean = true,
    fragmentManagerEnable: Boolean = false,
    clearBackStack: Boolean = false,
    @IdRes viewId: Int = R.id.nav_container,
    transitionType: TransitionType = TransitionType.REPLACE,
    animation: AnimationType = AnimationType.NO_ANIM
) {
    val fragNav = FragNav.create {
        setFragment(fragment)
        setFragmentManager(if (fragmentManagerEnable) childFragmentManager else null)
        setAddToBackStack(addToBackStack)
        setClearBackStack(clearBackStack)
        setViewId(viewId)
        setTransitionType(transitionType)
        setAnimation(animation)
    }

    activity?.showFragNav(fragNav)
}

fun FragmentActivity.navigateFragment(
    className: String,
    addToBackStack: Boolean = true,
    fragmentManagerEnable: Boolean = false,
    clearBackStack: Boolean = false,
    @IdRes viewId: Int = R.id.nav_container,
    transitionType: TransitionType = TransitionType.REPLACE,
    animation: AnimationType = AnimationType.ENTER_FROM_RIGHT
) {
    val frag = FragNav.create {
        setFragment(classFragment(className))
        setFragmentManager(if (fragmentManagerEnable) supportFragmentManager else null)
        setAddToBackStack(addToBackStack)
        setClearBackStack(clearBackStack)
        setViewId(viewId)
        setTransitionType(transitionType)
        setAnimation(animation)
    }

    showFragNav(frag)
}

fun Fragment.navigateFragment(
    className: String,
    addToBackStack: Boolean = true,
    fragmentManagerEnable: Boolean = false,
    clearBackStack: Boolean = false,
    @IdRes viewId: Int = R.id.nav_container,
    transitionType: TransitionType = TransitionType.REPLACE,
    animation: AnimationType = AnimationType.ENTER_FROM_RIGHT
) {
    val fragNav = FragNav.create {
        setFragment(classFragment(className))
        setFragmentManager(if (fragmentManagerEnable) childFragmentManager else null)
        setAddToBackStack(addToBackStack)
        setClearBackStack(clearBackStack)
        setViewId(viewId)
        setTransitionType(transitionType)
        setAnimation(animation)
    }

    requireActivity().showFragNav(fragNav)
}

/*
 **************************************************************
 **************************************************************
 */

fun FragmentActivity.showDialog() {
    val fragNav = FragNav.create { setFragment(DialogFragment()) }
    showFragNav(fragNav)
}

fun FragmentActivity.returnPage(fragment: Fragment, key: String? = null, bundle: Bundle? = null) {
    val fm = supportFragmentManager
    if (key != null && bundle != null) {
        fm.setFragmentResult(key, bundle)
    }
    fm.popBackStack(fragment.tag, 0)
}

fun Fragment.returnPage(fragment: Fragment, key: String? = null, bundle: Bundle? = null) {
    requireActivity().returnPage(fragment, key, bundle)
}

fun FragmentActivity.returnPage(className: String) {
    val fm = supportFragmentManager
    fm.popBackStack(classFragment(className).tag, 0)
}

fun Fragment.returnPage(className: String) {
    requireActivity().returnPage(className)
}

fun Fragment.replaceFragment(
    fragment: Fragment,
    backStack: Boolean = true,
    @IdRes containerId: Int = R.id.nav_container
) = activity?.replaceFragment(fragment, backStack, containerId)

fun FragmentActivity.replaceFragment(
    fragment: Fragment,
    backStack: Boolean = true,
    @IdRes containerId: Int = R.id.nav_container
) = supportFragmentManager
    .beginTransaction()
    .replace(containerId, fragment)
    .also { if (backStack) it.addToBackStack(null) }
    .commit()

fun FragmentActivity.returnPage(clazz: KClass<out Fragment>) {
    val fm = supportFragmentManager
    fm.popBackStack(clazz.java.simpleName, 0)
}

fun Fragment.startActivity(
    flags: Int = -1,
    bundle: Bundle? = null,
    clazz: Class<out AppCompatActivity>? = null
) {
    activity?.startActivity(flags, bundle, clazz)
}

fun FragmentActivity.startActivity(
    flags: Int = -1,
    bundle: Bundle? = null,
    clazz: Class<out AppCompatActivity>? = null
) {
    val intent = Intent(this, clazz)
    if (flags != -1) {
        intent.flags = flags
    }
    if (bundle != null) {
        intent.putExtras(bundle)
    }
    startActivity(intent)
}

fun FragmentActivity.startActivity(
    packageName: String,
    className: String,
    flags: Int = -1,
    bundle: Bundle? = null
) {
    val intent = Intent(Intent.ACTION_VIEW).setClassName(packageName, className)
    if (flags != -1) {
        intent.flags = flags
    }
    if (bundle != null) {
        intent.putExtras(bundle)
    }
    startActivity(intent)
}

fun Fragment.startActivity(
    packageName: String,
    className: String,
    flags: Int = -1,
    bundle: Bundle? = null
) {
    activity?.startActivity(packageName, className, flags, bundle)
}

/**
 * val fragment = supportFragmentManager.instantiate(fragmentClassName)
 * val fragment = childFragmentManager.instantiate(fragmentClassName)
 */

fun FragmentManager.instantiate(className: String): Fragment =
    fragmentFactory.instantiate(ClassLoader.getSystemClassLoader(), className)

fun Fragment.classFragment(className: String): Fragment =
    childFragmentManager.instantiate(className)

fun FragmentActivity.classFragment(className: String): Fragment =
    supportFragmentManager.instantiate(className)

fun Fragment.setFragmentResultListener(
    targetFragment: Fragment,
    requestKey: String? = null,
    onFragmentResultListener: (Bundle) -> Unit,
    isClearRequestKeyAfterResult: Boolean = true
) {
    val mRequestKey = if (requestKey.isNullOrEmpty()) {
        targetFragment.hashCode().toString()
    } else {
        requestKey
    }
    parentFragmentManager.setFragmentResultListener(
        mRequestKey,
        this,
    ) { key, result ->
        if (mRequestKey == key) {
            onFragmentResultListener.invoke(result)

            if (isClearRequestKeyAfterResult) {
                parentFragmentManager.clearFragmentResult(mRequestKey)
            }
        }
    }
}

fun Fragment.setFragmentResult(requestKey: String? = null, result: Bundle) {
    val mRequestKey = if (requestKey.isNullOrEmpty()) {
        hashCode().toString()
    } else {
        requestKey
    }
    setFragmentResult(mRequestKey, result)
}

inline fun <reified T : Parcelable> Fragment.setFragmentResultAndPopBackStack(
    fragmentRequestKey: String = hashCode().toString(),
    resultBundleKey: String = T::class.java.simpleName,
    result: T
) {
    parentFragmentManager.setFragmentResult(fragmentRequestKey, bundleOf(resultBundleKey to result))
    parentFragmentManager.popBackStack()
}

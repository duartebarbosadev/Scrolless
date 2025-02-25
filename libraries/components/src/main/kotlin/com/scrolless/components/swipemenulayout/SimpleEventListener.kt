/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.swipemenulayout

typealias OnMenuItemClickListener = ((item: SwipeMenuItem, position: Int) -> Unit)?

typealias OnMenuClosedListener = ((layout: SwipeMenuLayout) -> Unit)?

typealias OnMenuLeftOpenedListener = ((layout: SwipeMenuLayout) -> Unit)?

typealias OnMenuRightOpenedListener = ((layout: SwipeMenuLayout) -> Unit)?

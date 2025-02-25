/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.navigation

import java.time.LocalDate

interface NavigationListener {
    fun goLeft()

    fun goRight()

    fun goToDate(dateTime: LocalDate)

    fun goToToday()

    fun showGoToDateDialog(currentDate: LocalDate)

    fun refreshReservations()

    fun onBigScrollDown()

    fun onScrollUp()
}

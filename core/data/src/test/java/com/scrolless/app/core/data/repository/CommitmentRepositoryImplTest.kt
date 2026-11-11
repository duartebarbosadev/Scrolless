/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.data.commitment.PinHasher
import com.scrolless.app.core.data.commitment.SettingsChangeCodec
import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.database.model.UserSettingsEntity
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.ChangeRequestResult
import com.scrolless.app.core.model.CommitmentMode
import com.scrolless.app.core.model.SettingsChange
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.UserSettingsStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentRepositoryImplTest {

    private val dao = mockk<UserSettingsDao>(relaxed = true)
    private val blockingConfigRepository = mockk<BlockingConfigRepository>(relaxed = true)
    private val userSettingsStore = mockk<UserSettingsStore>(relaxed = true)
    private var nowMillis = 10_000L
    private val timeProvider = object : TimeProvider {
        override fun currentTimeInMillis(): Long = nowMillis
        override fun localDateNow(): LocalDate = LocalDate.EPOCH
        override fun localDateTimeNow(): LocalDateTime = LocalDateTime.MIN
    }
    private val repository = CommitmentRepositoryImpl(dao, blockingConfigRepository, userSettingsStore, timeProvider)

    private val turnOff = SettingsChange.SelectOption(BlockOption.NothingSelected)

    @Test
    fun `with commitment off a loosening change applies right away`() = runTest {
        coEvery { dao.getUserSettings() } returns settings(CommitmentMode.Off)

        assertEquals(ChangeRequestResult.Applied, repository.request(turnOff))

        coVerify { blockingConfigRepository.setActiveOption(BlockOption.NothingSelected) }
        coVerify { userSettingsStore.setPauseUntil(0L) }
    }

    @Test
    fun `a tightening change applies right away in delay mode`() = runTest {
        coEvery { dao.getUserSettings() } returns settings(CommitmentMode.Delay)

        assertEquals(ChangeRequestResult.Applied, repository.request(SettingsChange.SelectOption(BlockOption.BlockAll)))

        coVerify { blockingConfigRepository.setActiveOption(BlockOption.BlockAll) }
    }

    @Test
    fun `delay mode queues a loosening change instead of applying it`() = runTest {
        coEvery { dao.getUserSettings() } returns settings(CommitmentMode.Delay, delayMillis = 5_000L)

        assertEquals(ChangeRequestResult.Scheduled(15_000L), repository.request(turnOff))

        coVerify { dao.setPendingChange(SettingsChangeCodec.encode(turnOff), 15_000L) }
        coVerify(exactly = 0) { blockingConfigRepository.setActiveOption(any()) }
    }

    @Test
    fun `pin mode asks for the pin and applies only when it matches`() = runTest {
        coEvery { dao.getUserSettings() } returns settings(CommitmentMode.Pin, pinHash = PinHasher.hash("4321"))

        assertEquals(ChangeRequestResult.PinRequired, repository.request(turnOff))
        assertFalse(repository.applyWithPin(turnOff, "0000"))
        coVerify(exactly = 0) { blockingConfigRepository.setActiveOption(any()) }

        assertTrue(repository.applyWithPin(turnOff, "4321"))
        coVerify { blockingConfigRepository.setActiveOption(BlockOption.NothingSelected) }
    }

    @Test
    fun `a queued change waits until it is due`() = runTest {
        coEvery { dao.getUserSettings() } returns settings(
            CommitmentMode.Delay,
            pendingChange = SettingsChangeCodec.encode(turnOff),
            pendingApplyAt = 20_000L,
        )

        assertFalse(repository.applyDuePendingChange())
        coVerify(exactly = 0) { blockingConfigRepository.setActiveOption(any()) }

        nowMillis = 20_000L
        assertTrue(repository.applyDuePendingChange())
        coVerify { dao.setPendingChange(null, 0L) }
        coVerify { blockingConfigRepository.setActiveOption(BlockOption.NothingSelected) }
    }

    @Test
    fun `turning commitment off clears the pin and any queued change`() = runTest {
        coEvery { dao.getUserSettings() } returns settings(CommitmentMode.Pin, pinHash = PinHasher.hash("1111"))

        assertTrue(repository.applyWithPin(SettingsChange.DisableCommitment, "1111"))

        coVerify { dao.setCommitment(CommitmentMode.Off, any(), null) }
        coVerify { dao.setPendingChange(null, 0L) }
    }

    private fun settings(
        mode: CommitmentMode,
        delayMillis: Long = 60_000L,
        pinHash: String? = null,
        pendingChange: String? = null,
        pendingApplyAt: Long = 0L,
    ) = UserSettingsEntity(
        activeBlockOption = BlockOption.BlockAll,
        commitmentMode = mode,
        commitmentDelayMillis = delayMillis,
        commitmentPinHash = pinHash,
        pendingChange = pendingChange,
        pendingChangeApplyAt = pendingApplyAt,
    )
}

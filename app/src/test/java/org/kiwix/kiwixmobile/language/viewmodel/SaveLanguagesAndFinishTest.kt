/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.language.viewmodel

import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.zim_manager.Language

class SaveLanguagesAndFinishTest {
  @Test
  fun `invoke saves and finishes`() = runTest {
    val languageDao = mockk<NewLanguagesDao>()
    val activity = mockk<AppCompatActivity>()
    val lifeCycleScope = TestScope(testScheduler)
    val onBackPressedDispatcher = mockk<OnBackPressedDispatcher>()
    every { activity.onBackPressedDispatcher } returns onBackPressedDispatcher
    every { onBackPressedDispatcher.onBackPressed() } answers { }
    val languages = listOf<Language>()
    SaveLanguagesAndFinish(languages, languageDao, lifeCycleScope).invokeWith(activity)
    testScheduler.advanceUntilIdle()
    coEvery { languageDao.insert(languages) }
    testScheduler.advanceUntilIdle()
    verify { onBackPressedDispatcher.onBackPressed() }
  }
}

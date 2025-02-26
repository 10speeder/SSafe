/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun File.isFileExist(dispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean =
  withContext(dispatcher) { exists() }

suspend fun File.freeSpace(dispatcher: CoroutineDispatcher = Dispatchers.IO): Long =
  withContext(dispatcher) { freeSpace }

suspend fun File.totalSpace(dispatcher: CoroutineDispatcher = Dispatchers.IO): Long =
  withContext(dispatcher) { totalSpace }

suspend fun File.canReadFile(dispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean =
  withContext(dispatcher) { canRead() }

suspend fun File.deleteFile(dispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean =
  withContext(dispatcher) { delete() }

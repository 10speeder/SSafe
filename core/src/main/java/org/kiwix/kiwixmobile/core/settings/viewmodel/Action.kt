/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.settings.viewmodel

import eu.mhutti1.utils.storage.StorageDevice

sealed class Action {
  object ClearAllHistory : Action()
  object ClearAllNotes : Action()
  object OpenCredits : Action()
  object ExportBookmarks : Action()
  object ImportBookmarks : Action()
  object AllowPermission : Action()
  data class OnStorageItemClick(val storageDevice: StorageDevice) : Action()
}

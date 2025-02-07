/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.kiwix.kiwixmobile.core.page.history.adapter.WebViewHistoryItem

@Entity
data class WebViewHistoryEntity(
  @PrimaryKey(autoGenerate = true) var id: Long = 0L,
  val zimId: String,
  val title: String,
  val pageUrl: String,
  val isForward: Boolean = false,
  val timeStamp: Long
) {
  constructor(webViewHistoryItem: WebViewHistoryItem) : this(
    webViewHistoryItem.databaseId,
    webViewHistoryItem.zimId,
    webViewHistoryItem.title,
    webViewHistoryItem.pageUrl,
    webViewHistoryItem.isForward,
    webViewHistoryItem.timeStamp
  )
}

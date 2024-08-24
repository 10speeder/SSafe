package org.kiwix.kiwixmobile.core.page.history.viewmodel

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.page.historyItem
import org.kiwix.kiwixmobile.core.page.historyState
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler

@ExtendWith(InstantExecutorExtension::class)
internal class HistoryViewModelTest {
  private val historyRoomDao: HistoryRoomDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val viewModelScope = CoroutineScope(Dispatchers.IO)

  private lateinit var viewModel: HistoryViewModel
  private val testScheduler = TestScheduler()
  private val zimReaderSource: ZimReaderSource = mockk()

  init {
    setScheduler(testScheduler)
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
  }

  private val itemsFromDb: PublishProcessor<List<Page>> =
    PublishProcessor.create()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showHistoryAllBooks } returns true
    every { historyRoomDao.history() } returns itemsFromDb
    every { historyRoomDao.pages() } returns historyRoomDao.history()
    viewModel = HistoryViewModel(historyRoomDao, zimReaderContainer, sharedPreferenceUtil)
  }

  @Test
  fun `Initial state returns initial state`() {
    assertThat(viewModel.initialState()).isEqualTo(historyState())
  }

  @Test
  fun `updatePagesBasedOnFilter returns state with searchTerm`() {
    assertThat(viewModel.updatePagesBasedOnFilter(historyState(), Filter("searchTerm")))
      .isEqualTo(
        historyState(searchTerm = "searchTerm")
      )
  }

  @Test
  fun `updatePages return state with history items`() {
    assertThat(
      viewModel.updatePages(
        historyState(),
        UpdatePages(listOf(historyItem(zimReaderSource = zimReaderSource)))
      )
    ).isEqualTo(
      historyState(listOf(historyItem(zimReaderSource = zimReaderSource)))
    )
  }

  @Test
  fun `offerUpdateToShowAllToggle offers UpdateAllHistoryPreference`() {
    viewModel.effects.test().also {
      viewModel.offerUpdateToShowAllToggle(
        UserClickedShowAllToggle(false), historyState()
      )
    }.assertValues(UpdateAllHistoryPreference(sharedPreferenceUtil, false))
  }

  @Test
  fun `offerUpdateToShowAllToggle returns state with showAll set to input value`() {
    assertThat(
      viewModel.offerUpdateToShowAllToggle(
        UserClickedShowAllToggle(false),
        historyState()
      )
    ).isEqualTo(historyState(showAll = false))
  }

  @Test
  fun `deselectAllPages returns state with all pages deselected`() {
    assertThat(
      viewModel.deselectAllPages(
        historyState(
          listOf(
            historyItem(
              isSelected = true,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
    )
      .isEqualTo(
        historyState(
          listOf(
            historyItem(
              isSelected = false,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
  }

  @Test
  fun `createDeletePageDialogEffect returns ShowDeleteHistoryDialog`() = runTest {
    assertThat(viewModel.createDeletePageDialogEffect(historyState(), viewModelScope)).isEqualTo(
      ShowDeleteHistoryDialog(
        viewModel.effects,
        historyState(),
        historyRoomDao,
        viewModelScope
      )
    )
  }

  @Test
  fun `copyWithNewItems returns state with new items`() {
    assertThat(
      viewModel.copyWithNewItems(
        historyState(),
        listOf(historyItem(isSelected = true, zimReaderSource = zimReaderSource))
      )
    )
      .isEqualTo(
        historyState(
          listOf(
            historyItem(
              isSelected = true,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
  }
}

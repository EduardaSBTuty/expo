package expo.modules.notifications

import android.os.Bundle
import android.os.Parcel
import androidx.test.filters.SmallTest
import expo.modules.notifications.notifications.triggers.DailyTrigger
import expo.modules.notifications.notifications.triggers.DateTrigger
import expo.modules.notifications.notifications.triggers.MonthlyTrigger
import expo.modules.notifications.notifications.triggers.TimeIntervalTrigger
import expo.modules.notifications.notifications.triggers.WeeklyTrigger
import expo.modules.notifications.notifications.triggers.YearlyTrigger
import kotlinx.parcelize.parcelableCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Ignore("Those test were ignore, because there using current time to calculate next trigger date which is flaky.")
@SmallTest
@RunWith(RobolectricTestRunner::class)
class NotificationTriggerTest {
  private var calendarNow: Calendar = Calendar.getInstance()
  private var calendar5MinutesFromNow: Calendar = (calendarNow.clone() as Calendar).apply {
    add(Calendar.MINUTE, 5)
  }

  @Test
  fun testDateTrigger() {
    val date5MinutesFromNow = calendar5MinutesFromNow.time
    val dateTrigger = DateTrigger(null, date5MinutesFromNow.time)
    val nextTriggerDate = dateTrigger.nextTriggerDate()
    assertEquals(/* expected = */ calendarNow.get(Calendar.MINUTE) + 5, /* actual = */ nextTriggerDate?.minutes)
    assertEquals(/* expected = */ calendarNow.get(Calendar.SECOND), /* actual = */ nextTriggerDate?.seconds)
    assertNull(dateTrigger.channelId)

    val dateTriggerWithChannel = DateTrigger("myChannel", date5MinutesFromNow.time)
    assertEquals(/* expected = */ "myChannel", /* actual = */ dateTriggerWithChannel.channelId)
  }

  @Test
  fun testTimeIntervalTrigger() {
    val interval = 30L
    val timeIntervalTrigger = TimeIntervalTrigger(null, interval, true)
    val expectedSeconds = (calendarNow.get(Calendar.SECOND) + interval) % 60
    val nextTriggerDate = timeIntervalTrigger.nextTriggerDate()

    val diff = abs(expectedSeconds - nextTriggerDate!!.seconds)
    assertTrue(
      "Expected seconds to be close to $expectedSeconds but was ${nextTriggerDate.seconds}.",
      diff <= 1
    )
    assertTrue(timeIntervalTrigger.isRepeating)

    val timeIntervalTriggerWithChannel = TimeIntervalTrigger("myChannel", 5, false)
    assertEquals("myChannel", timeIntervalTriggerWithChannel.channelId)
    assertFalse(timeIntervalTriggerWithChannel.isRepeating)
  }

  @Test
  fun testRepeatingTimeIntervalTriggerFromPast() {
    val now = System.currentTimeMillis()
    val dayAgo = Date(now - TimeUnit.DAYS.toMillis(1))
    val interval = 30L
    val timeIntervalTrigger = TimeIntervalTrigger(null, interval, true, dayAgo)
    val nextTriggerDate = timeIntervalTrigger.nextTriggerDate()

    val diffSeconds = abs(nextTriggerDate!!.time - (now + interval * 1000)) / 1000
    assertTrue("Expected diff to be below 1 second, was $diffSeconds.", diffSeconds < 1)
  }

  @Test
  fun testDailyTrigger() {
    val dailyTrigger = DailyTrigger(null, 9, 15)
    val nextTriggerDateCalendar = Calendar.getInstance()
    nextTriggerDateCalendar.time = dailyTrigger.nextTriggerDate()!!
    assertEquals(9, nextTriggerDateCalendar.get(Calendar.HOUR_OF_DAY))
    assertTrue(nextTriggerDateCalendar.after(calendarNow))
  }

  @Test
  fun testWeeklyTrigger() {
    val weeklyTrigger = WeeklyTrigger(null, 4, 9, 15)
    val nextTriggerDateCalendar = Calendar.getInstance()
    nextTriggerDateCalendar.time = weeklyTrigger.nextTriggerDate()!!
    assertEquals(9, nextTriggerDateCalendar.get(Calendar.HOUR_OF_DAY))
    assertEquals(4, nextTriggerDateCalendar.get(Calendar.DAY_OF_WEEK))
    assertTrue(nextTriggerDateCalendar.after(calendarNow))
  }

  @Test
  fun testMonthlyTrigger() {
    val monthlyTrigger = MonthlyTrigger("myChannel", 15, 9, 15)
    val nextTriggerDateCalendar = Calendar.getInstance()
    nextTriggerDateCalendar.time = monthlyTrigger.nextTriggerDate()!!
    assertEquals(15, nextTriggerDateCalendar.get(Calendar.DAY_OF_MONTH))
    assertTrue(nextTriggerDateCalendar.after(calendarNow))
  }

  @Test
  fun testYearlyTrigger() {
    val yearlyTrigger = YearlyTrigger(null, 15, 4, 9, 15)
    val nextTriggerDateCalendar = Calendar.getInstance()
    nextTriggerDateCalendar.time = yearlyTrigger.nextTriggerDate()!!
    assertEquals(15, nextTriggerDateCalendar.get(Calendar.DAY_OF_MONTH))
    assertEquals(4, nextTriggerDateCalendar.get(Calendar.MONTH))
    assertTrue(nextTriggerDateCalendar.after(calendarNow))
  }

  @Test
  fun testDateTriggerParcel() {
    // Date trigger
    val dateTrigger = DateTrigger("myChannel", calendar5MinutesFromNow.time.time)
    val parcel = Parcel.obtain()
    dateTrigger.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    val dateTriggerFromParcel = parcelableCreator<DateTrigger>().createFromParcel(parcel)
    assertEquals(dateTrigger.channelId, dateTriggerFromParcel.channelId)
    assertEquals(dateTrigger.timestamp, dateTriggerFromParcel.timestamp)
    assertEquals(calendar5MinutesFromNow.time.time, dateTriggerFromParcel.timestamp)
  }

  @Test
  fun testTimeIntervalTriggerParcel() {
    val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5)
    val pastDate = Date(pastTime)

    // Create a time interval trigger with a 2-second interval.
    // The triggerDate is in the past to verify that nextTriggerDate returns null
    val timeIntervalTrigger = TimeIntervalTrigger(null, 2, false, pastDate)

    val parcel = Parcel.obtain()
    timeIntervalTrigger.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)

    // restore trigger from the parcel
    val timeIntervalTriggerFromParcel = parcelableCreator<TimeIntervalTrigger>().createFromParcel(parcel)
    assertEquals(timeIntervalTriggerFromParcel.timeInterval, timeIntervalTrigger.timeInterval)
    assertEquals(timeIntervalTriggerFromParcel.channelId, timeIntervalTrigger.channelId)
    assertEquals(timeIntervalTriggerFromParcel.isRepeating, timeIntervalTrigger.isRepeating)

    assertNull("Non-repeating trigger should return null when its time has passed", timeIntervalTriggerFromParcel.nextTriggerDate())
  }

  @Test
  fun testDateTriggerToBundle() {
    val dateTrigger = DateTrigger("myChannel", calendar5MinutesFromNow.time.time)
    val bundle = dateTrigger.toBundle()
    assertBundleEquals(
      mapOf(
        "type" to "date",
        "channelId" to "myChannel",
        "value" to calendar5MinutesFromNow.time.time
      ),
      bundle
    )
  }

  @Test
  fun testTimeIntervalTriggerToBundle() {
    val timeIntervalTrigger = TimeIntervalTrigger("myChannel", 300, true)
    val bundle = timeIntervalTrigger.toBundle()
    assertBundleEquals(
      mapOf(
        "type" to "timeInterval",
        "channelId" to "myChannel",
        "seconds" to 300L,
        "repeats" to true
      ),
      bundle
    )
  }

  @Test
  fun testDailyTriggerToBundle() {
    val dailyTrigger = DailyTrigger("myChannel", 14, 30)
    val bundle = dailyTrigger.toBundle()
    assertBundleEquals(
      mapOf(
        "type" to "daily",
        "channelId" to "myChannel",
        "hour" to 14,
        "minute" to 30
      ),
      bundle
    )
  }

  @Test
  fun testWeeklyTriggerToBundle() {
    val weeklyTrigger = WeeklyTrigger("myChannel", Calendar.MONDAY, 9, 11)
    val bundle = weeklyTrigger.toBundle()
    assertBundleEquals(
      mapOf(
        "type" to "weekly",
        "channelId" to "myChannel",
        "weekday" to Calendar.MONDAY,
        "hour" to 9,
        "minute" to 11
      ),
      bundle
    )
  }

  @Test
  fun testMonthlyTriggerToBundle() {
    val monthlyTrigger = MonthlyTrigger("myChannel", 15, 12, 0)
    val bundle = monthlyTrigger.toBundle()
    assertBundleEquals(
      mapOf(
        "type" to "monthly",
        "channelId" to "myChannel",
        "day" to 15,
        "hour" to 12,
        "minute" to 0
      ),
      bundle
    )
  }

  @Test
  fun testYearlyTriggerToBundle() {
    val yearlyTrigger = YearlyTrigger("myChannel", 1, 0, 2, 3)
    val bundle = yearlyTrigger.toBundle()
    assertBundleEquals(
      mapOf(
        "type" to "yearly",
        "channelId" to "myChannel",
        "day" to 1,
        "month" to 0,
        "hour" to 2,
        "minute" to 3
      ),
      bundle
    )
  }

  private fun assertBundleEquals(expected: Map<String, Any>, actual: Bundle) {
    for ((key, value) in expected) {
      when (value) {
        is String -> assertEquals(value, actual.getString(key))
        is Int -> assertEquals(value, actual.getInt(key))
        is Long -> assertEquals(value, actual.getLong(key))
        is Boolean -> assertEquals(value, actual.getBoolean(key))
        else -> fail("Unsupported type for key $key")
      }
    }
  }
}

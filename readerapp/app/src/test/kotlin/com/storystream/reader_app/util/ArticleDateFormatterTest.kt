package com.storystream.reader_app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ArticleDateFormatterTest {
	private lateinit var originalLocale: Locale
	private lateinit var originalTimeZone: TimeZone

	@Before
	fun setUp() {
		originalLocale = Locale.getDefault()
		originalTimeZone = TimeZone.getDefault()
		Locale.setDefault(Locale.US)
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
	}

	@After
	fun tearDown() {
		Locale.setDefault(originalLocale)
		TimeZone.setDefault(originalTimeZone)
	}

	@Test
	fun `formats iso to readable date`() {
		assertEquals("Apr 30, 2026", ArticleDateFormatter.formatIsoToDisplay("2026-04-30T00:09:13Z"))
	}

	@Test
	fun `returns empty for blank input`() {
		assertEquals("", ArticleDateFormatter.formatIsoToDisplay("  "))
	}

	@Test
	fun `returns raw input when parse fails`() {
		assertEquals("not-a-date", ArticleDateFormatter.formatIsoToDisplay("not-a-date"))
	}
}


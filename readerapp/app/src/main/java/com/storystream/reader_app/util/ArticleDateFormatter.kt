package com.storystream.reader_app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object ArticleDateFormatter {
	private val formatter: DateTimeFormatter = DateTimeFormatter
		.ofLocalizedDate(FormatStyle.MEDIUM)
		.withLocale(Locale.getDefault())

	fun formatIsoToDisplay(raw: String): String {
		if (raw.isBlank()) return ""
		return runCatching {
			val instant = Instant.parse(raw)
			formatter.format(instant.atZone(ZoneId.systemDefault()))
		}.getOrElse { raw }
	}
}


package com.plantdoctor.service;

import java.util.regex.Pattern;

/**
 * Detects invented spray/treatment schedules in solution text.
 */
public final class InventedSchedulePatterns {

	public static final Pattern PATTERN = Pattern.compile(
			"(twice|two times|2 times)\\s+(a|per)\\s+week|every\\s+\\d+\\s+days|spray\\s+weekly|"
					+ "twice a week|two times a week|spray daily|"
					+ "\\d+\\s*[-–]\\s*\\d+\\s+days|"
					+ "repeat\\s+(?:treatment\\s+)?(?:in|after|every)\\s+\\d+",
			Pattern.CASE_INSENSITIVE);

	private InventedSchedulePatterns() {
	}
}

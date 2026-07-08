package com.example.moneyquest.app.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * クエストの実施可能曜日。
 * quest_t.available_days にビットマスク(月=1,火=2,水=4,木=8,金=16,土=32,日=64)として保存する。
 */
public enum QuestDay {

	MON(1, "月"),
	TUE(2, "火"),
	WED(4, "水"),
	THU(8, "木"),
	FRI(16, "金"),
	SAT(32, "土"),
	SUN(64, "日");

	/** 全曜日を表すビットマスク（月〜日すべてのbitを立てた値）。既存クエストの後方互換デフォルト。 */
	public static final int ALL_DAYS_MASK = 127;

	private final int bit;
	private final String label;

	QuestDay(int bit, String label) {
		this.bit = bit;
		this.label = label;
	}

	public int getBit() {
		return bit;
	}

	public String getLabel() {
		return label;
	}

	public static QuestDay fromDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> MON;
			case TUESDAY -> TUE;
			case WEDNESDAY -> WED;
			case THURSDAY -> THU;
			case FRIDAY -> FRI;
			case SATURDAY -> SAT;
			case SUNDAY -> SUN;
		};
	}

	/**
	 * 今日がavailableDays(ビットマスク、nullは全曜日扱い)の対象曜日かどうか。
	 * QuestEntity#isAvailableToday からのみ呼ばれる想定（Thymeleafはテンプレート内でのBean参照(@bean)を
	 * th:eachのループ内で使うと解析エラーになるため、テンプレートからは直接呼べずEntity経由にする必要がある）。
	 */
	public static boolean isAvailableToday(Integer availableDays) {
		return isAvailableOn(availableDays, LocalDate.now().getDayOfWeek());
	}

	public static boolean isAvailableOn(Integer availableDays, DayOfWeek dayOfWeek) {
		int mask = availableDays == null ? ALL_DAYS_MASK : availableDays;
		return (mask & fromDayOfWeek(dayOfWeek).getBit()) != 0;
	}

	public static String format(Integer availableDays) {
		int mask = availableDays == null ? ALL_DAYS_MASK : availableDays;
		if (mask == ALL_DAYS_MASK) {
			return "毎日";
		}
		int finalMask = mask;
		return Arrays.stream(values())
				.filter(day -> (finalMask & day.getBit()) != 0)
				.map(QuestDay::getLabel)
				.collect(Collectors.joining("・"));
	}

	public static String toCsv(Integer availableDays) {
		int mask = availableDays == null ? ALL_DAYS_MASK : availableDays;
		int finalMask = mask;
		return Arrays.stream(values())
				.filter(day -> (finalMask & day.getBit()) != 0)
				.map(Enum::name)
				.collect(Collectors.joining(","));
	}
}

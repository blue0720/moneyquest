package com.example.moneyquest.app.infra.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.example.moneyquest.app.domain.model.QuestDay;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quest_t")
public class QuestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quest_id")
	private Integer questId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_user_id")
	private UserEntity childUser;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@Column(name = "reward_amount")
	private Integer rewardAmount;

	@Column(name = "exp")
	private Integer exp;

	@Column(name = "limit_amount")
	private Integer limitAmount;

	@Column(name = "available_days")
	private Integer availableDays;

	@Column(name = "status")
	private Integer status;

	@Column(name = "registered_date")
	private LocalDateTime registeredDate;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

	/**
	 * 今日が実施可能曜日か。テンプレート(child-home.html)からBean参照(@questService)を
	 * th:eachループ内で呼ぶとThymeleafが解析エラーにするため、Entityのメソッド経由で公開する。
	 */
	public boolean isAvailableToday() {
		return QuestDay.isAvailableToday(availableDays);
	}

	/** 実施可能曜日の表示用文字列（全曜日なら「毎日」、そうでなければ「月・水・金」等）。 */
	public String getAvailableDaysDisplay() {
		return QuestDay.format(availableDays);
	}

	/** 実施可能曜日をCSV形式のenum名(例: "MON,WED,FRI")で返す。保護者の編集モーダル復元用。 */
	public String getAvailableDayCodes() {
		return QuestDay.toCsv(availableDays);
	}

}

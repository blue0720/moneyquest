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

import lombok.Getter;
import lombok.Setter;

/**
 * コインショップで購入済みのアイテム（フレーム/称号）を子供ごとに記録するテーブル。
 * 装備中のものは character_t.equipped_frame / equipped_title で別管理し、
 * このテーブルは「購入済みかどうか」の判定のみに使う。
 */
@Getter
@Setter
@Entity
@Table(name = "character_item_t")
public class CharacterItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "character_item_id")
	private Integer characterItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_user_id")
	private UserEntity childUser;

	@Column(name = "item_code")
	private String itemCode;

	@Column(name = "purchased_date")
	private LocalDateTime purchasedDate;

}

package com.example.moneyquest.app.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.example.moneyquest.app.domain.model.CharacterType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "character_t")
public class CharacterEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "character_id")
	private Integer characterId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_user_id")
	private UserEntity childUser;

	@Enumerated(EnumType.STRING)
	@Column(name = "character_type")
	private CharacterType characterType;

	@Column(name = "character_name")
	private String characterName;

	@Column(name = "level")
	private Integer level;

	@Column(name = "total_achievement_count")
	private Integer totalAchievementCount;

	@Column(name = "current_exp")
	private Integer currentExp;

	public Integer getCharacterId() {
		return characterId;
	}


}

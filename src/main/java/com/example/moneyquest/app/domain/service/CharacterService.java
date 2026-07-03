package com.example.moneyquest.app.domain.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.infra.entity.CharacterEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.CharacterRepository;

/**
 * キャラクター（育成要素）のサービス。
 * 子供アカウント作成時の生成、ホーム表示用の取得、名前変更、
 * クエスト承認時の経験値・達成数の加算（レベルアップ判定）を担う。
 *
 * レベルの規則はプロトタイプ（child/screens.jsx の calcLevel）に合わせる：
 *   ・レベルアップは一律 10XP（XP_PER_LEVEL）
 *   ・レベル上限は 100（MAX_LEVEL）／1クエスト = 5XP
 */
@Service
@Transactional
public class CharacterService {

	private static final int XP_PER_LEVEL = 10;
	private static final int MAX_LEVEL = 100;
	/** 生成直後のキャラのレベル。DDL（character_t.level DEFAULT 0）＋プロトタイプに合わせて 0 始まり。 */
	private static final int INITIAL_LEVEL = 0;
	/** キャラ名の初期値（character_name は NOT NULL のため必ずセットする）。 */
	private static final String DEFAULT_NAME = "タネ";

	private final CharacterRepository characterRepository;

	public CharacterService(CharacterRepository characterRepository) {
		this.characterRepository = characterRepository;
	}

	/**
	 * キャラ生成。子供アカウント作成時に UserService から呼ばれ、その子のキャラを1体作る。
	 */
	public void createCharacter(Integer childUserId) {
		// FK（child_user_id）を張るために、id だけ設定した UserEntity 参照を使う。
		UserEntity child = new UserEntity();
		child.setUserId(childUserId);

		CharacterEntity character = new CharacterEntity();
		character.setChildUser(child);
		character.setCharacterName(DEFAULT_NAME);
		character.setLevel(INITIAL_LEVEL);
		character.setCurrentExp(0);
		character.setTotalAchievementCount(0);

		characterRepository.save(character);
	}

	/**
	 * 取得。ホーム表示用に、次レベルまでの必要経験値（nextLevelExp）も詰めて返す。
	 */
	@Transactional(readOnly = true)
	public CharacterDto getCharacter(Integer childUserId) {
		CharacterEntity character = getEntity(childUserId);

		CharacterDto dto = new CharacterDto();
		dto.setCharacterName(character.getCharacterName());
		dto.setLevel(character.getLevel());
		dto.setCurrentExp(character.getCurrentExp());
		dto.setNextLevelExp(XP_PER_LEVEL);
		dto.setTotalAchievementCount(character.getTotalAchievementCount());
		return dto;
	}

	/**
	 * 名前変更。子供ホームのモーダルから呼ばれる。
	 */
	public void updateCharacterName(Integer childUserId, String characterName) {
		CharacterEntity character = getEntity(childUserId);
		character.setCharacterName(characterName);
		characterRepository.save(character);
	}

	/**
	 * 経験値加算（レベルアップ判定）。クエスト承認時などに呼ばれる。
	 * 必要経験値（10）に達するごとにレベルを上げ、上限100でカンストさせる。
	 */
	public void addExp(Integer childUserId, Integer amount) {
		if (amount == null || amount <= 0) {
			return;
		}

		CharacterEntity character = getEntity(childUserId);
		int level = character.getLevel() == null ? INITIAL_LEVEL : character.getLevel();
		int exp = (character.getCurrentExp() == null ? 0 : character.getCurrentExp()) + amount;

		while (level < MAX_LEVEL && exp >= XP_PER_LEVEL) {
			exp -= XP_PER_LEVEL;
			level++;
		}
		if (level >= MAX_LEVEL) {
			// カンスト：レベルは100で止め、経験値バーは満タンで固定する。
			level = MAX_LEVEL;
			exp = Math.min(exp, XP_PER_LEVEL);
		}

		character.setLevel(level);
		character.setCurrentExp(exp);
		characterRepository.save(character);
	}

	/**
	 * 達成数加算。クエスト達成（承認）ごとに累計達成数を1増やす。
	 */
	public void incrementAchievement(Integer childUserId) {
		CharacterEntity character = getEntity(childUserId);
		int count = character.getTotalAchievementCount() == null ? 0 : character.getTotalAchievementCount();
		character.setTotalAchievementCount(count + 1);
		characterRepository.save(character);
	}

	/**
	 * child_user_id からキャラを1体取得する（User1＝Character1）。
	 * 見つからなければ例外。
	 */
	private CharacterEntity getEntity(Integer childUserId) {
		List<CharacterEntity> characters = characterRepository.findByChildUserId(childUserId);
		if (characters.isEmpty()) {
			throw new NoSuchElementException("Character not found for childUserId=" + childUserId);
		}
		return characters.get(0);
	}

}
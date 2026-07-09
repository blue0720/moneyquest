package com.example.moneyquest.app.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.domain.model.ShopItemDto;
import com.example.moneyquest.app.infra.entity.CharacterItemEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.CharacterItemRepository;

/**
 * コインショップサービス。
 * 商品カタログ（フレーム/称号）はバッジ機能と同様にハードコードで持つ。
 * 購入済みかどうかは character_item_t、現在の装備は character_t.equipped_frame/equipped_title で管理する。
 */
@Service
@Transactional
public class ShopService {

	private static final String CATEGORY_FRAME = "FRAME";
	private static final String CATEGORY_TITLE = "TITLE";

	private record ShopItemDefinition(String code, String category, String name, int cost) {
	}

	private static final List<ShopItemDefinition> DEFINITIONS = List.of(
			new ShopItemDefinition("FRAME_NONE", CATEGORY_FRAME, "ノーマル", 0),
			new ShopItemDefinition("FRAME_SILVER", CATEGORY_FRAME, "シルバーフレーム", 20),
			new ShopItemDefinition("FRAME_GOLD", CATEGORY_FRAME, "ゴールドフレーム", 50),
			new ShopItemDefinition("FRAME_RAINBOW", CATEGORY_FRAME, "レインボーフレーム", 100),
			new ShopItemDefinition("TITLE_NONE", CATEGORY_TITLE, "なし", 0),
			new ShopItemDefinition("TITLE_SHINE", CATEGORY_TITLE, "きらきら", 15),
			new ShopItemDefinition("TITLE_HERO", CATEGORY_TITLE, "ゆうしゃ", 40),
			new ShopItemDefinition("TITLE_LEGEND", CATEGORY_TITLE, "レジェンド", 100));

	private final CharacterService characterService;
	private final CharacterItemRepository characterItemRepository;

	public ShopService(CharacterService characterService, CharacterItemRepository characterItemRepository) {
		this.characterService = characterService;
		this.characterItemRepository = characterItemRepository;
	}

	/**
	 * 商品カタログ全件を、所持・装備状態付きで返す。
	 */
	@Transactional(readOnly = true)
	public List<ShopItemDto> getShopItems(Integer childUserId) {
		CharacterDto character = characterService.getCharacter(childUserId);
		List<CharacterItemEntity> owned = characterItemRepository.findByChildUserId(childUserId);

		return DEFINITIONS.stream()
				.map(def -> toDto(def, character, owned))
				.toList();
	}

	/**
	 * 商品を購入する。所持済みなら何もしない。コイン残高が足りなければ例外。
	 * 購入した商品はそのまま自動で装備する。
	 */
	public void purchase(Integer childUserId, String itemCode) {
		ShopItemDefinition def = findDefinition(itemCode);

		if (isOwned(childUserId, def)) {
			return;
		}

		characterService.spendCoins(childUserId, def.cost());

		UserEntity child = new UserEntity();
		child.setUserId(childUserId);

		CharacterItemEntity item = new CharacterItemEntity();
		item.setChildUser(child);
		item.setItemCode(def.code());
		item.setPurchasedDate(LocalDateTime.now());
		characterItemRepository.save(item);

		equip(childUserId, def.code());
	}

	/**
	 * 所持済みの商品（またはNONE系デフォルト）を装備に切り替える。未所持なら例外。
	 */
	public void equip(Integer childUserId, String itemCode) {
		ShopItemDefinition def = findDefinition(itemCode);

		if (!isOwned(childUserId, def)) {
			throw new IllegalArgumentException("まだ持っていないアイテムです。");
		}

		if (CATEGORY_FRAME.equals(def.category())) {
			characterService.updateEquippedFrame(childUserId, def.code());
		} else {
			characterService.updateEquippedTitle(childUserId, def.code());
		}
	}

	private boolean isOwned(Integer childUserId, ShopItemDefinition def) {
		return def.cost() == 0 || characterItemRepository.existsByChildUserIdAndItemCode(childUserId, def.code());
	}

	private ShopItemDefinition findDefinition(String itemCode) {
		return DEFINITIONS.stream()
				.filter(def -> def.code().equals(itemCode))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("対象の商品が見つかりません。"));
	}

	private ShopItemDto toDto(ShopItemDefinition def, CharacterDto character, List<CharacterItemEntity> owned) {
		boolean isOwned = def.cost() == 0
				|| owned.stream().anyMatch(item -> item.getItemCode().equals(def.code()));
		boolean isEquipped = CATEGORY_FRAME.equals(def.category())
				? def.code().equals(character.getEquippedFrame())
				: def.code().equals(character.getEquippedTitle());

		ShopItemDto dto = new ShopItemDto();
		dto.setCode(def.code());
		dto.setCategory(def.category());
		dto.setName(def.name());
		dto.setCost(def.cost());
		dto.setOwned(isOwned);
		dto.setEquipped(isEquipped);
		return dto;
	}

}

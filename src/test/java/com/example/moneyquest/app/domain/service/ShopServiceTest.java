package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.domain.model.ShopItemDto;
import com.example.moneyquest.app.infra.entity.CharacterItemEntity;
import com.example.moneyquest.app.infra.repository.CharacterItemRepository;

/**
 * ShopService の単体テスト。
 * 所持・装備状態の判定、購入時のコイン消費・自動装備、未所持アイテムの装備拒否を重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

	@Mock
	private CharacterService characterService;

	@Mock
	private CharacterItemRepository characterItemRepository;

	@InjectMocks
	private ShopService shopService;

	private static final Integer CHILD_USER_ID = 10;

	private CharacterDto defaultCharacter() {
		CharacterDto dto = new CharacterDto();
		dto.setEquippedFrame("FRAME_NONE");
		dto.setEquippedTitle("TITLE_NONE");
		return dto;
	}

	@Nested
	@DisplayName("getShopItems")
	class GetShopItems {

		@Test
		@DisplayName("NONE系のデフォルト商品は常に所持扱いになる")
		void getShopItems_noneItems_alwaysOwned() {
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(defaultCharacter());
			when(characterItemRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			List<ShopItemDto> items = shopService.getShopItems(CHILD_USER_ID);

			ShopItemDto frameNone = items.stream().filter(i -> i.getCode().equals("FRAME_NONE")).findFirst().orElseThrow();
			assertThat(frameNone.getOwned()).isTrue();
			assertThat(frameNone.getEquipped()).isTrue();

			ShopItemDto frameGold = items.stream().filter(i -> i.getCode().equals("FRAME_GOLD")).findFirst().orElseThrow();
			assertThat(frameGold.getOwned()).isFalse();
			assertThat(frameGold.getEquipped()).isFalse();
		}

		@Test
		@DisplayName("購入済みかつ装備中の商品はowned/equipped共にtrueになる")
		void getShopItems_purchasedAndEquipped() {
			CharacterDto character = defaultCharacter();
			character.setEquippedFrame("FRAME_GOLD");
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(character);

			CharacterItemEntity owned = new CharacterItemEntity();
			owned.setItemCode("FRAME_GOLD");
			when(characterItemRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(owned));

			List<ShopItemDto> items = shopService.getShopItems(CHILD_USER_ID);

			ShopItemDto frameGold = items.stream().filter(i -> i.getCode().equals("FRAME_GOLD")).findFirst().orElseThrow();
			assertThat(frameGold.getOwned()).isTrue();
			assertThat(frameGold.getEquipped()).isTrue();
		}
	}

	@Nested
	@DisplayName("purchase")
	class Purchase {

		@Test
		@DisplayName("既に所持している場合は何もしない(コイン消費もされない)")
		void purchase_alreadyOwned_doesNothing() {
			when(characterItemRepository.existsByChildUserIdAndItemCode(CHILD_USER_ID, "FRAME_SILVER"))
					.thenReturn(true);

			shopService.purchase(CHILD_USER_ID, "FRAME_SILVER");

			verify(characterService, never()).spendCoins(any(), any());
			verify(characterItemRepository, never()).save(any());
		}

		@Test
		@DisplayName("コイン残高が足りない場合は例外が伝播し、購入記録は作られない")
		void purchase_insufficientCoins_throws() {
			when(characterItemRepository.existsByChildUserIdAndItemCode(CHILD_USER_ID, "FRAME_SILVER"))
					.thenReturn(false);
			org.mockito.Mockito.doThrow(new IllegalArgumentException("コインが足りません。"))
					.when(characterService).spendCoins(CHILD_USER_ID, 20);

			assertThatThrownBy(() -> shopService.purchase(CHILD_USER_ID, "FRAME_SILVER"))
					.isInstanceOf(IllegalArgumentException.class);

			verify(characterItemRepository, never()).save(any());
		}

		@Test
		@DisplayName("購入に成功すると、コイン消費・購入記録の保存・自動装備が行われる")
		void purchase_success_equipsAutomatically() {
			when(characterItemRepository.existsByChildUserIdAndItemCode(CHILD_USER_ID, "FRAME_SILVER"))
					.thenReturn(false)
					.thenReturn(true); // purchase内のequip()呼び出し時には所持済みとして扱う

			shopService.purchase(CHILD_USER_ID, "FRAME_SILVER");

			verify(characterService).spendCoins(CHILD_USER_ID, 20);
			verify(characterItemRepository, times(1)).save(any(CharacterItemEntity.class));
			verify(characterService).updateEquippedFrame(CHILD_USER_ID, "FRAME_SILVER");
		}
	}

	@Nested
	@DisplayName("equip")
	class Equip {

		@Test
		@DisplayName("未所持のアイテムを装備しようとするとIllegalArgumentExceptionを投げる")
		void equip_notOwned_throws() {
			when(characterItemRepository.existsByChildUserIdAndItemCode(CHILD_USER_ID, "TITLE_HERO"))
					.thenReturn(false);

			assertThatThrownBy(() -> shopService.equip(CHILD_USER_ID, "TITLE_HERO"))
					.isInstanceOf(IllegalArgumentException.class);

			verify(characterService, never()).updateEquippedTitle(any(), any());
		}

		@Test
		@DisplayName("所持済みの称号は装備できる")
		void equip_owned_updatesTitle() {
			when(characterItemRepository.existsByChildUserIdAndItemCode(CHILD_USER_ID, "TITLE_HERO"))
					.thenReturn(true);

			shopService.equip(CHILD_USER_ID, "TITLE_HERO");

			verify(characterService).updateEquippedTitle(eq(CHILD_USER_ID), eq("TITLE_HERO"));
		}

		@Test
		@DisplayName("NONE系はいつでも装備できる(所持チェック不要)")
		void equip_noneItem_alwaysAllowed() {
			shopService.equip(CHILD_USER_ID, "FRAME_NONE");

			verify(characterService).updateEquippedFrame(CHILD_USER_ID, "FRAME_NONE");
			verify(characterItemRepository, never()).existsByChildUserIdAndItemCode(any(), any());
		}
	}
}

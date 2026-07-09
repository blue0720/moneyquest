package com.example.moneyquest.app.domain.model;

import lombok.Data;

/**
 * コインショップの商品1件分の表示用DTO。
 */
@Data
public class ShopItemDto {

	private String code;
	/** "FRAME" または "TITLE" */
	private String category;
	private String name;
	private Integer cost;
	private Boolean owned;
	private Boolean equipped;

}

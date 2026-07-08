package com.example.moneyquest.app.domain.model;

/**
 * キャラクターの種類。
 * 子供アカウント作成時に選択し、子供ホームの「しゅるいをかえる」からいつでも変更できる。
 */
public enum CharacterType {

	GRASS("くさタイプ"),
	FLAME("ほのおタイプ"),
	AQUA("みずタイプ"),
	THUNDER("かみなりタイプ");

	private final String displayName;

	CharacterType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

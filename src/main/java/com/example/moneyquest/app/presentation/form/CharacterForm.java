package com.example.moneyquest.app.presentation.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterForm {

	@NotBlank(message = "キャラクター名を入力してください")
	@Size(max = 15, message = "キャラクター名は15文字以内で入力してください")
	private String characterName;
}
package com.example.moneyquest.app.presentation.form;

import jakarta.validation.constraints.NotNull;

import com.example.moneyquest.app.domain.model.CharacterType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterTypeForm {

	@NotNull(message = "キャラクターのしゅるいを選んでください")
	private CharacterType characterType;
}

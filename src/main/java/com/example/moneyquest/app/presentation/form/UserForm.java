package com.example.moneyquest.app.presentation.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.moneyquest.app.domain.model.CharacterType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {

	/** 子供アカウント作成時のみ使用するキャラクター種類（保護者・管理者登録では未使用） */
	private CharacterType characterType;

	@NotBlank(message = "ユーザー名を入力してください")
	@Size(max = 20, message = "ユーザー名は20文字以内で入力してください")
	private String userName;

	@NotBlank(message = "メールアドレスを入力してください")
	@Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "メールアドレスの形式が正しくありません")
	@Size(max = 100, message = "メールアドレスは100文字以内で入力してください")
	private String mailAddress;

	@NotBlank(message = "パスワードを入力してください")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{4,20}$", 
	message = "パスワードは半角英数字を組み合わせて4文字以上20文字以内で入力してください")
	private String password;

	@NotBlank(message = "確認用パスワードを入力してください")
	private String passwordConfirm;

}

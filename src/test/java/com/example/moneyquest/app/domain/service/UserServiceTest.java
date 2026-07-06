package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.CharacterRepository;
import com.example.moneyquest.app.infra.repository.IncomeExpenseRepository;
import com.example.moneyquest.app.infra.repository.QuestRepository;
import com.example.moneyquest.app.infra.repository.SpendingLimitRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.UserForm;

/**
 * UserService の単体テスト。
 * 正常系に加え、メールアドレス重複・対象未存在・権限不一致による例外ハンドリングを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CharacterService characterService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private QuestRepository questRepository;

	@Mock
	private IncomeExpenseRepository incomeExpenseRepository;

	@Mock
	private SpendingLimitRepository spendingLimitRepository;

	@Mock
	private CharacterRepository characterRepository;

	@InjectMocks
	private UserService userService;

	private static final Integer PARENT_USER_ID = 1;
	private static final Integer CHILD_USER_ID = 10;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private UserForm buildForm(String name, String mail, String password) {
		UserForm form = new UserForm();
		form.setUserName(name);
		form.setMailAddress(mail);
		form.setPassword(password);
		form.setPasswordConfirm(password);
		return form;
	}

	@Nested
	@DisplayName("createUser")
	class CreateUser {

		@Test
		@DisplayName("保護者アカウント作成時はメールが未使用ならユーザーが保存される")
		void createUser_parent_success() {
			UserForm form = buildForm("親太郎", "parent@example.com", "pass1234");

			when(userRepository.findByMailAddress("parent@example.com")).thenReturn(Optional.empty());
			when(passwordEncoder.encode("pass1234")).thenReturn("ENCODED");

			userService.createUser(CustomUserDetails.AUTHORITY_PARENT, form);

			verify(userRepository, times(1)).save(any(UserEntity.class));
			verify(characterService, never()).createCharacter(any());
		}

		@Test
		@DisplayName("既に使用されているメールアドレスの場合はIllegalStateExceptionを投げる")
		void createUser_duplicatedMail_throws() {
			UserForm form = buildForm("親太郎", "dup@example.com", "pass1234");

			when(userRepository.findByMailAddress("dup@example.com"))
					.thenReturn(Optional.of(new UserEntity()));

			assertThatThrownBy(() -> userService.createUser(CustomUserDetails.AUTHORITY_PARENT, form))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("すでに使用されています");

			verify(userRepository, never()).save(any());
			verify(passwordEncoder, never()).encode(any());
		}

		@Test
		@DisplayName("子供アカウント作成時はキャラクターも作成される")
		void createUser_child_createsCharacter() {
			UserForm form = buildForm("子供太郎", "child@example.com", "pass1234");

			when(userRepository.findByMailAddress("child@example.com")).thenReturn(Optional.empty());
			lenient().when(passwordEncoder.encode("pass1234")).thenReturn("ENCODED");

			userService.createUser(CustomUserDetails.AUTHORITY_CHILD, form);

			verify(characterService, times(1)).createCharacter(any());
		}
	}

	@Nested
	@DisplayName("updateUser")
	class UpdateUser {

		@Test
		@DisplayName("存在しないユーザーIDの場合はIllegalArgumentExceptionを投げる")
		void updateUser_notFound_throws() {
			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.empty());

			UserForm form = buildForm("名前", "mail@example.com", "pass1234");

			assertThatThrownBy(() -> userService.updateUser(CHILD_USER_ID, form))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("見つかりません");
		}

		@Test
		@DisplayName("他人が使用中のメールアドレスに変更しようとした場合はIllegalStateExceptionを投げる")
		void updateUser_duplicatedMail_throws() {
			UserEntity existingUser = new UserEntity();
			existingUser.setUserId(CHILD_USER_ID);
			existingUser.setMailAddress("old@example.com");

			UserEntity otherUser = new UserEntity();
			otherUser.setUserId(999);

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(existingUser));
			when(userRepository.findByMailAddress("taken@example.com")).thenReturn(Optional.of(otherUser));

			UserForm form = buildForm("名前", "taken@example.com", "");
			form.setPassword("");

			assertThatThrownBy(() -> userService.updateUser(CHILD_USER_ID, form))
					.isInstanceOf(IllegalStateException.class);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("パスワードが空欄の場合は既存パスワードを維持したまま更新する")
		void updateUser_blankPassword_keepsExistingPassword() {
			UserEntity existingUser = new UserEntity();
			existingUser.setUserId(CHILD_USER_ID);
			existingUser.setMailAddress("old@example.com");
			existingUser.setPassword("OLD_ENCODED");

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(existingUser));
			when(userRepository.findByMailAddress("old@example.com")).thenReturn(Optional.of(existingUser));

			UserForm form = buildForm("新しい名前", "old@example.com", "");

			userService.updateUser(CHILD_USER_ID, form);

			assertThat(existingUser.getPassword()).isEqualTo("OLD_ENCODED");
			assertThat(existingUser.getUserName()).isEqualTo("新しい名前");
			verify(passwordEncoder, never()).encode(any());
			verify(userRepository, times(1)).save(existingUser);
		}
	}

	@Nested
	@DisplayName("deleteFamilyUser")
	class DeleteFamilyUser {

		@Test
		@DisplayName("存在しない対象IDの場合はIllegalArgumentExceptionを投げる")
		void deleteFamilyUser_notFound_throws() {
			when(userRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> userService.deleteFamilyUser(PARENT_USER_ID, 999))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("自分の子供でも自分自身でもないアカウントを削除しようとした場合はIllegalStateExceptionを投げる")
		void deleteFamilyUser_notOwned_throws() {
			UserEntity strangerChild = new UserEntity();
			strangerChild.setUserId(CHILD_USER_ID);
			strangerChild.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
			strangerChild.setParentUserId(555); // 別の保護者の子供

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(strangerChild));

			assertThatThrownBy(() -> userService.deleteFamilyUser(PARENT_USER_ID, CHILD_USER_ID))
					.isInstanceOf(IllegalStateException.class);

			verify(userRepository, never()).deleteById(any());
		}

		@Test
		@DisplayName("自分の子供を削除する場合は関連データが先に削除されユーザーも削除される")
		void deleteFamilyUser_ownChild_success() {
			UserEntity child = new UserEntity();
			child.setUserId(CHILD_USER_ID);
			child.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
			child.setParentUserId(PARENT_USER_ID);

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(child));
			when(incomeExpenseRepository.findByChildUserUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(questRepository.findByChildUserUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingLimitRepository.findByChildUser_UserIdOrderByRegisteredDateDesc(CHILD_USER_ID))
					.thenReturn(List.of());
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			userService.deleteFamilyUser(PARENT_USER_ID, CHILD_USER_ID);

			verify(userRepository, times(1)).deleteById(CHILD_USER_ID);
		}

		@Test
		@DisplayName("保護者が自分自身を削除する場合は子供のデータも先に削除される")
		void deleteFamilyUser_self_deletesChildrenFirst() {
			UserEntity parent = new UserEntity();
			parent.setUserId(PARENT_USER_ID);
			parent.setAuthority(CustomUserDetails.AUTHORITY_PARENT);

			UserEntity child = new UserEntity();
			child.setUserId(CHILD_USER_ID);

			when(userRepository.findById(PARENT_USER_ID)).thenReturn(Optional.of(parent));
			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(child));
			when(incomeExpenseRepository.findByChildUserUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(questRepository.findByChildUserUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingLimitRepository.findByChildUser_UserIdOrderByRegisteredDateDesc(CHILD_USER_ID))
					.thenReturn(List.of());
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			userService.deleteFamilyUser(PARENT_USER_ID, PARENT_USER_ID);

			verify(userRepository, times(1)).deleteById(CHILD_USER_ID);
			verify(userRepository, times(1)).deleteById(PARENT_USER_ID);
		}
	}

	@Nested
	@DisplayName("updateChildUser")
	class UpdateChildUser {

		@Test
		@DisplayName("存在しない子供IDの場合はIllegalArgumentExceptionを投げる")
		void updateChildUser_notFound_throws() {
			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.empty());

			UserForm form = buildForm("名前", "mail@example.com", "pass1234");

			assertThatThrownBy(() -> userService.updateChildUser(PARENT_USER_ID, CHILD_USER_ID, form))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("他の保護者の子供を編集しようとした場合はIllegalStateExceptionを投げる")
		void updateChildUser_wrongParent_throws() {
			UserEntity child = new UserEntity();
			child.setUserId(CHILD_USER_ID);
			child.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
			child.setParentUserId(555);

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(child));

			UserForm form = buildForm("名前", "mail@example.com", "pass1234");

			assertThatThrownBy(() -> userService.updateChildUser(PARENT_USER_ID, CHILD_USER_ID, form))
					.isInstanceOf(IllegalStateException.class);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("子供ではないアカウント（保護者）を子供として編集しようとした場合はIllegalStateExceptionを投げる")
		void updateChildUser_notAChild_throws() {
			UserEntity notChild = new UserEntity();
			notChild.setUserId(CHILD_USER_ID);
			notChild.setAuthority(CustomUserDetails.AUTHORITY_PARENT);
			notChild.setParentUserId(PARENT_USER_ID);

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(notChild));

			UserForm form = buildForm("名前", "mail@example.com", "pass1234");

			assertThatThrownBy(() -> userService.updateChildUser(PARENT_USER_ID, CHILD_USER_ID, form))
					.isInstanceOf(IllegalStateException.class);
		}
	}
}

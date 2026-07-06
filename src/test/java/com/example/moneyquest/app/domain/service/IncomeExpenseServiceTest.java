package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.infra.entity.IncomeExpenseEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.IncomeExpenseRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;

/**
 * IncomeExpenseService の単体テスト。
 * 所有権チェック・入力チェックによる SecurityException のハンドリングを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class IncomeExpenseServiceTest {

	@Mock
	private IncomeExpenseRepository incomeExpenseRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private IncomeExpenseService incomeExpenseService;

	private static final Integer PARENT_USER_ID = 1;
	private static final Integer CHILD_USER_ID = 10;

	private UserEntity childUser;
	private UserEntity parentUser;

	@BeforeEach
	void setUp() {
		childUser = new UserEntity();
		childUser.setUserId(CHILD_USER_ID);
		childUser.setParentUserId(PARENT_USER_ID);
		childUser.setAuthority(CustomUserDetails.AUTHORITY_CHILD);

		parentUser = new UserEntity();
		parentUser.setUserId(PARENT_USER_ID);
		parentUser.setAuthority(CustomUserDetails.AUTHORITY_PARENT);
	}

	@Nested
	@DisplayName("createRecord")
	class CreateRecord {

		@Test
		@DisplayName("子供が登録する場合は支出(recordType=1)として自分名義で登録される")
		void createRecord_child_createsExpense() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setAmount(500);
			form.setCategory("おかし");
			form.setMemo("メモ");

			incomeExpenseService.createRecord(form, childUser, null);

			verify(incomeExpenseRepository).save(org.mockito.ArgumentMatchers.argThat(entity ->
					entity.getRecordType() == 1
							&& entity.getChildUser() == childUser
							&& entity.getCategory().equals("おかし")));
		}

		@Test
		@DisplayName("子供の支出登録でカテゴリが空欄の場合はSecurityExceptionを投げる")
		void createRecord_child_blankCategory_throws() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setAmount(500);
			form.setCategory("  ");

			assertThatThrownBy(() -> incomeExpenseService.createRecord(form, childUser, null))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("なにをかったかおしえてね");

			verify(incomeExpenseRepository, never()).save(any());
		}

		@Test
		@DisplayName("保護者が登録する場合は収入(recordType=0)として対象の子供名義で登録される")
		void createRecord_parent_createsIncome() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setAmount(1000);
			form.setMemo("おこづかい");

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(childUser));

			incomeExpenseService.createRecord(form, parentUser, CHILD_USER_ID);

			verify(incomeExpenseRepository).save(org.mockito.ArgumentMatchers.argThat(entity ->
					entity.getRecordType() == 0
							&& entity.getCategory().equals("おこづかい"))); // 保護者収入はメモをカテゴリに使用
		}

		@Test
		@DisplayName("保護者が存在しない子供を対象に登録しようとした場合はSecurityExceptionを投げる")
		void createRecord_parent_childNotFound_throws() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setAmount(1000);

			when(userRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> incomeExpenseService.createRecord(form, parentUser, 999))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("対象の子供が存在しません");
		}

		@Test
		@DisplayName("保護者が自分の家族ではない子供に登録しようとした場合はSecurityExceptionを投げる")
		void createRecord_parent_notOwnChild_throws() {
			UserEntity strangerChild = new UserEntity();
			strangerChild.setUserId(CHILD_USER_ID);
			strangerChild.setParentUserId(555); // 別の保護者の子供

			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setAmount(1000);

			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(strangerChild));

			assertThatThrownBy(() -> incomeExpenseService.createRecord(form, parentUser, CHILD_USER_ID))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("権限がありません");

			verify(incomeExpenseRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("updateRecord")
	class UpdateRecord {

		@Test
		@DisplayName("存在しないレコードIDの場合はSecurityExceptionを投げる")
		void updateRecord_notFound_throws() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setIncomeExpenseId(1);

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> incomeExpenseService.updateRecord(form, childUser))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("データが存在しません");
		}

		@Test
		@DisplayName("他の子供のレコードを編集しようとした場合はSecurityExceptionを投げる")
		void updateRecord_wrongChild_throws() {
			UserEntity otherChild = new UserEntity();
			otherChild.setUserId(999);

			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(otherChild);
			entity.setRecordType(1);

			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setIncomeExpenseId(1);
			form.setCategory("test");

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> incomeExpenseService.updateRecord(form, childUser))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("権限がありません");
		}

		@Test
		@DisplayName("他の保護者の子供のレコードを編集しようとした場合はSecurityExceptionを投げる")
		void updateRecord_wrongParent_throws() {
			UserEntity otherParentsChild = new UserEntity();
			otherParentsChild.setUserId(CHILD_USER_ID);
			otherParentsChild.setParentUserId(555);

			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(otherParentsChild);
			entity.setRecordType(0);

			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setIncomeExpenseId(1);

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> incomeExpenseService.updateRecord(form, parentUser))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("権限がありません");
		}

		@Test
		@DisplayName("支出レコードでカテゴリが空欄の場合はSecurityExceptionを投げる")
		void updateRecord_blankCategoryOnExpense_throws() {
			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(childUser);
			entity.setRecordType(1);

			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setIncomeExpenseId(1);
			form.setCategory("");

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> incomeExpenseService.updateRecord(form, childUser))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("なにをかったかおしえてね");
		}

		@Test
		@DisplayName("本人が正しく編集した場合は金額・メモ・カテゴリが更新される")
		void updateRecord_success() {
			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(childUser);
			entity.setRecordType(1);

			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setIncomeExpenseId(1);
			form.setCategory("おもちゃ");
			form.setAmount(2000);
			form.setMemo("誕生日プレゼント");

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.of(entity));

			incomeExpenseService.updateRecord(form, childUser);

			assertThat(entity.getCategory()).isEqualTo("おもちゃ");
			assertThat(entity.getAmount()).isEqualTo(2000);
			assertThat(entity.getMemo()).isEqualTo("誕生日プレゼント");
			verify(incomeExpenseRepository, times(1)).save(entity);
		}
	}

	@Nested
	@DisplayName("deleteRecord")
	class DeleteRecord {

		@Test
		@DisplayName("存在しないレコードIDの場合はSecurityExceptionを投げる")
		void deleteRecord_notFound_throws() {
			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> incomeExpenseService.deleteRecord(1, childUser))
					.isInstanceOf(SecurityException.class);

			verify(incomeExpenseRepository, never()).delete(any());
		}

		@Test
		@DisplayName("他の子供のレコードを削除しようとした場合はSecurityExceptionを投げる")
		void deleteRecord_wrongChild_throws() {
			UserEntity otherChild = new UserEntity();
			otherChild.setUserId(999);

			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(otherChild);

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> incomeExpenseService.deleteRecord(1, childUser))
					.isInstanceOf(SecurityException.class);

			verify(incomeExpenseRepository, never()).delete(any());
		}

		@Test
		@DisplayName("本人が削除した場合はリポジトリのdeleteが呼ばれる")
		void deleteRecord_success() {
			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(childUser);

			when(incomeExpenseRepository.findById(1)).thenReturn(Optional.of(entity));

			incomeExpenseService.deleteRecord(1, childUser);

			verify(incomeExpenseRepository, times(1)).delete(entity);
		}
	}

	@Nested
	@DisplayName("getMonthlyExpenseAmount")
	class GetMonthlyExpenseAmount {

		@Test
		@DisplayName("指定日を含む月の開始・終了日時でリポジトリに問い合わせる")
		void getMonthlyExpenseAmount_delegatesWithMonthRange() {
			LocalDateTime targetDate = LocalDateTime.of(2026, 6, 15, 10, 30);

			when(incomeExpenseRepository.sumExpenseAmountByChildUserIdAndMonth(
					eq(CHILD_USER_ID),
					eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
					eq(LocalDateTime.of(2026, 7, 1, 0, 0))))
					.thenReturn(1234);

			Integer result = incomeExpenseService.getMonthlyExpenseAmount(CHILD_USER_ID, targetDate);

			assertThat(result).isEqualTo(1234);
		}

		private <T> T eq(T value) {
			return org.mockito.ArgumentMatchers.eq(value);
		}
	}

	@Nested
	@DisplayName("getRecords / getIncomeRecords / getExpenseRecords")
	class GetRecords {

		@Test
		@DisplayName("エンティティをDTOに変換して返す")
		void getRecords_convertsToDto() {
			IncomeExpenseEntity entity = new IncomeExpenseEntity();
			entity.setIncomeExpenseId(1);
			entity.setChildUser(childUser);
			entity.setRecordType(0);
			entity.setAmount(100);
			entity.setRegisteredDate(LocalDateTime.now());

			when(incomeExpenseRepository.findByChildUserUserId(CHILD_USER_ID)).thenReturn(List.of(entity));

			var result = incomeExpenseService.getRecords(CHILD_USER_ID);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getIncomeExpenseId()).isEqualTo(1);
			assertThat(result.get(0).getChildUserId()).isEqualTo(CHILD_USER_ID);
		}
	}
}

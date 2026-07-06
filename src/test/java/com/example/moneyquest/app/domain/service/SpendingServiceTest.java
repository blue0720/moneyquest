package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.SpendingLimitDto;
import com.example.moneyquest.app.infra.entity.SpendingLimitEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.SpendingLimitRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.QuestSendForm;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

/**
 * SpendingService の単体テスト。
 * 正常系に加え、対象未存在(NoSuchElementException)・権限不一致(SecurityException)の
 * 例外ハンドリングを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class SpendingServiceTest {

	@Mock
	private SpendingLimitRepository spendingLimitRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private IncomeExpenseService incomeExpenseService;

	@Mock
	private QuestService questService;

	@InjectMocks
	private SpendingService spendingService;

	private static final Integer PARENT_USER_ID = 1;
	private static final Integer CHILD_USER_ID = 10;
	private static final Integer OTHER_PARENT_USER_ID = 99;

	private UserEntity childUser;

	@BeforeEach
	void setUp() {
		childUser = new UserEntity();
		childUser.setUserId(CHILD_USER_ID);
		childUser.setParentUserId(PARENT_USER_ID);
	}

	@Nested
	@DisplayName("requestLimit")
	class RequestLimit {

		@Test
		@DisplayName("存在しない子供IDの場合はIllegalArgumentExceptionを投げる")
		void requestLimit_userNotFound_throws() {
			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.empty());

			SpendingLimitForm form = new SpendingLimitForm();
			form.setLimitAmount(1000);

			assertThatThrownBy(() -> spendingService.requestLimit(form, CHILD_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);

			verify(spendingLimitRepository, never()).save(any());
		}

		@Test
		@DisplayName("子供が存在する場合は申請中(0)ステータスで保存される")
		void requestLimit_success() {
			when(userRepository.findById(CHILD_USER_ID)).thenReturn(Optional.of(childUser));

			SpendingLimitForm form = new SpendingLimitForm();
			form.setLimitAmount(5000);

			spendingService.requestLimit(form, CHILD_USER_ID);

			verify(spendingLimitRepository, times(1)).save(argThatRequestStatusIs(0));
		}

		private SpendingLimitEntity argThatRequestStatusIs(int status) {
			return org.mockito.ArgumentMatchers.argThat(entity ->
					entity.getRequestStatus() != null && entity.getRequestStatus() == status);
		}
	}

	@Nested
	@DisplayName("getCurrentLimit")
	class GetCurrentLimit {

		@Test
		@DisplayName("承認済みの上限が存在しない場合はnullを返す")
		void getCurrentLimit_noApprovedLimit_returnsNull() {
			when(spendingLimitRepository
					.findFirstByChildUser_UserIdAndRequestStatusOrderByRegisteredDateDesc(CHILD_USER_ID, 1))
					.thenReturn(null);

			SpendingLimitDto result = spendingService.getCurrentLimit(CHILD_USER_ID);

			assertThat(result).isNull();
			verify(incomeExpenseService, never()).getMonthlyExpenseAmount(any(), any());
		}

		@Test
		@DisplayName("承認済みの上限が存在する場合は達成状況を含むDTOを返す")
		void getCurrentLimit_success() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setLimitAmount(5000);
			entity.setRequestStatus(1);

			when(spendingLimitRepository
					.findFirstByChildUser_UserIdAndRequestStatusOrderByRegisteredDateDesc(CHILD_USER_ID, 1))
					.thenReturn(entity);
			when(incomeExpenseService.getMonthlyExpenseAmount(any(), any())).thenReturn(3000);

			SpendingLimitDto result = spendingService.getCurrentLimit(CHILD_USER_ID);

			assertThat(result).isNotNull();
			assertThat(result.getCurrentExpenseAmount()).isEqualTo(3000);
			assertThat(result.getAchieved()).isTrue();
		}

		@Test
		@DisplayName("支出が上限を超えている場合は未達成となる")
		void getCurrentLimit_notAchieved() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setLimitAmount(1000);
			entity.setRequestStatus(1);

			when(spendingLimitRepository
					.findFirstByChildUser_UserIdAndRequestStatusOrderByRegisteredDateDesc(CHILD_USER_ID, 1))
					.thenReturn(entity);
			when(incomeExpenseService.getMonthlyExpenseAmount(any(), any())).thenReturn(2000);

			SpendingLimitDto result = spendingService.getCurrentLimit(CHILD_USER_ID);

			assertThat(result.getAchieved()).isFalse();
		}
	}

	@Nested
	@DisplayName("approveLimit")
	class ApproveLimit {

		@Test
		@DisplayName("存在しない申請IDの場合はIllegalArgumentExceptionを投げる")
		void approveLimit_notFound_throws() {
			when(spendingLimitRepository.findById(1)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> spendingService.approveLimit(1, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);

			verify(questService, never()).createQuest(any(), anyInt());
		}

		@Test
		@DisplayName("他の保護者が承認しようとした場合はSecurityExceptionを投げる")
		void approveLimit_wrongParent_throws() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setChildUser(childUser);

			when(spendingLimitRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> spendingService.approveLimit(1, OTHER_PARENT_USER_ID))
					.isInstanceOf(SecurityException.class)
					.hasMessageContaining("権限");

			verify(spendingLimitRepository, never()).save(any());
			verify(questService, never()).createQuest(any(), anyInt());
		}

		@Test
		@DisplayName("子供が紐づいていない申請を承認しようとした場合はSecurityExceptionを投げる")
		void approveLimit_noChildUser_throws() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setChildUser(null);

			when(spendingLimitRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> spendingService.approveLimit(1, PARENT_USER_ID))
					.isInstanceOf(SecurityException.class);
		}

		@Test
		@DisplayName("正しい保護者が承認した場合はステータスが承認済みになり確認クエストが作成される")
		void approveLimit_success() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setChildUser(childUser);
			entity.setLimitAmount(3000);
			entity.setRequestStatus(0);
			entity.setRegisteredDate(LocalDateTime.now());

			when(spendingLimitRepository.findById(1)).thenReturn(Optional.of(entity));

			spendingService.approveLimit(1, PARENT_USER_ID);

			assertThat(entity.getRequestStatus()).isEqualTo(1);
			verify(spendingLimitRepository, times(1)).save(entity);
			verify(questService, times(1)).createQuest(any(QuestSendForm.class), org.mockito.ArgumentMatchers.eq(PARENT_USER_ID));
		}
	}

	@Nested
	@DisplayName("rejectLimit")
	class RejectLimit {

		@Test
		@DisplayName("存在しない申請IDの場合はIllegalArgumentExceptionを投げる")
		void rejectLimit_notFound_throws() {
			when(spendingLimitRepository.findById(1)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> spendingService.rejectLimit(1, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("他の保護者が却下しようとした場合はSecurityExceptionを投げる")
		void rejectLimit_wrongParent_throws() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setChildUser(childUser);

			when(spendingLimitRepository.findById(1)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> spendingService.rejectLimit(1, OTHER_PARENT_USER_ID))
					.isInstanceOf(SecurityException.class);

			verify(spendingLimitRepository, never()).save(any());
		}

		@Test
		@DisplayName("正しい保護者が却下した場合はステータスが却下済みになる")
		void rejectLimit_success() {
			SpendingLimitEntity entity = new SpendingLimitEntity();
			entity.setSpendingLimitId(1);
			entity.setChildUser(childUser);
			entity.setRequestStatus(0);

			when(spendingLimitRepository.findById(1)).thenReturn(Optional.of(entity));

			spendingService.rejectLimit(1, PARENT_USER_ID);

			assertThat(entity.getRequestStatus()).isEqualTo(2);
			verify(spendingLimitRepository, times(1)).save(entity);
		}
	}
}

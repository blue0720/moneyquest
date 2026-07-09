package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

import com.example.moneyquest.app.domain.model.QuestDay;
import com.example.moneyquest.app.infra.entity.QuestEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.QuestRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.QuestSendForm;

/**
 * QuestService の単体テスト。
 * 正常系に加え、対象未存在・権限不一致による例外ハンドリングを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

	@Mock
	private QuestRepository questRepository;

	@Mock
	private IncomeExpenseService incomeExpenseService;

	@Mock
	private CharacterService characterService;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private QuestService questService;

	private UserEntity childUser;
	private static final Integer PARENT_USER_ID = 1;
	private static final Integer CHILD_USER_ID = 10;
	private static final Integer OTHER_PARENT_USER_ID = 99;

	@BeforeEach
	void setUp() {
		childUser = new UserEntity();
		childUser.setUserId(CHILD_USER_ID);
		childUser.setParentUserId(PARENT_USER_ID);
	}

	@Nested
	@DisplayName("createQuest")
	class CreateQuest {

		@Test
		@DisplayName("親に紐づく子供が指定された場合はクエストが作成される")
		void createQuest_success() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setDescription("お皿洗い");
			form.setRewardAmount(100);
			form.setExp(20);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getTitle()).isEqualTo("お手伝い");
			assertThat(result.getStatus()).isEqualTo(4);
			assertThat(result.getExp()).isEqualTo(20);
			verify(questRepository, times(1)).save(any(QuestEntity.class));
		}

		@Test
		@DisplayName("expが未指定の場合はデフォルト値5が設定される")
		void createQuest_defaultExp() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);
			form.setExp(null);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getExp()).isEqualTo(5);
		}

		@Test
		@DisplayName("coinRewardが未指定の場合は0で作成される")
		void createQuest_defaultCoinReward() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getCoinReward()).isEqualTo(0);
		}

		@Test
		@DisplayName("coinRewardが指定された場合はその値で作成される")
		void createQuest_specificCoinReward() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);
			form.setCoinReward(15);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getCoinReward()).isEqualTo(15);
		}

		@Test
		@DisplayName("availableDaysが未指定の場合は全曜日(127)で作成される")
		void createQuest_defaultAvailableDays() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getAvailableDays()).isEqualTo(QuestDay.ALL_DAYS_MASK);
		}

		@Test
		@DisplayName("availableDaysが指定された場合は該当曜日のビットマスクで作成される")
		void createQuest_specificAvailableDays() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);
			form.setAvailableDays(List.of(QuestDay.MON, QuestDay.WED));

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getAvailableDays()).isEqualTo(QuestDay.MON.getBit() | QuestDay.WED.getBit());
		}

		@Test
		@DisplayName("specificDateが未指定の場合はnullで作成される（日付による制限なし）")
		void createQuest_defaultSpecificDate() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getSpecificDate()).isNull();
		}

		@Test
		@DisplayName("specificDateが指定された場合はその日付で作成される")
		void createQuest_specificDate() {
			LocalDate date = LocalDate.of(2026, 8, 15);
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);
			form.setSpecificDate(date);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));
			when(questRepository.save(any(QuestEntity.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.createQuest(form, PARENT_USER_ID);

			assertThat(result.getSpecificDate()).isEqualTo(date);
		}

		@Test
		@DisplayName("指定した子供が自分の子ではない場合はIllegalArgumentExceptionを投げる")
		void createQuest_childNotOwned_throws() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(999); // 親に紐づかない子供ID

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of(childUser));

			assertThatThrownBy(() -> questService.createQuest(form, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);

			verify(questRepository, never()).save(any());
		}

		@Test
		@DisplayName("親に子供が1人も紐づいていない場合はIllegalArgumentExceptionを投げる")
		void createQuest_noChildren_throws() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);

			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());

			assertThatThrownBy(() -> questService.createQuest(form, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("updateQuest")
	class UpdateQuest {

		@Test
		@DisplayName("存在しないクエストIDの場合はIllegalArgumentExceptionを投げる")
		void updateQuest_notFound_throws() {
			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);

			when(questRepository.findById(123)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> questService.updateQuest(form, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("見つかりません");
		}

		@Test
		@DisplayName("他の保護者のクエストを更新しようとした場合は権限エラーを投げる")
		void updateQuest_wrongParent_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser); // parentUserId = 1

			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.updateQuest(form, OTHER_PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("権限");

			verify(questRepository, never()).save(any());
		}

		@Test
		@DisplayName("紐づく子供がいないクエストを更新しようとした場合は権限エラーを投げる")
		void updateQuest_noChildUser_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(null);

			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.updateQuest(form, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("正しい保護者が更新した場合は内容が反映されステータスが5になる")
		void updateQuest_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setExp(10);

			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);
			form.setTitle("更新後タイトル");
			form.setDescription("更新後説明");
			form.setRewardAmount(200);
			form.setExp(null); // 未指定なら既存値を維持

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.updateQuest(form, PARENT_USER_ID);

			assertThat(result.getTitle()).isEqualTo("更新後タイトル");
			assertThat(result.getStatus()).isEqualTo(5);
			assertThat(result.getExp()).isEqualTo(10); // 既存値維持
		}

		@Test
		@DisplayName("availableDays未指定の場合は既存値が維持される")
		void updateQuest_availableDaysNotSpecified_keepsExisting() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setAvailableDays(QuestDay.MON.getBit());

			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);
			form.setTitle("更新後タイトル");
			form.setRewardAmount(200);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.updateQuest(form, PARENT_USER_ID);

			assertThat(result.getAvailableDays()).isEqualTo(QuestDay.MON.getBit());
		}

		@Test
		@DisplayName("availableDaysが指定された場合は新しいビットマスクで上書きされる")
		void updateQuest_availableDaysSpecified_overwrites() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setAvailableDays(QuestDay.MON.getBit());

			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);
			form.setTitle("更新後タイトル");
			form.setRewardAmount(200);
			form.setAvailableDays(List.of(QuestDay.SAT, QuestDay.SUN));

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.updateQuest(form, PARENT_USER_ID);

			assertThat(result.getAvailableDays()).isEqualTo(QuestDay.SAT.getBit() | QuestDay.SUN.getBit());
		}

		@Test
		@DisplayName("specificDateを指定すると上書きされる")
		void updateQuest_specificDateSpecified_overwrites() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setSpecificDate(LocalDate.of(2026, 1, 1));

			LocalDate newDate = LocalDate.of(2026, 8, 15);
			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);
			form.setTitle("更新後タイトル");
			form.setRewardAmount(200);
			form.setSpecificDate(newDate);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.updateQuest(form, PARENT_USER_ID);

			assertThat(result.getSpecificDate()).isEqualTo(newDate);
		}

		@Test
		@DisplayName("specificDateを未指定(null)で更新すると日付指定が解除される")
		void updateQuest_specificDateCleared() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setSpecificDate(LocalDate.of(2026, 1, 1));

			QuestSendForm form = new QuestSendForm();
			form.setQuestId(123);
			form.setTitle("更新後タイトル");
			form.setRewardAmount(200);
			form.setSpecificDate(null);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			QuestEntity result = questService.updateQuest(form, PARENT_USER_ID);

			assertThat(result.getSpecificDate()).isNull();
		}
	}

	@Nested
	@DisplayName("deleteQuest")
	class DeleteQuest {

		@Test
		@DisplayName("存在しないクエストIDの場合はIllegalArgumentExceptionを投げる")
		void deleteQuest_notFound_throws() {
			when(questRepository.findById(123)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> questService.deleteQuest(123, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);

			verify(questRepository, never()).delete(any());
		}

		@Test
		@DisplayName("他の保護者が削除しようとした場合は権限エラーを投げる")
		void deleteQuest_wrongParent_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.deleteQuest(123, OTHER_PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);

			verify(questRepository, never()).delete(any());
		}

		@Test
		@DisplayName("正しい保護者が削除した場合はリポジトリのdeleteが呼ばれる")
		void deleteQuest_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			questService.deleteQuest(123, PARENT_USER_ID);

			verify(questRepository, times(1)).delete(quest);
		}
	}

	@Nested
	@DisplayName("requestComplete")
	class RequestComplete {

		@Test
		@DisplayName("存在しないクエストIDの場合はIllegalArgumentExceptionを投げる")
		void requestComplete_notFound_throws() {
			when(questRepository.findById(123)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> questService.requestComplete(123, CHILD_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("他の子供が完了申請しようとした場合は権限エラーを投げる")
		void requestComplete_wrongChild_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.requestComplete(123, 12345))
					.isInstanceOf(IllegalArgumentException.class);

			verify(questRepository, never()).save(any());
		}

		@Test
		@DisplayName("本人が完了申請した場合はステータスが1になる")
		void requestComplete_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.requestComplete(123, CHILD_USER_ID);

			assertThat(quest.getStatus()).isEqualTo(1);
		}

		@Test
		@DisplayName("今日が実施可能曜日に含まれる場合は完了申請できる")
		void requestComplete_availableToday_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			DayOfWeek today = LocalDate.now().getDayOfWeek();
			quest.setAvailableDays(QuestDay.fromDayOfWeek(today).getBit());

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.requestComplete(123, CHILD_USER_ID);

			assertThat(quest.getStatus()).isEqualTo(1);
		}

		@Test
		@DisplayName("今日が実施可能曜日に含まれない場合はIllegalArgumentExceptionを投げる")
		void requestComplete_unavailableToday_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			DayOfWeek today = LocalDate.now().getDayOfWeek();
			int maskWithoutToday = QuestDay.ALL_DAYS_MASK & ~QuestDay.fromDayOfWeek(today).getBit();
			quest.setAvailableDays(maskWithoutToday);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.requestComplete(123, CHILD_USER_ID))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("実施できません");

			verify(questRepository, never()).save(any());
		}

		@Test
		@DisplayName("specificDateが今日の場合は完了申請できる")
		void requestComplete_specificDateToday_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setSpecificDate(LocalDate.now());

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.requestComplete(123, CHILD_USER_ID);

			assertThat(quest.getStatus()).isEqualTo(1);
		}

		@Test
		@DisplayName("specificDateが今日ではない場合はIllegalArgumentExceptionを投げる（曜日が一致していても）")
		void requestComplete_specificDateNotToday_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setSpecificDate(LocalDate.now().plusDays(1));
			quest.setAvailableDays(QuestDay.ALL_DAYS_MASK);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.requestComplete(123, CHILD_USER_ID))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("実施できません");

			verify(questRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("approveQuest")
	class ApproveQuest {

		@Test
		@DisplayName("存在しないクエストIDの場合はIllegalArgumentExceptionを投げる")
		void approveQuest_notFound_throws() {
			UserEntity parentUser = new UserEntity();
			parentUser.setUserId(PARENT_USER_ID);

			when(questRepository.findById(123)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> questService.approveQuest(new QuestSendForm(), 123, parentUser))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("他の保護者が承認しようとした場合は権限エラーを投げ、収入・経験値は加算されない")
		void approveQuest_wrongParent_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			UserEntity otherParent = new UserEntity();
			otherParent.setUserId(OTHER_PARENT_USER_ID);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.approveQuest(new QuestSendForm(), 123, otherParent))
					.isInstanceOf(IllegalArgumentException.class);

			verify(incomeExpenseService, never()).createRecord(any(), any(), anyInt());
			verify(characterService, never()).addExp(anyInt(), anyInt());
		}

		@Test
		@DisplayName("正しい保護者が承認した場合は収入登録・経験値加算・実績加算が行われる")
		void approveQuest_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setRewardAmount(150);
			quest.setExp(30);
			quest.setCoinReward(20);
			quest.setTitle("宿題");
			quest.setDescription("算数ドリル");

			UserEntity parentUser = new UserEntity();
			parentUser.setUserId(PARENT_USER_ID);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.approveQuest(new QuestSendForm(), 123, parentUser);

			assertThat(quest.getStatus()).isEqualTo(2);
			verify(incomeExpenseService, times(1)).createRecord(any(), eq(parentUser), eq(CHILD_USER_ID));
			verify(characterService, times(1)).addExp(CHILD_USER_ID, 30);
			verify(characterService, times(1)).incrementAchievement(CHILD_USER_ID);
			verify(characterService, times(1)).addCoins(CHILD_USER_ID, 20);
		}

		@Test
		@DisplayName("expがnullのクエストを承認した場合はデフォルト値5が加算される")
		void approveQuest_defaultExp() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setRewardAmount(150);
			quest.setExp(null);

			UserEntity parentUser = new UserEntity();
			parentUser.setUserId(PARENT_USER_ID);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.approveQuest(new QuestSendForm(), 123, parentUser);

			verify(characterService, times(1)).addExp(CHILD_USER_ID, 5);
		}

		@Test
		@DisplayName("coinRewardがnullのクエストを承認した場合はコインが加算されない(0扱い)")
		void approveQuest_nullCoinReward() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);
			quest.setRewardAmount(150);
			quest.setCoinReward(null);

			UserEntity parentUser = new UserEntity();
			parentUser.setUserId(PARENT_USER_ID);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.approveQuest(new QuestSendForm(), 123, parentUser);

			verify(characterService, times(1)).addCoins(CHILD_USER_ID, 0);
		}
	}

	@Nested
	@DisplayName("rejectQuest")
	class RejectQuest {

		@Test
		@DisplayName("存在しないクエストIDの場合はIllegalArgumentExceptionを投げる")
		void rejectQuest_notFound_throws() {
			when(questRepository.findById(123)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> questService.rejectQuest(123, PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("他の保護者が却下しようとした場合は権限エラーを投げる")
		void rejectQuest_wrongParent_throws() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));

			assertThatThrownBy(() -> questService.rejectQuest(123, OTHER_PARENT_USER_ID))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("正しい保護者が却下した場合はステータスが3になる")
		void rejectQuest_success() {
			QuestEntity quest = new QuestEntity();
			quest.setQuestId(123);
			quest.setChildUser(childUser);

			when(questRepository.findById(123)).thenReturn(Optional.of(quest));
			when(questRepository.save(any(QuestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

			questService.rejectQuest(123, PARENT_USER_ID);

			assertThat(quest.getStatus()).isEqualTo(3);
		}
	}

	@Nested
	@DisplayName("曜日ヘルパー")
	class DayHelpers {

		@Test
		@DisplayName("availableDaysがnullの場合は全曜日として扱われる")
		void isAvailableToday_nullTreatedAsAllDays() {
			assertThat(questService.isAvailableToday(null)).isTrue();
		}

		@Test
		@DisplayName("formatAvailableDaysは全曜日の場合「毎日」を返す")
		void formatAvailableDays_allDays() {
			assertThat(questService.formatAvailableDays(QuestDay.ALL_DAYS_MASK)).isEqualTo("毎日");
		}

		@Test
		@DisplayName("formatAvailableDaysは一部曜日の場合は「・」区切りで返す")
		void formatAvailableDays_partialDays() {
			int mask = QuestDay.MON.getBit() | QuestDay.WED.getBit();
			assertThat(questService.formatAvailableDays(mask)).isEqualTo("月・水");
		}

		@Test
		@DisplayName("getAvailableDayCodesは選択曜日をCSV形式のenum名で返す")
		void getAvailableDayCodes_returnsCsv() {
			int mask = QuestDay.MON.getBit() | QuestDay.FRI.getBit();
			assertThat(questService.getAvailableDayCodes(mask)).isEqualTo("MON,FRI");
		}
	}

	@Nested
	@DisplayName("markQuestsViewed")
	class MarkQuestsViewed {

		@Test
		@DisplayName("新規/更新状態のクエストがある場合は未完了状態に更新されsaveAllが呼ばれる")
		void markQuestsViewed_updatesNewAndUpdatedStatuses() {
			QuestEntity newQuest = new QuestEntity();
			newQuest.setStatus(4);
			QuestEntity updatedQuest = new QuestEntity();
			updatedQuest.setStatus(5);

			when(questRepository.findByChildUserUserIdAndStatusIn(CHILD_USER_ID, List.of(0, 1, 4, 5)))
					.thenReturn(List.of(newQuest, updatedQuest));

			questService.markQuestsViewed(CHILD_USER_ID);

			assertThat(newQuest.getStatus()).isEqualTo(0);
			assertThat(updatedQuest.getStatus()).isEqualTo(0);
			verify(questRepository, times(1)).saveAll(List.of(newQuest, updatedQuest));
		}

		@Test
		@DisplayName("対象クエストがない場合はsaveAllを呼ばない")
		void markQuestsViewed_noTargets_doesNotSave() {
			when(questRepository.findByChildUserUserIdAndStatusIn(CHILD_USER_ID, List.of(0, 1, 4, 5)))
					.thenReturn(List.of());

			questService.markQuestsViewed(CHILD_USER_ID);

			verify(questRepository, never()).saveAll(any());
		}
	}
}

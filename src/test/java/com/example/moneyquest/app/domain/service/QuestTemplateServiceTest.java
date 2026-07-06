package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.QuestTemplateDto;
import com.example.moneyquest.app.infra.entity.QuestTemplateEntity;
import com.example.moneyquest.app.infra.repository.QuestTemplateRepository;
import com.example.moneyquest.app.presentation.form.QuestTemplateForm;

/**
 * QuestTemplateService の単体テスト。
 * 対象未存在(所有者不一致含む)の場合の例外ハンドリングを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class QuestTemplateServiceTest {

	@Mock
	private QuestTemplateRepository questTemplateRepository;

	@InjectMocks
	private QuestTemplateService questTemplateService;

	private static final Integer PARENT_USER_ID = 1;

	@Nested
	@DisplayName("findByParentUserId")
	class FindByParentUserId {

		@Test
		@DisplayName("テンプレート一覧をDTOに変換して返す")
		void findByParentUserId_success() {
			QuestTemplateEntity entity = new QuestTemplateEntity();
			entity.setQuestTemplateId(1);
			entity.setTitle("お皿洗い");
			entity.setRewardAmount(100);
			entity.setExp(10);

			when(questTemplateRepository.findByParentUser_UserId(PARENT_USER_ID)).thenReturn(List.of(entity));

			List<QuestTemplateDto> result = questTemplateService.findByParentUserId(PARENT_USER_ID);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getTitle()).isEqualTo("お皿洗い");
		}
	}

	@Nested
	@DisplayName("addTemplate")
	class AddTemplate {

		@Test
		@DisplayName("expが未指定の場合はデフォルト値5が設定される")
		void addTemplate_defaultExp() {
			QuestTemplateForm form = new QuestTemplateForm();
			form.setTitle("お風呂掃除");
			form.setRewardAmount(200);
			form.setExp(null);

			questTemplateService.addTemplate(PARENT_USER_ID, form);

			verify(questTemplateRepository).save(org.mockito.ArgumentMatchers.argThat(entity ->
					entity.getExp() == 5 && entity.getParentUser().getUserId().equals(PARENT_USER_ID)));
		}

		@Test
		@DisplayName("expが指定されている場合はその値が保存される")
		void addTemplate_explicitExp() {
			QuestTemplateForm form = new QuestTemplateForm();
			form.setTitle("お風呂掃除");
			form.setRewardAmount(200);
			form.setExp(15);

			questTemplateService.addTemplate(PARENT_USER_ID, form);

			verify(questTemplateRepository).save(org.mockito.ArgumentMatchers.argThat(entity -> entity.getExp() == 15));
		}
	}

	@Nested
	@DisplayName("updateTemplate")
	class UpdateTemplate {

		@Test
		@DisplayName("対象テンプレートが見つからない場合はIllegalArgumentExceptionを投げる")
		void updateTemplate_notFound_throws() {
			QuestTemplateForm form = new QuestTemplateForm();
			form.setQuestTemplateId(1);

			when(questTemplateRepository.findByQuestTemplateIdAndParentUser_UserId(1, PARENT_USER_ID))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> questTemplateService.updateTemplate(PARENT_USER_ID, form))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("見つかりません");

			verify(questTemplateRepository, never()).save(any());
		}

		@Test
		@DisplayName("他の保護者のテンプレートは自分のものとして取得できず更新できない")
		void updateTemplate_wrongOwner_throwsBecauseQueryScopedByOwner() {
			QuestTemplateForm form = new QuestTemplateForm();
			form.setQuestTemplateId(1);

			// findByQuestTemplateIdAndParentUser_UserId はDB側でオーナー一致を条件に含むため、
			// 別の保護者が指定した場合はOptional.emptyが返る想定
			when(questTemplateRepository.findByQuestTemplateIdAndParentUser_UserId(1, 999))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> questTemplateService.updateTemplate(999, form))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("対象が見つかった場合は内容が更新される")
		void updateTemplate_success() {
			QuestTemplateEntity entity = new QuestTemplateEntity();
			entity.setQuestTemplateId(1);
			entity.setTitle("旧タイトル");

			QuestTemplateForm form = new QuestTemplateForm();
			form.setQuestTemplateId(1);
			form.setTitle("新タイトル");
			form.setDescription("新説明");
			form.setRewardAmount(300);
			form.setExp(null);

			when(questTemplateRepository.findByQuestTemplateIdAndParentUser_UserId(1, PARENT_USER_ID))
					.thenReturn(Optional.of(entity));

			questTemplateService.updateTemplate(PARENT_USER_ID, form);

			assertThat(entity.getTitle()).isEqualTo("新タイトル");
			assertThat(entity.getExp()).isEqualTo(5);
			verify(questTemplateRepository, times(1)).save(entity);
		}
	}

	@Nested
	@DisplayName("deleteTemplate")
	class DeleteTemplate {

		@Test
		@DisplayName("対象テンプレートが見つからない場合はIllegalArgumentExceptionを投げる")
		void deleteTemplate_notFound_throws() {
			when(questTemplateRepository.findByQuestTemplateIdAndParentUser_UserId(1, PARENT_USER_ID))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> questTemplateService.deleteTemplate(PARENT_USER_ID, 1))
					.isInstanceOf(IllegalArgumentException.class);

			verify(questTemplateRepository, never()).delete(any());
		}

		@Test
		@DisplayName("対象が見つかった場合は削除される")
		void deleteTemplate_success() {
			QuestTemplateEntity entity = new QuestTemplateEntity();
			entity.setQuestTemplateId(1);

			when(questTemplateRepository.findByQuestTemplateIdAndParentUser_UserId(1, PARENT_USER_ID))
					.thenReturn(Optional.of(entity));

			questTemplateService.deleteTemplate(PARENT_USER_ID, 1);

			verify(questTemplateRepository, times(1)).delete(entity);
		}
	}
}

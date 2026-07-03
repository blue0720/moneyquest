package com.example.moneyquest.app.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.moneyquest.app.domain.model.QuestTemplateDto;
import com.example.moneyquest.app.infra.entity.QuestTemplateEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.QuestTemplateRepository;
import com.example.moneyquest.app.presentation.form.QuestTemplateForm;

@Service
public class QuestTemplateService {

	private final QuestTemplateRepository questTemplateRepository;

	public QuestTemplateService(QuestTemplateRepository questTemplateRepository) {
		this.questTemplateRepository = questTemplateRepository;
	}

	public List<QuestTemplateDto> findByParentUserId(Integer parentUserId) {
		return questTemplateRepository.findByParentUser_UserId(parentUserId)
				.stream()
				.map(this::toDto)
				.toList();
	}

	public void addTemplate(Integer parentUserId, QuestTemplateForm form) {
		UserEntity parent = new UserEntity();
		parent.setUserId(parentUserId);

		QuestTemplateEntity entity = new QuestTemplateEntity();
		entity.setParentUser(parent);
		entity.setTitle(form.getTitle());
		entity.setDescription(form.getDescription());
		entity.setRewardAmount(form.getRewardAmount());
		entity.setExp(form.getExp() == null ? 5 : form.getExp());

		questTemplateRepository.save(entity);
	}

	public void updateTemplate(Integer parentUserId, QuestTemplateForm form) {
		QuestTemplateEntity entity =
				questTemplateRepository
						.findByQuestTemplateIdAndParentUser_UserId(
								form.getQuestTemplateId(),
								parentUserId)
						.orElseThrow(() -> new IllegalArgumentException("対象のテンプレートが見つかりません。"));

		entity.setTitle(form.getTitle());
		entity.setDescription(form.getDescription());
		entity.setRewardAmount(form.getRewardAmount());
		entity.setExp(form.getExp() == null ? 5 : form.getExp());

		questTemplateRepository.save(entity);
	}

	public void deleteTemplate(Integer parentUserId, Integer questTemplateId) {
		QuestTemplateEntity entity =
				questTemplateRepository
						.findByQuestTemplateIdAndParentUser_UserId(
								questTemplateId,
								parentUserId)
						.orElseThrow(() -> new IllegalArgumentException("対象のテンプレートが見つかりません。"));

		questTemplateRepository.delete(entity);
	}

	private QuestTemplateDto toDto(QuestTemplateEntity entity) {
		QuestTemplateDto dto = new QuestTemplateDto();
		dto.setQuestTemplateId(entity.getQuestTemplateId());
		dto.setTitle(entity.getTitle());
		dto.setDescription(entity.getDescription());
		dto.setRewardAmount(entity.getRewardAmount());
		dto.setExp(entity.getExp());
		return dto;
	}
}
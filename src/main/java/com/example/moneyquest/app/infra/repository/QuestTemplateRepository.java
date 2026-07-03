package com.example.moneyquest.app.infra.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.moneyquest.app.infra.entity.QuestTemplateEntity;

@Repository
public interface QuestTemplateRepository extends JpaRepository<QuestTemplateEntity, Integer> {

    List<QuestTemplateEntity> findByParentUser_UserId(Integer parentUserId);

    Optional<QuestTemplateEntity> findByQuestTemplateIdAndParentUser_UserId(
            Integer questTemplateId,
            Integer parentUserId);
}
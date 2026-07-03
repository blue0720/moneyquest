package com.example.moneyquest.app.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.moneyquest.app.infra.entity.QuestEntity;

@Repository
public interface QuestRepository extends JpaRepository<QuestEntity, Integer> {

	@EntityGraph(attributePaths = "childUser")
	List<QuestEntity> findByChildUserUserId(Integer userId);

	List<QuestEntity> findByStatus(Integer status);
	
	List<QuestEntity> findByChildUserUserIdAndStatusIn(Integer childUserId, List<Integer> statuses);
	

}

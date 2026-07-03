package com.example.moneyquest.app.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moneyquest.app.infra.entity.SpendingLimitEntity;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimitEntity, Integer> {

	List<SpendingLimitEntity> findByChildUser_ParentUserIdOrderByRegisteredDateDesc(
			Integer parentUserId);//保護者用履歴取得
	
	List<SpendingLimitEntity>findByChildUser_UserIdOrderByRegisteredDateDesc(
	        Integer childUserId);//子供用履歴取得

	List<SpendingLimitEntity> findByChildUser_ParentUserIdAndRequestStatusOrderByRegisteredDateDesc(
			Integer parentUserId,
			Integer requestStatus);//承認待ち一覧

	SpendingLimitEntity findFirstByChildUser_UserIdAndRequestStatusOrderByRegisteredDateDesc(
			Integer childUserId,
			Integer requestStatus);//現在の上限取得

}

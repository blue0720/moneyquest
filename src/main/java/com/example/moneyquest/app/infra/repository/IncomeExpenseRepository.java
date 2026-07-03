package com.example.moneyquest.app.infra.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.moneyquest.app.infra.entity.IncomeExpenseEntity;

@Repository
public interface IncomeExpenseRepository extends JpaRepository<IncomeExpenseEntity, Integer> {
	// 子供1人の収支一覧取得
	List<IncomeExpenseEntity> findByChildUserUserId(Integer childUserId);

	// 子供1人の収入・支出一覧取得
	List<IncomeExpenseEntity> findByChildUserUserIdAndRecordType(
			Integer childUserId, Integer recordType);

	// 保護者に紐づいた子供全員の収入一覧を取得
	List<IncomeExpenseEntity> findByChildUserParentUserIdAndRecordType(
			Integer parentUserId, Integer recordType);

	@Query("""
			SELECT
				COALESCE(
					SUM(
						CASE
							WHEN i.recordType = 0 THEN i.amount
							ELSE -i.amount
						END
					), 0
				)
			FROM IncomeExpenseEntity i
			WHERE i.childUser.userId = :childUserId
			""")
	Integer getCurrentMoney(Integer childUserId);
	
	
	//月の支出を取得(SpendingServiceで必要なため追記
	@Query("""
	        SELECT COALESCE(SUM(i.amount), 0)
	        FROM IncomeExpenseEntity i
	        WHERE i.childUser.userId = :childUserId
	        AND i.recordType = 1
	        AND i.registeredDate >= :startDate
	        AND i.registeredDate < :endDate
	        """)
	Integer sumExpenseAmountByChildUserIdAndMonth(
	        @Param("childUserId") Integer childUserId,
	        @Param("startDate") LocalDateTime startDate,
	        @Param("endDate") LocalDateTime endDate);

}
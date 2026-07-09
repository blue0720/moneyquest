package com.example.moneyquest.app.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.moneyquest.app.infra.entity.CharacterItemEntity;

/**
 * コインショップの購入済みアイテム（character_item_t）を管理するリポジトリ。
 */
public interface CharacterItemRepository extends JpaRepository<CharacterItemEntity, Integer> {

	@Query("SELECT i FROM CharacterItemEntity i WHERE i.childUser.userId = :childUserId")
	List<CharacterItemEntity> findByChildUserId(@Param("childUserId") Integer childUserId);

	@Query("SELECT COUNT(i) > 0 FROM CharacterItemEntity i "
			+ "WHERE i.childUser.userId = :childUserId AND i.itemCode = :itemCode")
	boolean existsByChildUserIdAndItemCode(@Param("childUserId") Integer childUserId,
			@Param("itemCode") String itemCode);

}

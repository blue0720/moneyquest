package com.example.moneyquest.app.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.moneyquest.app.infra.entity.CharacterEntity; 



/**
 * キャラクター（character_t）を管理・取得するリポジトリ。
 * User1人＝Character1体（child_user_id で紐づく）。
 */
public interface CharacterRepository extends JpaRepository<CharacterEntity, Integer> {

	@Query("SELECT c FROM CharacterEntity c WHERE c.childUser.userId = :childUserId")
	List<CharacterEntity> findByChildUserId(@Param("childUserId") Integer childUserId);

}

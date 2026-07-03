package com.example.moneyquest.app.infra.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.moneyquest.app.infra.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

	// ログイン認証用：メールアドレスでユーザーを1件取得
	Optional<UserEntity> findByMailAddress(String mailAddress);

	// ある保護者に紐づく子供の一覧を取得
	List<UserEntity> findByParentUserId(Integer parentUserId);

	// 権限種別でユーザー一覧を取得
	List<UserEntity> findByAuthority(Integer authority);

}
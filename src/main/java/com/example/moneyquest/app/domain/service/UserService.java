package com.example.moneyquest.app.domain.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.moneyquest.app.domain.model.UserDto;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.CharacterRepository;
import com.example.moneyquest.app.infra.repository.IncomeExpenseRepository;
import com.example.moneyquest.app.infra.repository.QuestRepository;
import com.example.moneyquest.app.infra.repository.SpendingLimitRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.UserForm;

import lombok.RequiredArgsConstructor;

/**
 * アカウントの管理を行うService
 * パスワードはBCryptでハッシュ化して保存
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final CharacterService characterService;
	private final PasswordEncoder passwordEncoder;
	private final QuestRepository questRepository;
	private final IncomeExpenseRepository incomeExpenseRepository;
	private final SpendingLimitRepository spendingLimitRepository;
	private final CharacterRepository characterRepository;

	/**
	 * アカウント登録
	 */

	@Transactional
	public void createUser(Integer authority, UserForm form) {
		validateMailNotDuplicated(form.getMailAddress());
		UserEntity user = new UserEntity();
		user.setUserName(form.getUserName());
		user.setMailAddress(form.getMailAddress());
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setAuthority(authority);
		// 子供アカウントの場合のみ、ログイン中の保護者を親として紐づける
		if (authority != null && authority == CustomUserDetails.AUTHORITY_CHILD) {
			user.setParentUserId(currentLoginUserId());
		} else {
			user.setParentUserId(null);
		}
		userRepository.save(user);

		if (authority != null && authority == CustomUserDetails.AUTHORITY_CHILD) {
			characterService.createCharacter(user.getUserId());
		}
	}

	/**
	 * アカウント情報編集
	 */

	@Transactional
	public void updateUser(Integer userId, UserForm form) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("対象のアカウントが見つかりません"));
		validateMailNotDuplicatedForUpdate(userId, form.getMailAddress());
		user.setUserName(form.getUserName());
		user.setMailAddress(form.getMailAddress());
		// パスワードが入力された場合のみ更新（空欄ならのパスワードを維持）
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			user.setPassword(passwordEncoder.encode(form.getPassword()));
		}
		userRepository.save(user);
	}

	/**
	 * アカウント削除（管理者用）
	 * 保護者削除時は紐づく子供の全データを先に削除してから保護者を削除する
	 */
	@Transactional
	public void deleteUser(Integer userId) {
		for (UserEntity child : userRepository.findByParentUserId(userId)) {
			deleteChildData(child.getUserId());
		}
		userRepository.deleteById(userId);
	}

	/**
	 * 子供の関連データ＋子供ユーザーを削除する（内部ヘルパー）
	 * 削除順序: 収支記録 → クエスト → 支出上限 → キャラクター → ユーザー
	 */
	private void deleteChildData(Integer childId) {
		incomeExpenseRepository.deleteAll(incomeExpenseRepository.findByChildUserUserId(childId));
		questRepository.deleteAll(questRepository.findByChildUserUserId(childId));
		spendingLimitRepository.deleteAll(
				spendingLimitRepository.findByChildUser_UserIdOrderByRegisteredDateDesc(childId));
		characterRepository.deleteAll(characterRepository.findByChildUserId(childId));
		userRepository.deleteById(childId);
	}

	/**
	 * 家族一覧
	 */
	@Transactional
	public List<UserDto> getFamilyByParentId(Integer parentUserId) {
		List<UserDto> family = new ArrayList<>();
		userRepository.findById(parentUserId).ifPresent(parent -> family.add(toDto(parent)));
		for (UserEntity child : userRepository.findByParentUserId(parentUserId)) {
			family.add(toDto(child));
		}
		return family;
	}

	/**
	 * 保護者アカウント一覧
	 */
	@Transactional
	public List<UserDto> getParentList() {
		return toDtoList(userRepository.findByAuthority(CustomUserDetails.AUTHORITY_PARENT));
	}

	/**
	 * 管理者アカウント一覧
	 */
	@Transactional
	public List<UserDto> getAdminList() {
		return toDtoList(userRepository.findByAuthority(CustomUserDetails.AUTHORITY_ADMIN));
	}

	/**
	 * メールアドレス重複チェック（新規登録用）
	 */
	private void validateMailNotDuplicated(String mailAddress) {
		if (userRepository.findByMailAddress(mailAddress).isPresent()) {
			throw new IllegalStateException("このメールアドレスはすでに使用されています");
		}
	}

	/**
	 * メールアドレス重複チェック（編集用：自分自身は除外）
	 */
	private void validateMailNotDuplicatedForUpdate(Integer userId, String mailAddress) {
		userRepository.findByMailAddress(mailAddress).ifPresent(existing -> {
			if (!existing.getUserId().equals(userId)) {
				throw new IllegalStateException("このメールアドレスはすでに使用されています");
			}
		});
	}

	/**
	 * ログイン中のuserIdを取得
	 */
	private Integer currentLoginUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof CustomUserDetails loginUser) {
			return loginUser.getUserId();
		}
		return null;
	}

	private List<UserDto> toDtoList(List<UserEntity> users) {
		List<UserDto> list = new ArrayList<>();
		for (UserEntity u : users) {
			list.add(toDto(u));
		}
		return list;
	}

	private UserDto toDto(UserEntity user) {
		UserDto dto = new UserDto();
		dto.setUserId(user.getUserId());
		dto.setUserName(user.getUserName());
		dto.setAuthority(user.getAuthority());
		dto.setMailAddress(user.getMailAddress());
		return dto;
	}

	/**
	 * 子供アカウント登録
	 * 保護者IDを明示的に指定して紐づける
	 */
	@Transactional
	public void createChildUser(Integer parentUserId, UserForm form) {
		validateMailNotDuplicated(form.getMailAddress());

		UserEntity user = new UserEntity();
		user.setUserName(form.getUserName());
		user.setMailAddress(form.getMailAddress());
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
		user.setParentUserId(parentUserId);

		userRepository.save(user);

		characterService.createCharacter(user.getUserId());
	}

	/**
	 * 子供アカウント編集
	 */
	@Transactional
	public void updateChildUser(Integer parentUserId, Integer childUserId, UserForm form) {
		UserEntity child = userRepository.findById(childUserId)
				.orElseThrow(() -> new IllegalArgumentException("対象の子供アカウントが見つかりません"));

		if (child.getAuthority() == null
				|| !child.getAuthority().equals(CustomUserDetails.AUTHORITY_CHILD)
				|| !parentUserId.equals(child.getParentUserId())) {
			throw new IllegalStateException("この子供アカウントは編集できません");
		}

		validateMailNotDuplicatedForUpdate(childUserId, form.getMailAddress());
		child.setUserName(form.getUserName());
		child.setMailAddress(form.getMailAddress());

		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			child.setPassword(passwordEncoder.encode(form.getPassword()));
		}

		userRepository.save(child);
	}

	/**
	 * 家族アカウント削除（保護者用）
	 * 自己削除時は子供の全データも先に削除、子供削除時はその子のデータを先に削除
	 */
	@Transactional
	public void deleteFamilyUser(Integer loginParentUserId, Integer targetUserId) {
		UserEntity target = userRepository.findById(targetUserId)
				.orElseThrow(() -> new IllegalArgumentException("対象のアカウントが見つかりません"));

		boolean isSelf = loginParentUserId.equals(targetUserId);

		boolean isOwnChild = target.getAuthority() != null
				&& target.getAuthority().equals(CustomUserDetails.AUTHORITY_CHILD)
				&& loginParentUserId.equals(target.getParentUserId());

		if (!isSelf && !isOwnChild) {
			throw new IllegalStateException("このアカウントは削除できません");
		}

		if (isSelf) {
			// 保護者自己削除：子供の全データを先に削除してから自分を削除
			for (UserEntity child : userRepository.findByParentUserId(targetUserId)) {
				deleteChildData(child.getUserId());
			}
			userRepository.deleteById(targetUserId);
		} else {
			// 子供削除：その子のデータ＋子供ユーザーを削除
			deleteChildData(targetUserId);
		}
	}
}
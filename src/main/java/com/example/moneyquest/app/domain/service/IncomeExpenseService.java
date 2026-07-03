package com.example.moneyquest.app.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.moneyquest.app.domain.model.IncomeExpenseDto;
import com.example.moneyquest.app.infra.entity.IncomeExpenseEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.IncomeExpenseRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncomeExpenseService {

	private final IncomeExpenseRepository incomeExpenseRepository;
	private final UserRepository userRepository;

	// コンストラクタ自動生成

	// EntityからDtoへ変換
	private IncomeExpenseDto toDto(IncomeExpenseEntity entity) {
		IncomeExpenseDto dto = new IncomeExpenseDto();
		dto.setIncomeExpenseId(entity.getIncomeExpenseId());
		dto.setChildUserId(entity.getChildUser().getUserId());
		dto.setChildUserName(entity.getChildUser().getUserName());
		dto.setRecordType(entity.getRecordType());
		dto.setCategory(entity.getCategory());
		dto.setAmount(entity.getAmount());
		dto.setMemo(entity.getMemo());
		dto.setRegisteredDate(entity.getRegisteredDate());
		return dto;
	}

	// 子供1人の収支一覧取得
	public List<IncomeExpenseDto> getRecords(Integer childUserId) {
		return incomeExpenseRepository.findByChildUserUserId(childUserId)
				.stream().map(this::toDto).collect(Collectors.toList());
	}

	// 子供1人の収入一覧取得
	public List<IncomeExpenseDto> getIncomeRecords(Integer childUserId) {
		return incomeExpenseRepository.findByChildUserUserIdAndRecordType(childUserId, 0)
				.stream().map(this::toDto).collect(Collectors.toList());
	}

	// 子供1人の支出一覧取得
	public List<IncomeExpenseDto> getExpenseRecords(Integer childUserId) {
		return incomeExpenseRepository.findByChildUserUserIdAndRecordType(childUserId, 1)
				.stream().map(this::toDto).collect(Collectors.toList());
	}

	// 保護者用 全子供アカウントの収入一覧取得
	public List<IncomeExpenseDto> getParentIncomeRecords(Integer parentUserId) {

		List<UserEntity> children = userRepository.findByParentUserId(parentUserId);

		// 保護者アカウントに紐づいている全子供アカウントの収入記録を最新順にソートし表示
		return children.stream()
				.flatMap(child -> incomeExpenseRepository
						.findByChildUserUserIdAndRecordType(
								child.getUserId(), 0)
						.stream())
				.sorted((a, b) -> b.getRegisteredDate()
						.compareTo(a.getRegisteredDate()))
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	// 登録
	public void createRecord(IncomeExpenseForm form, UserEntity loginUser, Integer targetChildUserId) {

		int recordType;
		UserEntity childUser;

		if (loginUser.getAuthority() == 2) {
			recordType = 1; // 子供なら支出
			childUser = loginUser;
		} else {
			recordType = 0; // 保護者なら収入
			childUser = userRepository.findById(targetChildUserId)
					.orElseThrow(() -> new SecurityException("対象の子供が存在しません"));

			// 所有権チェック：自分の家族の子供以外には登録できない
			if (!loginUser.getUserId().equals(childUser.getParentUserId())) {
				throw new SecurityException("権限がありません");
			}
		}

		// 支出記録かつカテゴリが空欄だった場合エラーメッセージを表示
		if (recordType == 1 && (form.getCategory() == null || form.getCategory().isBlank())) {
			throw new SecurityException("なにをかったかおしえてね");
		}

		IncomeExpenseEntity entity = new IncomeExpenseEntity();

		// フォームの入力値をEntityに設定
		entity.setChildUser(childUser);
		entity.setRecordType(recordType);
		entity.setAmount(form.getAmount());
		entity.setMemo(form.getMemo());
		entity.setRegisteredDate(LocalDateTime.now());

		if (recordType == 1) {
			entity.setCategory(form.getCategory());
		} else {
			// 保護者からの収入はメモをカテゴリとして使用
			entity.setCategory(form.getMemo());
		}

		incomeExpenseRepository.save(entity);
	}

	// 更新
	public void updateRecord(IncomeExpenseForm form, UserEntity loginUser) {
		IncomeExpenseEntity entity = incomeExpenseRepository.findById(form.getIncomeExpenseId())
				.orElseThrow(() -> new SecurityException("データが存在しません"));

		// 所有権チェック
		if (loginUser.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
			if (!entity.getChildUser().getUserId().equals(loginUser.getUserId())) {
				throw new SecurityException("権限がありません");
			}

		} else if (loginUser.getAuthority() == CustomUserDetails.AUTHORITY_PARENT) {
			// 子供のparentUserIdが自分のuserIdと一致するか確認
			if (!entity.getChildUser().getParentUserId().equals(loginUser.getUserId())) {
				throw new SecurityException("権限がありません");
			}
		}

		// 支出記録かつカテゴリが空欄だった場合エラーメッセージを表示
		if (entity.getRecordType() == 1 && (form.getCategory() == null || form.getCategory().isBlank())) {
			throw new SecurityException("なにをかったかおしえてね");
		}

		// フォームの入力値をEntityに設定
		if (entity.getRecordType() == 1) {
			entity.setCategory(form.getCategory());
		}
		entity.setAmount(form.getAmount());
		entity.setMemo(form.getMemo());
		incomeExpenseRepository.save(entity);
	}

	// 削除
	public void deleteRecord(Integer incomeExpenseId, UserEntity loginUser) {
		IncomeExpenseEntity entity = incomeExpenseRepository.findById(incomeExpenseId)
				.orElseThrow(() -> new SecurityException("データが存在しません"));

		// 所有権チェック（子供の場合のみ）
		if (loginUser.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
			if (!entity.getChildUser().getUserId().equals(loginUser.getUserId())) {
				throw new SecurityException("権限がありません");
			}
		} else if (loginUser.getAuthority() == CustomUserDetails.AUTHORITY_PARENT) {
			// 子供のparentUserIdが自分のuserIdと一致するか確認
			if (!entity.getChildUser().getParentUserId().equals(loginUser.getUserId())) {
				throw new SecurityException("権限がありません");
			}
		}

		incomeExpenseRepository.delete(entity);
	}

	//月ごとの支出を取得
	public Integer getMonthlyExpenseAmount(
			Integer childUserId,
			LocalDateTime targetDate) {

		LocalDateTime startDate = targetDate.withDayOfMonth(1)
				.toLocalDate()
				.atStartOfDay();

		LocalDateTime endDate = startDate.plusMonths(1);

		return incomeExpenseRepository
				.sumExpenseAmountByChildUserIdAndMonth(
						childUserId,
						startDate,
						endDate);
	}
}
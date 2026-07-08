package com.example.moneyquest.app.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moneyquest.app.domain.model.QuestDay;
import com.example.moneyquest.app.infra.entity.QuestEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.QuestRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;
import com.example.moneyquest.app.presentation.form.QuestSendForm;

@Service
@Transactional
public class QuestService {

	QuestRepository questRepository;
	IncomeExpenseService incomeExpenseService;
	CharacterService characterService;
	UserRepository userRepository;

	public QuestService(QuestRepository questRepository,
			IncomeExpenseService incomeExpenseService,
			CharacterService characterService,
			UserRepository userRepository) {
		this.questRepository = questRepository;
		this.incomeExpenseService = incomeExpenseService;
		this.characterService = characterService;
		this.userRepository = userRepository;
	}

	/**
	 * 0. getQuestsByChild: 特定の子供のクエスト一覧を取得
	 * 
	 */
	@Transactional(readOnly = true)
	public List<QuestEntity> getQuestsByChild(Integer childId) {
		return questRepository.findByChildUserUserId(childId);
	}

	@Transactional(readOnly = true)
	public List<QuestEntity> getQuestsByChildAndStatuses(Integer childUserId, List<Integer> statuses) {
		return questRepository.findByChildUserUserIdAndStatusIn(childUserId, statuses);
	}

	/**
	 * 1. createQuest: 作成 (FormからEntityに詰め替えて保存、状態は「4:新規」)
	 */

	public QuestEntity createQuest(QuestSendForm form,Integer parentId) {
		// 1. 親IDに紐づく子供の一覧を取得
	    List<UserEntity> children = userRepository.findByParentUserId(parentId);
	    
	    // 2. その中から、フォームで指定された子供IDを持つ1件を探す
	    UserEntity child = children.stream()
	            .filter(c -> c.getUserId().equals(form.getChildUserId()))
	            .findFirst()
	            .orElseThrow(() -> new IllegalArgumentException());
		
		QuestEntity questEntity = new QuestEntity();

		UserEntity user = new UserEntity();
		user.setUserId(form.getChildUserId());

		questEntity.setChildUser(user);
		questEntity.setRewardAmount(form.getRewardAmount());
		questEntity.setExp(form.getExp() == null ? 5 : form.getExp());
		questEntity.setLimitAmount(form.getLimitAmount());
		questEntity.setDescription(form.getDescription());
		questEntity.setTitle(form.getTitle());
		questEntity.setAvailableDays(toBitmask(form.getAvailableDays()));
		questEntity.setStatus(4);
		questEntity.setRegisteredDate(LocalDateTime.now());
		questEntity.setUpdatedDate(LocalDateTime.now());

		return questRepository.save(questEntity);

	}

	/**
	 * 2. updateQuest: 更新 (Formの値で既存データを上書き、状態は「5:更新」)
	 */

	public QuestEntity updateQuest(QuestSendForm form, Integer parentUserId) {
		Integer questId = form.getQuestId();
		QuestEntity questEntity = questRepository.findById(questId)
				.orElseThrow(() -> new IllegalArgumentException("対象のクエストが見つかりません。"));

		if (questEntity.getChildUser() == null
				|| !parentUserId.equals(questEntity.getChildUser().getParentUserId())) {
			throw new IllegalArgumentException("この操作を行う権限がありません。");
		}

		questEntity.setRewardAmount(form.getRewardAmount());
		questEntity.setExp(form.getExp() == null ? questEntity.getExp() : form.getExp());
		questEntity.setDescription(form.getDescription());
		questEntity.setTitle(form.getTitle());
		if (form.getAvailableDays() != null && !form.getAvailableDays().isEmpty()) {
			questEntity.setAvailableDays(toBitmask(form.getAvailableDays()));
		}
		questEntity.setStatus(5);
		questEntity.setUpdatedDate(LocalDateTime.now());

		return questRepository.save(questEntity);

	}

	/**
	 * 3.deleteQuest: 削除
	 */

	public void deleteQuest(Integer questId, Integer parentUserId) {
		QuestEntity questEntity = questRepository.findById(questId)
				.orElseThrow(() -> new IllegalArgumentException("対象のクエストが見つかりません。"));

		if (questEntity.getChildUser() == null
				|| !parentUserId.equals(questEntity.getChildUser().getParentUserId())) {
			throw new IllegalArgumentException("この操作を行う権限がありません。");
		}

		questRepository.delete(questEntity);
	}

	/**
	 * 4. requestComplete: 完了申請 (状態を「1:申請中」に更新)
	 */

	public void requestComplete(Integer questId, Integer childUserId) {
		QuestEntity questEntity = questRepository.findById(questId)
				.orElseThrow(() -> new IllegalArgumentException("対象のクエストが見つかりません。"));

		if (questEntity.getChildUser() == null
				|| !childUserId.equals(questEntity.getChildUser().getUserId())) {
			throw new IllegalArgumentException("この操作を行う権限がありません。");
		}

		if (!isAvailableToday(questEntity.getAvailableDays())) {
			throw new IllegalArgumentException("今日はこのクエストを実施できません。");
		}

		questEntity.setStatus(1);
		questEntity.setUpdatedDate(LocalDateTime.now());
		questRepository.save(questEntity);
	}

	/**
	 * 5. getApprovalList: 承認待ち一覧 (status = 1 のものを取得)
	 */

	@Transactional(readOnly = true) // 読み取り専用でパフォーマンス最適化
	public List<QuestEntity> getApprovalList() {
		return questRepository.findByStatus(1);
	}

	/**
	 * 6. approveQuest: 承認 (状態を「2:承認済み」に更新)
	 */

	public void approveQuest(QuestSendForm form, Integer questId, UserEntity parentUser) {
		QuestEntity questEntity = questRepository.findById(questId)
				.orElseThrow(() -> new IllegalArgumentException("対象のクエストが見つかりません。"));

		if (questEntity.getChildUser() == null
				|| !parentUser.getUserId().equals(questEntity.getChildUser().getParentUserId())) {
			throw new IllegalArgumentException("この操作を行う権限がありません。");
		}

		questEntity.setStatus(2);
		questEntity.setUpdatedDate(LocalDateTime.now());
		questRepository.save(questEntity);

		// 収入情報追加

		IncomeExpenseForm incomeForm = new IncomeExpenseForm();

		incomeForm.setAmount(questEntity.getRewardAmount());
		incomeForm.setCategory("クエスト達成：" + questEntity.getTitle());
		incomeForm.setMemo(questEntity.getDescription());

		incomeExpenseService.createRecord(incomeForm, parentUser, questEntity.getChildUser().getUserId());

		Integer exp = questEntity.getExp() == null ? 5 : questEntity.getExp();
		characterService.addExp(questEntity.getChildUser().getUserId(), exp);
		characterService.incrementAchievement(questEntity.getChildUser().getUserId());
	}

	/**
	 * 7. rejectQuest: 却下 (状態を「3:却下」に更新)
	 */

	public void rejectQuest(Integer questId, Integer parentUserId) {
		QuestEntity quest = questRepository.findById(questId)
				.orElseThrow(() -> new IllegalArgumentException("対象のクエストが見つかりません。"));

		if (quest.getChildUser() == null
				|| !parentUserId.equals(quest.getChildUser().getParentUserId())) {
			throw new IllegalArgumentException("この操作を行う権限がありません。");
		}

		quest.setStatus(3);
		quest.setUpdatedDate(LocalDateTime.now());
		questRepository.save(quest);
	}

	/**
	 * 8. markQuestsViewed: タブ閲覧後に新規/更新状態を未完了に更新 (4:新規, 5:更新 ➔ 0:未完了)
	 */

	public void markQuestsViewed(Integer childUserId) {
		// 子供に紐づくクエストを全件取得
		List<Integer> activeStatuses = List.of(0, 1, 4, 5);
		List<QuestEntity> quests = questRepository.findByChildUserUserIdAndStatusIn(childUserId, activeStatuses);

		boolean isUpdated = false;
		for (QuestEntity questEntity : quests) {
			if (questEntity.getStatus() == 4 || questEntity.getStatus() == 5) {
				questEntity.setStatus(0);
				questEntity.setUpdatedDate(LocalDateTime.now());
				isUpdated = true;
			}
		}

		// 1件でも変更があった場合のみ一括保存を実行
		if (isUpdated) {
			questRepository.saveAll(quests);
		}
	}

	/**
	 * 実施可能曜日のリストをビットマスクに変換する。未指定(null/空)の場合は全曜日(毎日実施可能)。
	 */
	private Integer toBitmask(List<QuestDay> days) {
		if (days == null || days.isEmpty()) {
			return QuestDay.ALL_DAYS_MASK;
		}
		int mask = 0;
		for (QuestDay day : days) {
			mask |= day.getBit();
		}
		return mask;
	}

	/**
	 * 9. isAvailableToday: 今日がこのクエストの実施可能曜日か判定する。
	 * available_days が未設定(null)の場合は毎日実施可能として扱う（既存データとの後方互換）。
	 * ※ テンプレートからは呼び出さないこと。Thymeleafはth:eachのループ内でのBean参照(@bean)を解析エラーにするため、
	 *   表示用にはQuestEntity#isAvailableToday()等のインスタンスメソッド経由で参照する。
	 */
	@Transactional(readOnly = true)
	public boolean isAvailableToday(Integer availableDays) {
		return QuestDay.isAvailableToday(availableDays);
	}

	/**
	 * 10. formatAvailableDays: 実施可能曜日を表示用文字列に整形する（全曜日なら「毎日」）。
	 */
	@Transactional(readOnly = true)
	public String formatAvailableDays(Integer availableDays) {
		return QuestDay.format(availableDays);
	}

	/**
	 * 11. getAvailableDayCodes: 実施可能曜日をCSV形式のenum名(例: "MON,WED,FRI")で返す。
	 * 保護者画面の編集モーダルへ現在値を引き渡す(JS側でのチェックボックス復元)用途。
	 */
	@Transactional(readOnly = true)
	public String getAvailableDayCodes(Integer availableDays) {
		return QuestDay.toCsv(availableDays);
	}

}

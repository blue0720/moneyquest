package com.example.moneyquest.app.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moneyquest.app.domain.model.SpendingLimitDto;
import com.example.moneyquest.app.infra.entity.SpendingLimitEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.SpendingLimitRepository;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.form.QuestSendForm;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

    

@Service
@Transactional
public class SpendingService {

    private static final Integer REQUESTING = 0;
    private static final Integer APPROVED = 1;
    private static final Integer REJECTED = 2;

    private final SpendingLimitRepository spendingLimitRepository;
    private final UserRepository userRepository;
  
    private final IncomeExpenseService incomeExpenseService;
    private final QuestService questService;

    public SpendingService(
            SpendingLimitRepository spendingLimitRepository,
            UserRepository userRepository,
            IncomeExpenseService incomeExpenseService,
            QuestService questService
            ) {

        this.spendingLimitRepository = spendingLimitRepository;
        this.userRepository = userRepository;
        
        this.incomeExpenseService = incomeExpenseService;
        this.questService = questService;
    }

    /**
     * 子供：支出上限申請
     */
    public void requestLimit(
            SpendingLimitForm form,
            Integer childUserId) {

        UserEntity childUser =
                userRepository.findById(childUserId)
                        .orElseThrow();

        SpendingLimitEntity entity =
                new SpendingLimitEntity();

        entity.setChildUser(childUser);
        entity.setLimitAmount(form.getLimitAmount());
        entity.setRequestStatus(REQUESTING);
        entity.setRegisteredDate(LocalDateTime.now());
        entity.setUpdatedDate(LocalDateTime.now());

        spendingLimitRepository.save(entity);
    }

    /**
     * 子供：現在の上限と現在の支出を取得
     */
    @Transactional(readOnly = true)
    public SpendingLimitDto getCurrentLimit(
            Integer childUserId) {

        SpendingLimitEntity entity =
                spendingLimitRepository
                        .findFirstByChildUser_UserIdAndRequestStatusOrderByRegisteredDateDesc(
                                childUserId,
                                APPROVED);

        if (entity == null) {
            return null;
        }
        

        SpendingLimitDto dto =
                new SpendingLimitDto();
        
        Integer monthlyExpenseAmount =
                incomeExpenseService.getMonthlyExpenseAmount(childUserId,LocalDateTime.now());//今月の支出

        dto.setSpendingLimitId(entity.getSpendingLimitId());
        dto.setLimitAmount(entity.getLimitAmount());
        dto.setRequestStatus(entity.getRequestStatus());
        
        dto.setCurrentExpenseAmount(monthlyExpenseAmount);
        
        dto.setAchieved(monthlyExpenseAmount <= entity.getLimitAmount());

        return dto;
    }
    /**
     * 子供：最新の申請中の上限を1件取得//追加
     */
    @Transactional(readOnly = true)
    public SpendingLimitDto getCurrentPendingLimit(
            Integer childUserId) {

        SpendingLimitEntity entity =
                spendingLimitRepository
                        .findFirstByChildUser_UserIdAndRequestStatusOrderByRegisteredDateDesc(
                                childUserId,
                                REQUESTING);

        if (entity == null) {
            return null;
        }

        SpendingLimitDto dto =
                new SpendingLimitDto();

        dto.setSpendingLimitId(entity.getSpendingLimitId());
        dto.setLimitAmount(entity.getLimitAmount());
        dto.setRequestStatus(entity.getRequestStatus());

        return dto;
    }
    /**
     * 子供：自分の支出上限履歴
     */
    @Transactional(readOnly = true)
    public List<SpendingLimitDto> getChildLimitHistory(
            Integer childUserId) {

        List<SpendingLimitEntity> entityList =
                spendingLimitRepository
                        .findByChildUser_UserIdOrderByRegisteredDateDesc(
                                childUserId);

        List<SpendingLimitDto> dtoList = new ArrayList<>();
        List<String> addedApprovedMonths =new ArrayList<>();
        for (SpendingLimitEntity entity : entityList) {
            // 申請中は履歴に表示しない
            if (entity.getRequestStatus() == REQUESTING) {
                continue;
            }

            String targetMonth =
                    entity.getRegisteredDate().getYear()
                            + "-"
                            + String.format("%02d",
                                    entity.getRegisteredDate().getMonthValue());

            
            
            // 承認済みだけ、同じ月は最新1件だけ表示
            if (entity.getRequestStatus() == APPROVED) {

                if (addedApprovedMonths.contains(targetMonth)) {
                    continue;
                }

                addedApprovedMonths.add(targetMonth);
            }

        	
            SpendingLimitDto dto = new SpendingLimitDto();

            Integer monthlyExpenseAmount =
                    incomeExpenseService.getMonthlyExpenseAmount(
                            childUserId,
                            entity.getRegisteredDate());

            dto.setSpendingLimitId(entity.getSpendingLimitId());
            dto.setLimitAmount(entity.getLimitAmount());
            dto.setRequestStatus(entity.getRequestStatus());
            dto.setTargetMonth(
                    entity.getRegisteredDate().getYear()
                            + "-"
                            + String.format("%02d",
                                    entity.getRegisteredDate().getMonthValue()));
            dto.setMonthlyExpenseAmount(monthlyExpenseAmount);
            dto.setAchieved(
                    entity.getRequestStatus() == APPROVED
                            && monthlyExpenseAmount <= entity.getLimitAmount());

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * 保護者:子供全員の支出上限履歴
     */
    @Transactional(readOnly = true)
    public List<SpendingLimitDto> getParentLimitHistory(
            Integer parentUserId) {

        List<SpendingLimitEntity> entityList =
                spendingLimitRepository
                        .findByChildUser_ParentUserIdOrderByRegisteredDateDesc(
                                parentUserId);

        List<SpendingLimitDto> dtoList = new ArrayList<>();
        List<String> addedKeys = new ArrayList<>();
        for (SpendingLimitEntity entity : entityList) {
        	// 申請中は履歴に出さない
        	
        	if (entity.getRequestStatus() == REQUESTING) {
                continue;
            }

        	Integer childUserId = entity.getChildUser().getUserId();
        	
        	
        	String targetMonth =
                    entity.getRegisteredDate().getYear()
                            + "-"
                            + String.format("%02d",
                                    entity.getRegisteredDate().getMonthValue());

            String key = childUserId + "-" + targetMonth;
        	
         // 同じ子供・同じ月は最新1件だけ
            if (addedKeys.contains(key)) {
                continue;
            }

            addedKeys.add(key);

            Integer monthlyExpenseAmount =
                    incomeExpenseService.getMonthlyExpenseAmount(
                            childUserId,
                            entity.getRegisteredDate());
            
            

            SpendingLimitDto dto = new SpendingLimitDto();
            
            dto.setChildUserName(entity.getChildUser().getUserName());
            dto.setTargetMonth(targetMonth);
            dto.setMonthlyExpenseAmount(monthlyExpenseAmount);

            dto.setAchieved(
                    entity.getRequestStatus() == APPROVED
                            && monthlyExpenseAmount <= entity.getLimitAmount());

            dto.setSpendingLimitId(entity.getSpendingLimitId());
            dto.setLimitAmount(entity.getLimitAmount());
            dto.setRequestStatus(entity.getRequestStatus());

            dtoList.add(dto);
        }

        return dtoList;

    }

    /**
     * 保護者：承認待ち一覧
     */
    @Transactional(readOnly = true)
    public List<SpendingLimitDto> getPendingLimits(
            Integer parentUserId) {

        List<SpendingLimitEntity> entityList =
                spendingLimitRepository
                        .findByChildUser_ParentUserIdAndRequestStatusOrderByRegisteredDateDesc(
                                parentUserId,
                                REQUESTING);

        List<SpendingLimitDto> dtoList =
                new ArrayList<>();

        for (SpendingLimitEntity entity : entityList) {

            SpendingLimitDto dto =
                    new SpendingLimitDto();

            dto.setSpendingLimitId(entity.getSpendingLimitId());
            dto.setLimitAmount(entity.getLimitAmount());
            dto.setRequestStatus(entity.getRequestStatus());
            dto.setChildUserName(entity.getChildUser().getUserName());

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * 保護者：承認
     */
    public void approveLimit(
            Integer spendingLimitId,
            Integer parentUserId) {

        SpendingLimitEntity entity =
                spendingLimitRepository.findById(spendingLimitId)
                        .orElseThrow();

        if (entity.getChildUser() == null
                || !parentUserId.equals(entity.getChildUser().getParentUserId())) {
            throw new SecurityException("この操作を行う権限がありません。");
        }

        entity.setRequestStatus(APPROVED);
        entity.setUpdatedDate(LocalDateTime.now());

        spendingLimitRepository.save(entity);

// 承認時にクエスト作成
        QuestSendForm form =
                new QuestSendForm();

        form.setChildUserId(
                entity.getChildUser().getUserId());

        form.setTitle(
                "今月の上限をまもれたかな？");

        form.setDescription(
                "設定した支出上限を守れたか確認しよう");

        form.setRewardAmount(0);
        form.setExp(30);
        form.setLimitAmount(entity.getLimitAmount());

        questService.createQuest(form,parentUserId);
    }

    /**
     * 保護者：却下
     */
    public void rejectLimit(
            Integer spendingLimitId,
            Integer parentUserId) {

        SpendingLimitEntity entity =
                spendingLimitRepository.findById(spendingLimitId)
                        .orElseThrow();

        if (entity.getChildUser() == null
                || !parentUserId.equals(entity.getChildUser().getParentUserId())) {
            throw new SecurityException("この操作を行う権限がありません。");
        }

        entity.setRequestStatus(REJECTED);
        entity.setUpdatedDate(LocalDateTime.now());

        spendingLimitRepository.save(entity);
    }
}
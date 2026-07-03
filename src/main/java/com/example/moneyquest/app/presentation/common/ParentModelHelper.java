package com.example.moneyquest.app.presentation.common;

import java.util.HashMap;
import java.util.List;

import org.springframework.ui.Model;

import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;

public class ParentModelHelper {

    // 基本版（全部デフォルト）
    public static void setDefaults(
            Model model,
            CustomUserDetails loginUser,
            UserService userService,
            QuestService questService,
            QuestTemplateService questTemplateService,
            SpendingService spendingService) {
    	
    	Integer parentUserId = loginUser.getUserId();
    	
    	model.addAttribute("userName", loginUser.getUser().getUserName());
        model.addAttribute("currentMoneyMap", new HashMap<>());
        model.addAttribute("incomeExpenseList", List.of());
        model.addAttribute("questList", List.of());
        model.addAttribute("templateList", List.of());
        model.addAttribute("childList", List.of());
        model.addAttribute("familyList", List.of());
        
        Integer questApprovalCount = questService.getApprovalList().stream()
                .filter(quest -> quest.getChildUser() != null)
                .filter(quest -> parentUserId.equals(quest.getChildUser().getParentUserId()))
                .toList()
                .size();
        model.addAttribute("questApprovalCount", questApprovalCount);
        
        Integer limitApprovalCount = spendingService.getPendingLimits(parentUserId).size();
        model.addAttribute("limitApprovalCount", limitApprovalCount);
        
    
        var templateList = questTemplateService.findByParentUserId(parentUserId);
        model.addAttribute("templateCount", templateList.size());
        
        
        model.addAttribute("incomeExpenseForm", new IncomeExpenseForm());
        model.addAttribute("limitMap", new HashMap<>()); // ← 追加
    }

}
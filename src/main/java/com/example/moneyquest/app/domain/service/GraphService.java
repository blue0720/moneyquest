package com.example.moneyquest.app.domain.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moneyquest.app.domain.model.IncomeExpenseDto;
import com.example.moneyquest.app.domain.model.SavingDto;

/**
 * グラフ（収支・貯金）の集計サービス。
 * IncomeExpenseService から子供の収支データを取得し、画面表示用に集計する。
 *   ・現在の貯金額（FR-32）
 *   ・貯金額推移（折れ線, FR-33）
 *   ・月次収支（棒, FR-27/34〜36）
 *   ・累計収支（棒, FR-28/37）
 *
 * record_type は 0:収入 / 1:支出（income_expense_t 定義より）。
 */
@Service
@Transactional(readOnly = true)
public class GraphService {

	private static final int RECORD_TYPE_INCOME = 0;
	private static final int RECORD_TYPE_EXPENSE = 1;
	/** グラフ横軸のラベル書式。表示の都合で変えたければここを調整する。 */
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy/MM");

	private final IncomeExpenseService incomeExpenseService;

	public GraphService(IncomeExpenseService incomeExpenseService) {
		this.incomeExpenseService = incomeExpenseService;
	}

	/**
	 * グラフ画面用に、貯金・月次・累計をまとめて1つの SavingDto に詰めて返す。
	 * showGraph はこれを呼んで model に載せる想定（収支データの取得は1回で済む）。
	 */
	public SavingDto getGraphData(Integer childUserId) {
		List<IncomeExpenseDto> records = incomeExpenseService.getRecords(childUserId);
		List<YearMonth> months = sortedMonths(records);

		List<Integer> monthlyIncome = monthlySum(records, months, RECORD_TYPE_INCOME);
		List<Integer> monthlyExpense = monthlySum(records, months, RECORD_TYPE_EXPENSE);
		List<Integer> monthlyBalance = subtract(monthlyIncome, monthlyExpense);
		List<Integer> cumulativeBalance = runningTotal(monthlyBalance);

		int totalIncome = sum(monthlyIncome);
		int totalExpense = sum(monthlyExpense);

		SavingDto dto = new SavingDto();
		dto.setTotalIncome(totalIncome);
		dto.setTotalExpense(totalExpense);
		dto.setSaving(totalIncome - totalExpense);
		dto.setLavels(toLabels(months));
		dto.setMonthlyBalance(monthlyBalance);
		dto.setCumulativeBalance(cumulativeBalance);
		return dto;
	}

	// ===== メソッド一覧の8メソッド（単体でも呼べるよう公開） =====

	/** 現在の貯金額 = 収入合計 − 支出合計（FR-32）。 */
	public Integer calcSavings(Integer childUserId) {
		List<IncomeExpenseDto> records = incomeExpenseService.getRecords(childUserId);
		return totalByType(records, RECORD_TYPE_INCOME) - totalByType(records, RECORD_TYPE_EXPENSE);
	}

	/** 貯金額推移：月ごとの収支を累積した残高の系列（折れ線, FR-33）。 */
	public List<Integer> getSavingsTrend(Integer childUserId) {
		return getCumulativeBalance(childUserId);
	}

	/** 月次収入合計：月ごとの収入の系列（FR-35）。 */
	public List<Integer> getMonthlyIncome(Integer childUserId) {
		List<IncomeExpenseDto> records = incomeExpenseService.getRecords(childUserId);
		return monthlySum(records, sortedMonths(records), RECORD_TYPE_INCOME);
	}

	/** 月次支出合計：月ごとの支出の系列（FR-36）。 */
	public List<Integer> getMonthlyExpense(Integer childUserId) {
		List<IncomeExpenseDto> records = incomeExpenseService.getRecords(childUserId);
		return monthlySum(records, sortedMonths(records), RECORD_TYPE_EXPENSE);
	}

	/** 月次収支合計：月ごとの（収入 − 支出）の系列（棒, FR-34/27）。 */
	public List<Integer> getMonthlyBalance(Integer childUserId) {
		List<IncomeExpenseDto> records = incomeExpenseService.getRecords(childUserId);
		List<YearMonth> months = sortedMonths(records);
		return subtract(monthlySum(records, months, RECORD_TYPE_INCOME),
				monthlySum(records, months, RECORD_TYPE_EXPENSE));
	}

	/** 累計収入合計（全期間の収入合計, FR-37）。 */
	public Integer getCumulativeIncome(Integer childUserId) {
		return totalByType(incomeExpenseService.getRecords(childUserId), RECORD_TYPE_INCOME);
	}

	/** 累計支出合計（全期間の支出合計, FR-37）。 */
	public Integer getCumulativeExpense(Integer childUserId) {
		return totalByType(incomeExpenseService.getRecords(childUserId), RECORD_TYPE_EXPENSE);
	}

	/** 累計収支：月ごとの収支を累積した系列（棒, FR-28/37）。 */
	public List<Integer> getCumulativeBalance(Integer childUserId) {
		return runningTotal(getMonthlyBalance(childUserId));
	}

	// ===== private helpers =====

	/** 登録日時から月（YearMonth）を取り出し、重複を除いて昇順に並べる。 */
	private List<YearMonth> sortedMonths(List<IncomeExpenseDto> records) {
		return records.stream()
				.filter(r -> r.getRegisteredDate() != null)
				.map(r -> YearMonth.from(r.getRegisteredDate()))
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	/** 指定 record_type について、月ごとの合計を months の並び順で返す（その月が無ければ0）。 */
	private List<Integer> monthlySum(List<IncomeExpenseDto> records, List<YearMonth> months, int recordType) {
		Map<YearMonth, Integer> byMonth = new HashMap<>();
		for (IncomeExpenseDto r : records) {
			if (r.getRecordType() == null || r.getRecordType() != recordType || r.getRegisteredDate() == null) {
				continue;
			}
			YearMonth ym = YearMonth.from(r.getRegisteredDate());
			byMonth.merge(ym, amount(r), Integer::sum);
		}
		List<Integer> result = new ArrayList<>();
		for (YearMonth m : months) {
			result.add(byMonth.getOrDefault(m, 0));
		}
		return result;
	}

	/** 指定 record_type の全期間合計。 */
	private int totalByType(List<IncomeExpenseDto> records, int recordType) {
		int total = 0;
		for (IncomeExpenseDto r : records) {
			if (r.getRecordType() != null && r.getRecordType() == recordType) {
				total += amount(r);
			}
		}
		return total;
	}

	/** a - b を要素ごとに計算（同じ長さ・同じ月並び前提）。 */
	private List<Integer> subtract(List<Integer> a, List<Integer> b) {
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < a.size(); i++) {
			result.add(a.get(i) - b.get(i));
		}
		return result;
	}

	/** 月次系列を先頭から積み上げた累積系列にする。 */
	private List<Integer> runningTotal(List<Integer> monthly) {
		List<Integer> result = new ArrayList<>();
		int running = 0;
		for (Integer v : monthly) {
			running += v;
			result.add(running);
		}
		return result;
	}

	private List<String> toLabels(List<YearMonth> months) {
		List<String> labels = new ArrayList<>();
		for (YearMonth m : months) {
			labels.add(m.format(MONTH_LABEL));
		}
		return labels;
	}

	private int sum(List<Integer> values) {
		int total = 0;
		for (Integer v : values) {
			total += v;
		}
		return total;
	}

	/** amount の null 安全な取り出し（DBは NOT NULL だが念のため）。 */
	private int amount(IncomeExpenseDto r) {
		return r.getAmount() == null ? 0 : r.getAmount();
	}

}
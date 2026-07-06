package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.IncomeExpenseDto;
import com.example.moneyquest.app.domain.model.SavingDto;

/**
 * GraphService の単体テスト。
 * 月次・累計の集計ロジック、レコードが空/null日時を含む場合の挙動を確認する。
 */
@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

	@Mock
	private IncomeExpenseService incomeExpenseService;

	@InjectMocks
	private GraphService graphService;

	private static final Integer CHILD_USER_ID = 10;

	private IncomeExpenseDto record(int recordType, int amount, LocalDateTime date) {
		IncomeExpenseDto dto = new IncomeExpenseDto();
		dto.setRecordType(recordType);
		dto.setAmount(amount);
		dto.setRegisteredDate(date);
		return dto;
	}

	@Nested
	@DisplayName("getGraphData")
	class GetGraphData {

		@Test
		@DisplayName("レコードが1件もない場合は0埋めのSavingDtoを返す")
		void getGraphData_noRecords_returnsZeroed() {
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());

			SavingDto dto = graphService.getGraphData(CHILD_USER_ID);

			assertThat(dto.getTotalIncome()).isZero();
			assertThat(dto.getTotalExpense()).isZero();
			assertThat(dto.getSaving()).isZero();
			assertThat(dto.getLavels()).isEmpty();
			assertThat(dto.getMonthlyBalance()).isEmpty();
			assertThat(dto.getCumulativeBalance()).isEmpty();
		}

		@Test
		@DisplayName("複数月の収入・支出を月次集計し、累計残高を計算する")
		void getGraphData_aggregatesAcrossMonths() {
			List<IncomeExpenseDto> records = List.of(
					record(0, 1000, LocalDateTime.of(2026, 1, 10, 0, 0)),
					record(1, 300, LocalDateTime.of(2026, 1, 20, 0, 0)),
					record(0, 500, LocalDateTime.of(2026, 2, 5, 0, 0)),
					record(1, 800, LocalDateTime.of(2026, 2, 15, 0, 0)));

			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(records);

			SavingDto dto = graphService.getGraphData(CHILD_USER_ID);

			assertThat(dto.getTotalIncome()).isEqualTo(1500);
			assertThat(dto.getTotalExpense()).isEqualTo(1100);
			assertThat(dto.getSaving()).isEqualTo(400);
			assertThat(dto.getLavels()).containsExactly("2026/01", "2026/02");
			assertThat(dto.getMonthlyBalance()).containsExactly(700, -300);
			assertThat(dto.getCumulativeBalance()).containsExactly(700, 400);
		}

		@Test
		@DisplayName("登録日時がnullのレコードは月次集計・合計の両方から除外される")
		void getGraphData_ignoresNullRegisteredDate() {
			IncomeExpenseDto noDate = record(0, 999, null);
			List<IncomeExpenseDto> records = List.of(noDate);

			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(records);

			SavingDto dto = graphService.getGraphData(CHILD_USER_ID);

			assertThat(dto.getLavels()).isEmpty();
			// getGraphDataの合計は月次集計の積み上げのため、日時nullのレコードは合計にも反映されない
			assertThat(dto.getTotalIncome()).isZero();
		}

		@Test
		@DisplayName("amountがnullのレコードは0として扱われる")
		void getGraphData_nullAmountTreatedAsZero() {
			IncomeExpenseDto nullAmount = record(0, 0, LocalDateTime.of(2026, 3, 1, 0, 0));
			nullAmount.setAmount(null);

			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of(nullAmount));

			SavingDto dto = graphService.getGraphData(CHILD_USER_ID);

			assertThat(dto.getTotalIncome()).isZero();
			assertThat(dto.getMonthlyBalance()).containsExactly(0);
		}
	}

	@Nested
	@DisplayName("calcSavings")
	class CalcSavings {

		@Test
		@DisplayName("収入合計から支出合計を引いた値を返す")
		void calcSavings_success() {
			List<IncomeExpenseDto> records = List.of(
					record(0, 2000, LocalDateTime.now()),
					record(1, 500, LocalDateTime.now()));

			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(records);

			assertThat(graphService.calcSavings(CHILD_USER_ID)).isEqualTo(1500);
		}
	}

	@Nested
	@DisplayName("getCumulativeIncome / getCumulativeExpense")
	class Cumulative {

		@Test
		@DisplayName("収入・支出それぞれの全期間合計を返す")
		void cumulativeIncomeAndExpense() {
			List<IncomeExpenseDto> records = List.of(
					record(0, 1000, LocalDateTime.now()),
					record(0, 500, LocalDateTime.now()),
					record(1, 300, LocalDateTime.now()));

			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(records);

			assertThat(graphService.getCumulativeIncome(CHILD_USER_ID)).isEqualTo(1500);
			assertThat(graphService.getCumulativeExpense(CHILD_USER_ID)).isEqualTo(300);
		}
	}
}

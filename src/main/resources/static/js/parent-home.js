// 収入タブのモーダル
function openIncomeAddModal(btn) {
	document.getElementById('incomeChildUserId').value = btn.dataset.id;
	document.getElementById('incomeTargetName').textContent = btn.dataset.name;
	document.getElementById('incomeTargetMoney').textContent = btn.dataset.money;
	document.getElementById('incomeAddModal').classList.add('is-show');
}

function openIncomeEditModal(btn) {
	const id = btn.dataset.id;
	document.getElementById('editIncomeId').value = id;
	document.getElementById('editIncomeChildUserId').value = btn.dataset.childId;
	document.getElementById('editIncomeAmount').value = btn.dataset.amount;
	document.getElementById('editIncomeMemo').value = btn.dataset.memo;
	document.getElementById('incomeEditForm').action = `/parent/income/${id}/edit`;
	document.getElementById('incomeEditModal').classList.add('is-show');
}

function openIncomeDeleteModal(btn) {
	const id = btn.dataset.id;
	document.getElementById('deleteIncomeId').value = id;
	document.getElementById('deleteIncomeMemo').textContent = btn.dataset.memo;
	document.getElementById('deleteIncomeAmount').textContent = btn.dataset.amount;
	document.getElementById('incomeDeleteForm').action = `/parent/income/${id}/delete`;
	document.getElementById('incomeDeleteModal').classList.add('is-show');
}

function closeIncomeModal(modalId) {
	const modal = document.getElementById(modalId);
	modal.classList.remove('is-show');
	resetModal(modal);
}

document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('.income-modal-overlay').forEach(modal => {
		modal.addEventListener('click', event => {
			if (event.target === modal) {
				closeIncomeModal(modal.id);
			}
		});
	});
});

// テンプレタブのモーダル
function openTemplateAddModal() {
	document.getElementById('templateAddModal').classList.add('is-show');
}

function openTemplateEditModal(button) {
	document.getElementById('editTemplateId').value = button.dataset.id;
	document.getElementById('editTemplateTitle').value = button.dataset.title;
	document.getElementById('editTemplateReward').value = button.dataset.reward;
	document.getElementById('editTemplateDescription').value =
		button.dataset.description || '';

	document.getElementById('editTemplateForm').action =
		'/parent/templates/' + button.dataset.id + '/edit';

	document.getElementById('templateEditModal').classList.add('is-show');
}

function openTemplateDeleteModal(button) {
	document.getElementById('deleteTemplateId').value = button.dataset.id;
	document.getElementById('deleteTemplateTitle').textContent =
		button.dataset.title;

	document.getElementById('deleteTemplateForm').action =
		'/parent/templates/' + button.dataset.id + '/delete';

	document.getElementById('templateDeleteModal').classList.add('is-show');
}

function openTemplateQuestModal(button) {
	document.getElementById('questTemplateId').value = button.dataset.id;

	document.getElementById('questTemplateTitle').textContent = button.dataset.title;
	document.getElementById('questTemplateReward').textContent = button.dataset.reward;
	document.getElementById('questTemplateExp').textContent = button.dataset.exp;

	document.getElementById('questTemplateTitleInput').value = button.dataset.title;
	document.getElementById('questTemplateRewardInput').value = button.dataset.reward;
	document.getElementById('questTemplateExpInput').value = button.dataset.exp;
	document.getElementById('questTemplateDescriptionInput').value =
		button.dataset.description || '';

	document.getElementById('templateQuestModal').classList.add('is-show');
}

function closeTemplateModal(modalId) {
	const modal = document.getElementById(modalId);
	modal.classList.remove('is-show');
	resetModal(modal);
}

document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('.template-modal-overlay').forEach(modal => {
		modal.addEventListener('click', event => {
			if (event.target === modal) {
				closeTemplateModal(modal.id);
			}
		});
	});
});

// クエストタブのとこ
function openQuestAddModal() {
	document.getElementById('questAddModal').classList.add('is-show');
}

function openQuestEditModal(button) {
	const id = button.dataset.id;
	document.getElementById('editQuestId').value = id;
	document.getElementById('editQuestChildUserId').value = button.dataset.childId || '';
	document.getElementById('editQuestTitle').value = button.dataset.title || '';
	document.getElementById('editQuestReward').value = button.dataset.reward || 0;
	document.getElementById('editQuestExp').value = button.dataset.exp || 5;
	document.getElementById('editQuestDescription').value = button.dataset.description || '';

	const selectedDays = (button.dataset.availableDays || '').split(',').filter(Boolean);
	document.querySelectorAll('#editQuestDayList input[name="availableDays"]').forEach(checkbox => {
		checkbox.checked = selectedDays.includes(checkbox.dataset.day);
	});

	document.getElementById('editQuestSpecificDate').value = button.dataset.specificDate || '';

	document.getElementById('editQuestForm').action = '/parent/quest/' + id + '/edit';
	document.getElementById('questEditModal').classList.add('is-show');
}

function openQuestDeleteModal(button) {
	const id = button.dataset.id;
	document.getElementById('deleteQuestId').value = id;
	document.getElementById('deleteQuestTitle').textContent = button.dataset.title || '';
	document.getElementById('deleteQuestForm').action = '/parent/quest/' + id + '/delete';
	document.getElementById('questDeleteModal').classList.add('is-show');
}

function closeQuestModal(modalId) {
	const modal = document.getElementById(modalId);
	modal.classList.remove('is-show');
	resetModal(modal);
}

window.addEventListener('click', function(e) {
	const addModal = document.getElementById('questAddModal');
	const editModal = document.getElementById('questEditModal');
	const deleteModal = document.getElementById('questDeleteModal');

	if (e.target === addModal) closeQuestModal('questAddModal');
	if (e.target === editModal) closeQuestModal('questEditModal');
	if (e.target === deleteModal) closeQuestModal('questDeleteModal');
});

// ログアウトモーダル
function openParentLogoutModal() {
	document.getElementById('parentLogoutModal').classList.add('is-show');
}

// 収支タブ
let parentSavingsChartInstance = null;
let parentMonthlyChartInstance = null;
let parentCumulativeChartInstance = null;

function selectChild(btn) {
	document.querySelectorAll('.parent-child-btn').forEach(b => b.classList.remove('is-active'));
	btn.classList.add('is-active');

	fetch('/parent/graph/data?childUserId=' + btn.dataset.id)
		.then(res => res.json())
		.then(data => renderParentGraph(data));
}

function renderParentGraph(data) {
	const shortLabels = (data.labels || []).map(l => {
		const parts = l.split('/');
		return parts.length === 2 ? parseInt(parts[1]) + '月' : l;
	});

	if (parentSavingsChartInstance) parentSavingsChartInstance.destroy();
	if (parentMonthlyChartInstance) parentMonthlyChartInstance.destroy();
	if (parentCumulativeChartInstance) parentCumulativeChartInstance.destroy();

	// 貯金額の推移
	const lastIdx = data.cumulativeBalance.length - 1;
	const barData = data.cumulativeBalance.map((v, i) => i === lastIdx ? v : null);

	parentSavingsChartInstance = new Chart(
		document.getElementById('parentSavingsChart'), {
		type: 'bar',
		data: {
			labels: shortLabels,
			datasets: [
				{
					type: 'line', label: '月末ざんだか',
					data: data.cumulativeBalance,
					borderColor: '#7c3aed', backgroundColor: 'transparent',
					tension: 0.3, pointRadius: 5,
					pointBackgroundColor: '#fff', pointBorderColor: '#7c3aed',
					pointBorderWidth: 2, order: 0
				},
				{
					type: 'bar', label: '今月',
					data: barData,
					backgroundColor: 'rgba(124,58,237,0.2)',
					borderColor: '#7c3aed', borderWidth: 1,
					borderRadius: 4, barPercentage: 0.5, order: 1
				}
			]
		},
		options: {
			responsive: true,
			plugins: { legend: { display: false } },
			scales: { y: { display: false }, x: { grid: { display: false } } }
		}
	});

	// 月次収支
	parentMonthlyChartInstance = new Chart(
		document.getElementById('parentMonthlyChart'), {
		type: 'bar',
		data: {
			labels: shortLabels,
			datasets: [
				{
					type: 'bar', label: '収入', data: data.monthlyIncome,
					backgroundColor: 'rgba(34,197,94,0.75)',
					borderRadius: 4, barPercentage: 0.4, categoryPercentage: 0.6, order: 2
				},
				{
					type: 'bar', label: '支出', data: data.monthlyExpense,
					backgroundColor: 'rgba(239,68,68,0.75)',
					borderRadius: 4, barPercentage: 0.4, categoryPercentage: 0.6, order: 2
				},
				{
					type: 'line', label: '差引', data: data.monthlyBalance,
					borderColor: '#7c3aed', backgroundColor: 'transparent',
					tension: 0.3, pointRadius: 5,
					pointBackgroundColor: '#fff', pointBorderColor: '#7c3aed',
					pointBorderWidth: 2, order: 0
				}
			]
		},
		options: {
			responsive: true,
			plugins: {
				legend: {
					position: 'bottom',
					labels: { usePointStyle: true, pointStyle: 'circle', padding: 14, font: { size: 11 } }
				}
			},
			scales: { y: { display: false }, x: { grid: { display: false } } }
		}
	});

	// 累計収支
	const saving = data.totalIncome - data.totalExpense;
	parentCumulativeChartInstance = new Chart(
		document.getElementById('parentCumulativeChart'), {
		type: 'doughnut',
		data: {
			labels: ['収入', '支出'],
			datasets: [{
				data: [data.totalIncome, data.totalExpense],
				backgroundColor: ['rgba(34,197,94,0.8)', 'rgba(239,68,68,0.8)'],
				borderWidth: 0, cutout: '68%'
			}]
		},
		options: {
			responsive: true, maintainAspectRatio: true,
			plugins: { legend: { display: false } }
		},
		plugins: [{
			id: 'centerText',
			afterDraw(chart) {
				const ctx = chart.ctx;
				const cx = chart.width / 2;
				const cy = chart.height / 2;
				ctx.save();
				ctx.textAlign = 'center';
				ctx.fillStyle = '#6d28d9';
				ctx.font = '700 11px sans-serif';
				ctx.fillText('さしひき', cx, cy - 8);
				ctx.fillStyle = '#4c1d95';
				ctx.font = '900 18px sans-serif';
				ctx.fillText('¥' + saving.toLocaleString(), cx, cy + 14);
				ctx.restore();
			}
		}]
	});

	// 累計数値
	document.getElementById('parentTotalIncome').textContent =
		'¥' + data.totalIncome.toLocaleString();
	document.getElementById('parentTotalExpense').textContent =
		'¥' + data.totalExpense.toLocaleString();
	document.getElementById('parentSaving').textContent =
		'¥' + saving.toLocaleString();
}

// ページ読み込み時に選択済み子供のグラフを描画
document.addEventListener('DOMContentLoaded', () => {
	if (window.parentGraphChildId) {
		fetch('/parent/graph/data?childUserId=' + window.parentGraphChildId)
			.then(res => res.json())
			.then(data => renderParentGraph(data));
	}
});

// 家族タブ
function openFamilyAddChildModal() {
	const modal = document.getElementById('familyAddChildModal');
	if (modal) {
		modal.classList.add('is-show');
	}
}

function openFamilyEditModal(button) {
	const userId = button.dataset.id;
	const userName = button.dataset.name;
	const mailAddress = button.dataset.mail;
	const authority = button.dataset.authority;

	const modal = document.getElementById('familyEditModal');
	const form = document.getElementById('familyEditForm');
	const title = document.getElementById('familyEditTitle');
	const nameInput = document.getElementById('familyEditName');
	const mailInput = document.getElementById('familyEditMail');
	const passwordInput = document.getElementById('familyEditPassword');
	const passwordConfirmInput = document.getElementById('familyEditPasswordConfirm');

	if (!modal || !form) return;

	if (authority === '1') {
		title.textContent = '🖍 保護者アカウントを編集';
		form.action = '/parent/family/edit';
	} else {
		title.textContent = '🖍 子供アカウントを編集';
		form.action = `/parent/family/child/${userId}/edit`;
	}

	nameInput.value = userName;
	mailInput.value = mailAddress;
	passwordInput.value = '';
	passwordConfirmInput.value = '';

	modal.classList.add('is-show');
}

function openFamilyDeleteModal(button) {
	const userId = button.dataset.id;
	const userName = button.dataset.name;
	const authority = button.dataset.authority;

	const modal = document.getElementById('familyDeleteModal');
	const form = document.getElementById('familyDeleteForm');
	const title = document.getElementById('familyDeleteTitle');
	const message = document.getElementById('familyDeleteMessage');

	if (!modal || !form) return;

	form.action = `/parent/family/${userId}/delete`;

	if (authority === '1') {
		title.textContent = 'あなたのアカウントを削除しますか？';
		message.innerHTML =
			`「${userName}」を削除します。<br>` +
			'※紐づく子供アカウントとすべての記録も削除されます。<br>' +
			'この操作は元に戻せません。';
	} else {
		title.textContent = '子供アカウントを削除しますか？';
		message.innerHTML =
			`「${userName}」を削除します。<br>` +
			'この操作は元に戻せません。';
	}

	modal.classList.add('is-show');
}

function closeFamilyModal(modalId) {
	const modal = document.getElementById(modalId);
	if (modal) {
		modal.classList.remove('is-show');
		resetModal(modal);
	}
}

document.addEventListener('click', function(event) {
	if (event.target.classList.contains('family-modal-overlay')) {
		closeFamilyModal(event.target.id);
	}
});

// モーダルのリセット共通処理
function resetModal(modal) {
	modal.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"])').forEach(input => {
		input.value = '';
		input.setCustomValidity('');
	});
	modal.querySelectorAll('p[style*="color:#e53935"]').forEach(el => {
		el.style.display = 'none';
	});
}

// 全角数字→半角変換 + 数字以外はエラー表示 + 上限チェック
document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('input[inputmode="numeric"]').forEach(input => {
		let composing = false;

		input.addEventListener('compositionstart', () => { composing = true; });

		input.addEventListener('compositionend', () => {
			composing = false;
			validate(input);
		});

		input.addEventListener('input', () => {
			if (composing) return;
			validate(input);
		});

		function validate(input) {
			// 全角→半角変換
			input.value = input.value
				.replace(/[０-９]/g, s => String.fromCharCode(s.charCodeAt(0) - 0xFEE0));

			// 数字以外が含まれているか確認
			if (/[^0-9]/.test(input.value)) {
				input.setCustomValidity('数字を入力してください');
				input.reportValidity();
				return;
			}

			const val = parseInt(input.value, 10);

			// 最小値チェック
			if (!isNaN(val) && val < 1) {
				input.setCustomValidity('¥1以上の金額を入力してください');
				input.reportValidity();
				return;
			}

			// 上限チェック
			if (!isNaN(val) && val > 100000) {
				input.setCustomValidity('¥100,000以下の金額を入力してください');
				input.reportValidity();
				return;
			}

			input.setCustomValidity('');
			input.value = input.value.replace(/[^0-9]/g, '');
		}
	});
});

// 文字数カウント表示
document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('input[maxlength]').forEach(input => {
		if (input.getAttribute('inputmode') === 'numeric') return;
		if (input.type === 'password') return;
		if (input.type === 'email') return;
		if (input.type === 'hidden') return;
		if (input.type === 'radio') return;

		const max = parseInt(input.getAttribute('maxlength'));

		const countEl = document.createElement('p');
		countEl.style.cssText = 'color:#aaa;font-size:11px;margin:2px 0 0;text-align:right;display:none;';
		countEl.textContent = `0 / ${max}`;
		input.insertAdjacentElement('afterend', countEl);

		input.addEventListener('focus', () => {
			countEl.style.display = 'block';
		});

		input.addEventListener('blur', () => {
			countEl.style.display = 'none';
		});

		input.addEventListener('input', () => {
			const len = input.value.length;
			countEl.textContent = `${len} / ${max}`;
			countEl.style.color = len === max ? '#e53935' : '#aaa';
		});
	});
});

// メールアドレスの文字数カウント＋フォーマットチェック
document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('input[type="email"]').forEach(input => {
		// 文字数カウント（maxlength がある場合のみ）
		if (input.getAttribute('maxlength')) {
			const max = parseInt(input.getAttribute('maxlength'));
			const countEl = document.createElement('p');
			countEl.style.cssText = 'color:#aaa;font-size:11px;margin:2px 0 0;text-align:right;display:none;';
			countEl.textContent = `0 / ${max}`;
			input.insertAdjacentElement('afterend', countEl);

			input.addEventListener('focus', () => countEl.style.display = 'block');
			input.addEventListener('blur', () => countEl.style.display = 'none');
			input.addEventListener('input', () => {
				const len = input.value.length;
				countEl.textContent = `${len} / ${max}`;
				countEl.style.color = len >= max ? '#e53935' : '#aaa';
			});

			// フォーマットエラーはカウンターの下に挿入
			const errorEl = document.createElement('p');
			errorEl.style.cssText = 'color:#e53935;font-size:12px;margin:4px 0 0;display:none;';
			errorEl.textContent = '正しいメールアドレスの形式で入力してください。';
			countEl.insertAdjacentElement('afterend', errorEl);

			input.addEventListener('input', () => {
				const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
				if (input.value && !emailRegex.test(input.value)) {
					errorEl.style.display = 'block';
					input.setCustomValidity('正しいメールアドレスの形式で入力してください。');
				} else {
					errorEl.style.display = 'none';
					input.setCustomValidity('');
				}
			});
		}
	});
});

// パスワード形式チェック（半角英数字4〜20文字）
document.addEventListener('DOMContentLoaded', () => {
	['familyEditPassword', 'familyAddPassword'].forEach(id => {
		const input = document.getElementById(id)
			|| document.querySelector(`#familyAddChildModal input[name="password"]`)
			|| document.querySelector(`#familyEditModal input[name="password"]`);
		if (!input) return;

		const errorEl = document.createElement('p');
		errorEl.style.cssText = 'color:#e53935;font-size:12px;margin:4px 0 0;display:none;';
		errorEl.textContent = 'パスワードは半角英数字4文字以上20文字以内で入力してください。';
		input.insertAdjacentElement('afterend', errorEl);

		input.addEventListener('input', () => {
			if (input.value.length < 4) {
				errorEl.style.display = 'none';
				input.setCustomValidity('');
				return;
			}
			const pwRegex = /^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{4,20}$/;
			if (!pwRegex.test(input.value)) {
				errorEl.textContent = 'パスワードは半角英数字を組み合わせて4文字以上20文字以内で入力してください。';
				errorEl.style.display = 'block';
				input.setCustomValidity('パスワードは半角英数字を組み合わせて4文字以上20文字以内で入力してください。');
			} else {
				errorEl.style.display = 'none';
				input.setCustomValidity('');
			}
		});
	});
});

// パスワード確認一致チェック
document.addEventListener('DOMContentLoaded', () => {
	[
		{
			modalId: 'familyAddChildModal',
			pwSelector: 'input[name="password"]',
			confirmSelector: 'input[name="passwordConfirm"]'
		},
		{
			modalId: 'familyEditModal',
			pwSelector: 'input[name="password"]',
			confirmSelector: 'input[name="passwordConfirm"]'
		}
	].forEach(({ modalId, pwSelector, confirmSelector }) => {
		const modal = document.getElementById(modalId);
		if (!modal) return;

		const pwInput = modal.querySelector(pwSelector);
		const confirmInput = modal.querySelector(confirmSelector);
		if (!pwInput || !confirmInput) return;

		const errorEl = document.createElement('p');
		errorEl.style.cssText = 'color:#e53935;font-size:12px;margin:4px 0 0;display:none;';
		errorEl.textContent = 'パスワードが一致しません。';
		confirmInput.insertAdjacentElement('afterend', errorEl);

		const check = () => {
			if (confirmInput.value && pwInput.value !== confirmInput.value) {
				errorEl.style.display = 'block';
				confirmInput.setCustomValidity('パスワードが一致しません。');
			} else {
				errorEl.style.display = 'none';
				confirmInput.setCustomValidity('');
			}
		};
		pwInput.addEventListener('input', check);
		confirmInput.addEventListener('input', check);
	});
});
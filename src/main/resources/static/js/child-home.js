function openCharacterNameModal() {
	document.getElementById('characterNameModal').classList.add('is-show');
}

function openCharacterTypeModal() {
	document.getElementById('characterTypeModal').classList.add('is-show');
}

function openRecordAddModal() {
	document.getElementById('recordAddModal').classList.add('is-show');
}

function openLimitRequestModal() {
	document.getElementById('limitRequestModal').classList.add('is-show');
}

function closeChildModal(modalId) {
	const modal = document.getElementById(modalId);
	modal.classList.remove('is-show');

	// キャラクター名モーダルはリセットしない（元の名前を保持）
	if (modalId === 'characterNameModal') return;

	modal.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"])').forEach(input => {
		input.value = '';
		input.setCustomValidity('');
	});
	modal.querySelectorAll('p').forEach(el => {
		if (el.style.color === 'rgb(229, 57, 53)' || el.style.color === '#e53935') {
			el.style.display = 'none';
		}
	});
}

document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('.child-modal-overlay').forEach(modal => {
		modal.addEventListener('click', event => {
			if (event.target === modal) {
				closeChildModal(modal.id);
			}
		});
	});
});

function openRecordEditModal(btn) {
	const id = btn.dataset.id;
	const category = btn.dataset.category || '';
	const amount = btn.dataset.amount;
	const memo = btn.dataset.memo || '';

	document.getElementById('editIncomeExpenseId').value = id;
	document.getElementById('editCategory').value = category;
	document.getElementById('editAmount').value = amount;
	document.getElementById('editMemo').value = memo;
	document.getElementById('recordEditForm').action = '/child/records/' + id + '/edit';

	// 値がセットされたのでカスタムバリデーションをリセット
	const amountInput = document.getElementById('editAmount');
	const categoryInput = document.getElementById('editCategory');
	if (amountInput && amountInput.value !== '') amountInput.setCustomValidity('');
	if (categoryInput && categoryInput.value.trim() !== '') categoryInput.setCustomValidity('');

	document.getElementById('recordEditModal').classList.add('is-show');
}

function openRecordDeleteModal(btn) {
	const id = btn.dataset.id;
	const category = btn.dataset.category || '';
	const amount = btn.dataset.amount;

	document.getElementById('deleteCategory').textContent = category;
	document.getElementById('deleteAmount').textContent = Number(amount).toLocaleString();
	document.getElementById('recordDeleteForm').action = '/child/records/' + id + '/delete';
	document.getElementById('recordDeleteModal').classList.add('is-show');
}

function openLogoutModal() {
	document.getElementById('logoutModal').classList.add('is-show');
}

// 全角数字→半角変換 + 空欄・数字以外・上限チェック
document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('input[inputmode="numeric"]').forEach(input => {
		let composing = false;

		// 編集モーダルと上限モーダルは除外（開くときに値がセットされる）
		const isExcluded = input.id === 'editAmount' || input.name === 'limitAmount';
		if (!isExcluded && input.value === '') {
			input.setCustomValidity('いくら使ったか入力してください。');
		}

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

			// 空欄チェック
			if (input.value === '') {
				if (input.name === 'limitAmount') {
					input.setCustomValidity('上限金額を入力してください。');
				} else {
					input.setCustomValidity('いくら使ったか入力してください。');
				}
				return;
			}

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
			// maxlengthを超えた場合は切り捨て（全角変換後の超過対策）
			if (input.value.length > max) {
				input.value = input.value.slice(0, max);
			}
			const len = input.value.length;
			countEl.textContent = `${len} / ${max}`;
			countEl.style.color = len === max ? '#e53935' : '#aaa';
		});
	});
});

// メールアドレスのフォーマットチェック（日本語エラー）
document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('input[type="email"]').forEach(input => {
		const errorEl = document.createElement('p');
		errorEl.style.cssText = 'color:#e53935;font-size:12px;margin:4px 0 0;display:none;';
		errorEl.textContent = '正しいメールアドレスの形式で入力してください。';
		input.insertAdjacentElement('afterend', errorEl);

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
	});
});

// テキスト入力の空白のみ禁止・必須チェック
document.addEventListener('DOMContentLoaded', () => {
	// カテゴリ（なにをかったの？）
	[
		document.querySelector('#recordAddModal input[name="category"]'),
		document.querySelector('#recordEditModal input[name="category"]')
	].forEach(input => {
		if (!input) return;

		// 初期状態でカスタムメッセージをセット
		if (input.value === '') {
			input.setCustomValidity('なにをかったのか入力してください。');
		}

		const errorEl = document.createElement('p');
		errorEl.style.cssText = 'color:#e53935;font-size:12px;margin:4px 0 0;display:none;';
		errorEl.textContent = 'なにをかったのか入力してください。';
		input.insertAdjacentElement('afterend', errorEl);

		input.addEventListener('input', () => {
			if (input.value.trim() === '') {
				errorEl.style.display = 'block';
				input.setCustomValidity('なにをかったのか入力してください。');
			} else {
				errorEl.style.display = 'none';
				input.setCustomValidity('');
			}
		});
	});

	// キャラクター名
	const characterNameInput = document.querySelector('#characterNameModal input[name="characterName"]');
	if (characterNameInput) {
		const errorEl = document.createElement('p');
		errorEl.style.cssText = 'color:#e53935;font-size:12px;margin:4px 0 0;display:none;';
		errorEl.textContent = 'キャラクター名を入力してください。';
		characterNameInput.insertAdjacentElement('afterend', errorEl);

		characterNameInput.addEventListener('input', () => {
			if (characterNameInput.value.trim() === '') {
				errorEl.style.display = 'block';
				characterNameInput.setCustomValidity('キャラクター名を入力してください。');
			} else {
				errorEl.style.display = 'none';
				characterNameInput.setCustomValidity('');
			}
		});
	}
});
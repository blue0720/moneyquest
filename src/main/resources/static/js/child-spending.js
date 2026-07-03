/**
 子供の支出上限変更モーダルです
 */
function openLimitModal() {
	const modal = document.getElementById('limitModal');
	modal.classList.add('is-show');

	// 開くときに金額フィールドにカスタムメッセージをセット
	const limitInput = modal.querySelector('input[name="limitAmount"]');
	if (limitInput && limitInput.value === '') {
		limitInput.setCustomValidity('上限金額を入力してください。');
	}
}

function closeLimitModal() {
	document
		.getElementById('limitModal')
		.classList.remove('is-show');
}

document.addEventListener('DOMContentLoaded', () => {

	const limitModal =
		document.getElementById('limitModal');

	if (limitModal) {

		limitModal.addEventListener('click', (e) => {

			if (e.target === limitModal) {
				closeLimitModal();
			}

		});

	}

});
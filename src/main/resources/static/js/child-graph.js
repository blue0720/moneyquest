document.addEventListener("DOMContentLoaded", function () {

    var labelsEl = document.getElementById('graphLabels');
    if (!labelsEl) return;

    var labels = JSON.parse(labelsEl.textContent);
    var monthlyIncome = JSON.parse(document.getElementById('graphMonthlyIncome').textContent);
    var monthlyExpense = JSON.parse(document.getElementById('graphMonthlyExpense').textContent);
    var cumulativeBalance = JSON.parse(document.getElementById('graphCumulativeBalance').textContent);
    var monthlyBalance = JSON.parse(document.getElementById('graphMonthlyBalance').textContent);
    var totalIncome = JSON.parse(document.getElementById('graphTotalIncome').textContent);
    var totalExpense = JSON.parse(document.getElementById('graphTotalExpense').textContent);

    var shortLabels = labels.map(function (l) {
        var parts = l.split('/');
        return parts.length === 2 ? parseInt(parts[1]) + '月' : l;
    });

    // ===== 貯金額の推移（折れ線 + 直近月だけ棒） =====
    var lastIdx = cumulativeBalance.length - 1;
    var barData = cumulativeBalance.map(function (v, i) {
        return i === lastIdx ? v : null;
    });

    new Chart(document.getElementById('savingsChart'), {
        type: 'bar',
        data: {
            labels: shortLabels,
            datasets: [
                {
                    type: 'line',
                    label: '月末ざんだか',
                    data: cumulativeBalance,
                    borderColor: '#f97316',
                    backgroundColor: 'transparent',
                    tension: 0.3,
                    pointRadius: 5,
                    pointBackgroundColor: '#fff',
                    pointBorderColor: '#f97316',
                    pointBorderWidth: 2,
                    order: 0
                },
                {
                    type: 'bar',
                    label: '今月',
                    data: barData,
                    backgroundColor: 'rgba(249,115,22,0.25)',
                    borderColor: '#f97316',
                    borderWidth: 1,
                    borderRadius: 4,
                    barPercentage: 0.5,
                    order: 1
                }
            ]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { display: false },
                x: { grid: { display: false } }
            }
        }
    });

    // ===== 月次収支（収入・支出の棒 + 差引の折れ線） =====
    new Chart(document.getElementById('monthlyChart'), {
        type: 'bar',
        data: {
            labels: shortLabels,
            datasets: [
                {
                    type: 'bar',
                    label: '収入',
                    data: monthlyIncome,
                    backgroundColor: 'rgba(34,197,94,0.75)',
                    borderRadius: 4,
                    barPercentage: 0.4,
                    categoryPercentage: 0.6,
                    order: 2
                },
                {
                    type: 'bar',
                    label: '支出',
                    data: monthlyExpense,
                    backgroundColor: 'rgba(239,68,68,0.75)',
                    borderRadius: 4,
                    barPercentage: 0.4,
                    categoryPercentage: 0.6,
                    order: 2
                },
                {
                    type: 'line',
                    label: '差引（収支）',
                    data: monthlyBalance,
                    borderColor: '#f97316',
                    backgroundColor: 'transparent',
                    tension: 0.3,
                    pointRadius: 5,
                    pointBackgroundColor: '#fff',
                    pointBorderColor: '#f97316',
                    pointBorderWidth: 2,
                    order: 0
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'circle',
                        padding: 14,
                        font: { size: 11, weight: '700' }
                    }
                }
            },
            scales: {
                y: { display: false },
                x: {
                    grid: { display: false },
                    ticks: {
                        callback: function (val, idx) {
                            var sign = monthlyBalance[idx] >= 0 ? '+' : '';
                            return [shortLabels[idx], sign + '¥' + monthlyBalance[idx].toLocaleString()];
                        },
                        font: { size: 10 }
                    }
                }
            }
        }
    });

	// ===== 累計収支（ドーナツ） =====
	    var saving = totalIncome - totalExpense;
	    new Chart(document.getElementById('cumulativeChart'), {
	        type: 'doughnut',
	        data: {
	            labels: ['収入', '支出'],
	            datasets: [{
	                data: [totalIncome, totalExpense],
	                backgroundColor: ['rgba(34,197,94,0.8)', 'rgba(239,68,68,0.8)'],
	                borderWidth: 0,
	                cutout: '68%'
	            }]
	        },
	        options: {
	            responsive: true,
	            maintainAspectRatio: true,
	            plugins: {
	                legend: { display: false },
	                tooltip: {
	                    callbacks: {
	                        label: function (ctx) { return ctx.label + ': ¥' + ctx.parsed.toLocaleString(); }
	                    }
	                }
	            }
	        },
	        plugins: [{
	            id: 'centerText',
	            afterDraw: function (chart) {
	                var ctx = chart.ctx;
	                var cx = chart.width / 2;
	                var cy = chart.height / 2;
	                ctx.save();
	                ctx.textAlign = 'center';
	                ctx.fillStyle = '#a0724a';
	                ctx.font = '700 11px "Yu Gothic", sans-serif';
	                ctx.fillText('さしひき', cx, cy - 8);
	                ctx.fillStyle = '#7a4b24';
	                ctx.font = '900 18px "Yu Gothic", sans-serif';
	                ctx.fillText('¥' + saving.toLocaleString(), cx, cy + 14);
	                ctx.restore();
	            }
	        }]
	    });

	});
(function initPriceHistoryChart() {
    const DATASET_STYLE = [
        {
            label: '买入价',
            borderColor: '#10B981',
            backgroundColor: 'rgba(16, 185, 129, 0.10)',
            tension: 0,
            fill: true,
            pointRadius: 0,
            pointHoverRadius: 4,
            spanGaps: true
        },
        {
            label: '卖出价',
            borderColor: '#EF4444',
            backgroundColor: 'rgba(239, 68, 68, 0.10)',
            tension: 0,
            fill: true,
            pointRadius: 0,
            pointHoverRadius: 4,
            spanGaps: true
        }
    ];

    function pad(n) {
        return String(n).padStart(2, '0');
    }

    function parseTimestamp(ts) {
        const raw = String(ts ?? '').trim();
        if (!raw) return NaN;
        const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T');
        const withTz = /[zZ]|[+-]\d{2}:?\d{2}$/.test(normalized) ? normalized : `${normalized}+08:00`;
        const ms = Date.parse(withTz);
        return Number.isFinite(ms) ? ms : NaN;
    }

    function getParts(ms) {
        const d = new Date(ms);
        return {
            year: d.getFullYear(),
            month: d.getMonth() + 1,
            day: d.getDate(),
            hour: d.getHours(),
            minute: d.getMinutes(),
            second: d.getSeconds()
        };
    }

    function dayLabel(p) {
        return `${p.month}月${p.day}日`;
    }

    function dayKey(p) {
        return `${p.year}-${p.month}-${p.day}`;
    }

    function hourKey(p) {
        return `${dayKey(p)}-${p.hour}`;
    }

    function minuteKey(p) {
        return `${hourKey(p)}-${p.minute}`;
    }

    function hasConsecutiveDuplicate(labels) {
        for (let i = 1; i < labels.length; i += 1) {
            if (labels[i] === labels[i - 1]) return true;
        }
        return false;
    }

    function isFirstOfGroup(index, allParts, keyFn) {
        if (index === 0) return true;
        return keyFn(allParts[index]) !== keyFn(allParts[index - 1]);
    }

    function formatTooltipTitle(ms) {
        if (!Number.isFinite(ms)) return '';
        const p = getParts(ms);
        return `${p.year}-${pad(p.month)}-${pad(p.day)} ${pad(p.hour)}:${pad(p.minute)}:${pad(p.second)}`;
    }

    function formatAdaptiveTicks(tickValues) {
        const parts = tickValues.map((ms) => getParts(ms));
        if (!parts.length) return () => '';

        const dayLabels = parts.map(dayLabel);
        const dayDup = hasConsecutiveDuplicate(dayLabels);
        const spanMs = tickValues[tickValues.length - 1] - tickValues[0];

        if (!dayDup) {
            return (idx) => {
                const p = parts[idx];
                if (spanMs > 400 * 24 * 60 * 60 * 1000) {
                    const label = `${p.year}年${p.month}月`;
                    return isFirstOfGroup(idx, parts, (pt) => `${pt.year}-${pt.month}`) ? label : '';
                }
                if (spanMs > 2 * 24 * 60 * 60 * 1000) {
                    return dayLabel(p);
                }
                return `${dayLabel(p)} ${pad(p.hour)}:${pad(p.minute)}`;
            };
        }

        const hourLabels = parts.map((p) => `${dayLabel(p)} ${pad(p.hour)}`);
        const hourDup = hasConsecutiveDuplicate(hourLabels);

        if (!hourDup) {
            return (idx) => {
                const p = parts[idx];
                if (isFirstOfGroup(idx, parts, dayKey)) return dayLabel(p);
                return `${pad(p.hour)}:00`;
            };
        }

        const minuteLabels = parts.map((p) => `${pad(p.hour)}:${pad(p.minute)}`);
        const minuteDup = hasConsecutiveDuplicate(minuteLabels);

        if (!minuteDup) {
            return (idx) => {
                const p = parts[idx];
                if (isFirstOfGroup(idx, parts, hourKey)) return `${pad(p.hour)}:00`;
                return `${pad(p.hour)}:${pad(p.minute)}`;
            };
        }

        return (idx) => {
            const p = parts[idx];
            if (isFirstOfGroup(idx, parts, minuteKey)) return `${pad(p.hour)}:${pad(p.minute)}`;
            return `${pad(p.second)}秒`;
        };
    }

    function createTickCallback() {
        return function adaptiveTickCallback(value, index, ticks) {
            const tickValues = ticks.map((tick) => Number(tick.value)).filter(Number.isFinite);
            const signature = `${tickValues.length}:${tickValues[0] || 0}:${tickValues[tickValues.length - 1] || 0}`;
            if (this.$adaptiveTickSignature !== signature) {
                this.$adaptiveTickSignature = signature;
                this.$adaptiveTickFormatter = formatAdaptiveTicks(tickValues);
            }
            return this.$adaptiveTickFormatter ? this.$adaptiveTickFormatter(index) : '';
        };
    }

    function resolveZoomPlugin() {
        if (typeof ChartZoom !== 'undefined') return ChartZoom;
        const pkg = window['chartjs-plugin-zoom'];
        if (!pkg) return null;
        return pkg.default || pkg;
    }

    function ensureReady() {
        if (window.__mcwwsPriceHistoryZoomReady || !window.Chart) return;
        const plugin = resolveZoomPlugin();
        if (!plugin) return;
        window.Chart.register(plugin);
        window.__mcwwsPriceHistoryZoomReady = true;
    }

    function buildChartOptions() {
        return {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'nearest', axis: 'x', intersect: false },
            plugins: {
                legend: { labels: { color: '#CBD5E1' } },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        title(items) {
                            const x = items?.[0]?.parsed?.x;
                            return formatTooltipTitle(x);
                        }
                    }
                },
                zoom: {
                    zoom: {
                        wheel: {
                            enabled: true,
                            speed: 0.08
                        },
                        mode: 'x',
                        onZoomComplete({ chart }) {
                            chart.update('none');
                        }
                    },
                    pan: {
                        enabled: true,
                        mode: 'x',
                        threshold: 6,
                        onPanComplete({ chart }) {
                            chart.update('none');
                        }
                    },
                    limits: {
                        x: { minRange: 60 * 1000 }
                    }
                }
            },
            scales: {
                x: {
                    type: 'linear',
                    ticks: {
                        color: '#94A3B8',
                        maxRotation: 0,
                        autoSkip: true,
                        callback: createTickCallback()
                    },
                    grid: { color: 'rgba(51, 65, 85, 0.5)' }
                },
                y: {
                    ticks: { color: '#94A3B8' },
                    grid: { color: 'rgba(51, 65, 85, 0.5)' }
                }
            }
        };
    }

    function buildChartData(priceHistory) {
        const points = priceHistory
            .map((row) => ({
                x: parseTimestamp(row.timestamp),
                buy: Number(row.avgBuyPrice),
                sell: Number(row.avgSellPrice)
            }))
            .filter((row) => Number.isFinite(row.x));

        return {
            datasets: [
                {
                    ...DATASET_STYLE[0],
                    data: points.map((row) => ({ x: row.x, y: row.buy }))
                },
                {
                    ...DATASET_STYLE[1],
                    data: points.map((row) => ({ x: row.x, y: row.sell }))
                }
            ]
        };
    }

    function destroyChart(chart) {
        if (chart) {
            chart.destroy();
        }
        return null;
    }

    function render({ chart, canvas, priceHistory, rangeKey }) {
        ensureReady();
        const data = buildChartData(priceHistory);
        const options = buildChartOptions();
        const rangeChanged = Boolean(chart && chart.$historyRangeKey && chart.$historyRangeKey !== rangeKey);
        const canvasChanged = Boolean(chart && chart.canvas !== canvas);

        if (!chart || canvasChanged) {
            destroyChart(chart);
            chart = new window.Chart(canvas, {
                type: 'line',
                data,
                options
            });
            chart.$historyRangeKey = rangeKey;
            return chart;
        }

        chart.data.datasets[0].data = data.datasets[0].data;
        chart.data.datasets[1].data = data.datasets[1].data;
        chart.update('none');
        if (rangeChanged && typeof chart.resetZoom === 'function') {
            chart.resetZoom();
        }
        chart.$historyRangeKey = rangeKey;
        return chart;
    }

    window.McPriceHistoryChart = {
        ensureReady,
        render,
        destroy: destroyChart
    };
})();

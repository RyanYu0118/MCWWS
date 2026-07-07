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

    const PRESET_RANGE_MS = {
        '10m': 10 * 60 * 1000,
        '30m': 30 * 60 * 1000,
        '1h': 60 * 60 * 1000,
        '6h': 6 * 60 * 60 * 1000,
        '24h': 24 * 60 * 60 * 1000,
        '7d': 7 * 24 * 60 * 60 * 1000,
        '1mo': 30 * 24 * 60 * 60 * 1000,
        '1y': 365 * 24 * 60 * 60 * 1000,
        '3y': 3 * 365 * 24 * 60 * 60 * 1000,
        all: null
    };

    function getPresetViewport(bounds, rangeKey) {
        if (!bounds) return null;
        const span = PRESET_RANGE_MS[rangeKey];
        if (span == null || rangeKey === 'all') {
            return { min: bounds.min, max: bounds.max };
        }
        return {
            min: Math.max(bounds.min, bounds.max - span),
            max: bounds.max
        };
    }

    function setXViewport(chart, viewport) {
        if (!chart || !viewport) return;
        if (typeof chart.zoomScale === 'function') {
            chart.zoomScale('x', viewport, 'none');
            return;
        }
        chart.options.scales.x.min = viewport.min;
        chart.options.scales.x.max = viewport.max;
        chart.update('none');
    }

    function applyPresetRange(chart, rangeKey, options = {}) {
        const { force = false } = options;
        if (!chart || (!force && chart.$historyViewportCustomized)) return;
        const viewport = getPresetViewport(chart.$historyBounds, rangeKey);
        if (!viewport) return;
        chart.$historyViewportCustomized = false;
        chart.$historyRangeKey = rangeKey;
        setXViewport(chart, viewport);
    }

    function getDataBounds(priceHistory) {
        const xs = priceHistory
            .map((row) => parseTimestamp(row.timestamp))
            .filter(Number.isFinite);
        if (!xs.length) return null;
        return {
            min: Math.min(...xs),
            max: Math.max(...xs)
        };
    }

    function clearPresetRangeHighlight(chartWrap) {
        if (!chartWrap) return;
        const tabs = chartWrap.previousElementSibling;
        const scope = tabs?.classList?.contains('item-history-range-tabs')
            ? tabs
            : chartWrap.closest('#modalBody, #itemDetailContent');
        scope?.querySelectorAll('[data-item-history-range]').forEach((btn) => {
            btn.classList.remove('active');
        });
    }

    function markManualViewportChange(chart, chartWrap) {
        if (!chart || chart.$historyViewportCustomized) return;
        chart.$historyViewportCustomized = true;
        clearPresetRangeHighlight(chartWrap);
    }

    function buildChartOptions(bounds, chartWrap) {
        const xLimits = bounds
            ? { min: bounds.min, max: bounds.max, minRange: 60 * 1000 }
            : { minRange: 60 * 1000 };

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
                            markManualViewportChange(chart, chartWrap);
                            chart.update('none');
                        }
                    },
                    pan: {
                        enabled: true,
                        mode: 'x',
                        scaleMode: 'x',
                        threshold: 4,
                        onPanComplete({ chart }) {
                            markManualViewportChange(chart, chartWrap);
                            chart.update('none');
                        }
                    },
                    limits: {
                        x: xLimits
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

    function attachInteractionHints(chartWrap) {
        if (!chartWrap || chartWrap.dataset.hintsReady === '1') return;
        chartWrap.dataset.hintsReady = '1';
        chartWrap.title = '滚轮缩放时间轴 · 按住鼠标左键左右拖拽平移';
        chartWrap.classList.add('item-modal-chart-wrap--interactive');
        if (!chartWrap.nextElementSibling?.classList.contains('item-modal-chart-hint')) {
            const hint = document.createElement('div');
            hint.className = 'item-modal-chart-hint';
            hint.textContent = '滚轮缩放 · 按住拖拽左右平移';
            chartWrap.insertAdjacentElement('afterend', hint);
        }
    }

    function render({ chart, canvas, priceHistory, rangeKey }) {
        ensureReady();
        const data = buildChartData(priceHistory);
        const bounds = getDataBounds(priceHistory);
        const rangeChanged = Boolean(chart && chart.$historyRangeKey && chart.$historyRangeKey !== rangeKey);
        const canvasChanged = Boolean(chart && chart.canvas !== canvas);
        const chartWrap = canvas?.closest('[data-item-history-chart]') || canvas?.parentElement;
        const options = buildChartOptions(bounds, chartWrap);

        if (!chart || canvasChanged) {
            destroyChart(chart);
            chart = new window.Chart(canvas, {
                type: 'line',
                data,
                options
            });
            chart.$historyRangeKey = rangeKey;
            chart.$historyBounds = bounds;
            chart.$historyViewportCustomized = false;
            attachInteractionHints(chartWrap);
            applyPresetRange(chart, rangeKey, { force: true });
            return chart;
        }

        chart.$historyBounds = bounds;
        if (bounds && chart.options?.plugins?.zoom?.limits) {
            chart.options.plugins.zoom.limits.x = {
                min: bounds.min,
                max: bounds.max,
                minRange: 60 * 1000
            };
        }

        chart.data.datasets[0].data = data.datasets[0].data;
        chart.data.datasets[1].data = data.datasets[1].data;

        if (rangeChanged) {
            applyPresetRange(chart, rangeKey, { force: true });
        } else if (!chart.$historyViewportCustomized && chart.$historyRangeKey) {
            applyPresetRange(chart, chart.$historyRangeKey, { force: true });
        } else {
            chart.update('none');
        }
        chart.$historyRangeKey = rangeKey;
        attachInteractionHints(chartWrap);
        return chart;
    }

    window.McPriceHistoryChart = {
        ensureReady,
        render,
        destroy: destroyChart,
        applyPresetRange,
        getPresetViewport,
        isViewportCustomized(chart) {
            return Boolean(chart?.$historyViewportCustomized);
        }
    };
})();

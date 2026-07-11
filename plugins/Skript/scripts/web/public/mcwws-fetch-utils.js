/**
 * 统一解析 fetch 响应，避免把 HTML 404 页当 JSON 解析时报错。
 */
(function () {
    function isHtmlBody(text) {
        const head = String(text || '').trimStart().slice(0, 64).toLowerCase();
        return head.startsWith('<!doctype') || head.startsWith('<html');
    }

    async function readJsonResponse(response, urlHint) {
        const text = await response.text();
        const trimmed = text.trim();

        if (isHtmlBody(trimmed)) {
            const target = urlHint || response.url || 'API';
            throw new Error(
                `接口 ${target} 返回了网页而不是 JSON。`
                + ' 请确认：① 已运行 plugins/Skript/scripts/web 下的 node server.js；'
                + ' ② 通过 Node 服务地址访问（默认 http://服务器IP:8002）；'
                + ' ③ 若用 Nginx/HTTPS 反代，需把 /api 转发到 8002。'
            );
        }

        let data = null;
        if (trimmed) {
            try {
                data = JSON.parse(text);
            } catch (_) {
                const target = urlHint || response.url || 'API';
                throw new Error(`接口 ${target} 返回了无效 JSON（HTTP ${response.status}）`);
            }
        }

        if (!response.ok) {
            const msg = data && (data.error || data.message);
            throw new Error(msg || `请求失败（HTTP ${response.status}）`);
        }

        return data;
    }

    async function fetchJson(url, options) {
        const response = await fetch(url, options);
        return readJsonResponse(response, url);
    }

    window.MCWWS_readJsonResponse = readJsonResponse;
    window.MCWWS_fetchJson = fetchJson;
})();

export const MAP_RUNTIME: string = `
(() => {
  if (location.origin !== 'https://mcsmap.ryanstudio.work') return false;
  if (window.MCWWSMobileRuntime) return true;

  const style = document.createElement('style');
  style.textContent = [
    'body.mcwws-mobile #app{visibility:hidden!important;pointer-events:none!important}',
    'body.mcwws-mobile #mcwws-shop-panel,body.mcwws-mobile #mcwws-map-controls,',
    'body.mcwws-mobile .mcwws-auth-widget,body.mcwws-mobile .mcwws-ctrl-gis{display:none!important}',
    'body.mcwws-mobile:not(.mcwws-mobile-editor) #mcwws-gis-layer-dialog,',
    'body.mcwws-mobile:not(.mcwws-mobile-editor) #mcwws-mobile-sheet-backdrop{display:none!important}',
    'body.mcwws-mobile-editor #mcwws-gis-layer-dialog{color-scheme:dark;background:#171719;color:#f4f4f5}',
    'body.mcwws-mobile-editor #mcwws-gis-layer-dialog :is(input,select,textarea){color-scheme:dark}',
    'body.mcwws-mobile .mcwws-screen-pin{font-family:system-ui,sans-serif;background:#84919a65!important;',
    'border:1px solid #ffffff70!important;color:#fff!important;text-shadow:0 1px 3px #18212b}',
    'body.mcwws-mobile .mcwws-screen-pin-icon{background:none!important;color:transparent!important;',
    'position:relative;width:18px;height:18px}',
    'body.mcwws-mobile .mcwws-screen-pin-icon:before{content:"";position:absolute;inset:5px;',
    'border:1.5px solid #fff;border-radius:50%}'
  ].join('');
  document.head.append(style);
  document.body.classList.add('mcwws-mobile');
  const q = selector => document.querySelector(selector);
  const control = name => q('.mcwws-ctrl-' + name);
  const tilt = name => control(name)?.classList;
  const allowedControls = new Set(['zoom-in','zoom-out','mode','compass','daynight','night-vision','fly','locate']);
  const switches = {
    'info-toggle':'infoEnabled', 'show-road-names':'roadNames', 'show-building-names':'buildingNames',
    'render-above-320':'above320', 'show-residence-bounds':'residenceBounds'
  };
  const held = new Map();
  let cacheResult = {tiles:0, active:false, busy:false};

  function openLayers() {
    const dialog = q('#mcwws-gis-layer-dialog');
    if (!dialog?.hidden && dialog?.querySelector('[data-gis-info-toggle]')) return true;
    control('gis')?.dispatchEvent(new PointerEvent('pointerup', {bubbles:true, pointerType:'touch'}));
    return !!q('#mcwws-gis-layer-dialog [data-gis-info-toggle]');
  }

  function closeLayers() {
    const dialog = q('#mcwws-gis-layer-dialog');
    if (dialog && !dialog.hidden) dialog.querySelector('[data-action="close-layer-dialog"]')?.click();
  }

  function layerInfo() {
    const data = {};
    for (const key of Object.keys(switches)) {
      const input = q('#mcwws-gis-layer-dialog [data-gis-' + key + ']');
      if (input) data[switches[key]] = input.checked;
    }
    data.simplified = !!q('#mcwws-gis-layer-dialog [data-map-mode="simplified"].is-active');
    return data;
  }

  function getMapApp() {
    const root = document.getElementById('app');
    return root?.__vue_app__?.config?.globalProperties?.$bluemap
      || root?.__vueParentComponent?.appContext?.config?.globalProperties?.$bluemap;
  }

  function read() {
    const app = getMapApp();
    const viewer = app?.mapViewer;
    const maps = Array.isArray(app?.appState?.maps) ? app.appState.maps : [];
    return {
      ready: !!viewer && !!control('mode'),
      mode: tilt('fly')?.contains('is-active') ? '飞行' : q('.mcwws-ctrl-mode-label')?.textContent || '3D',
      map: viewer?.data?.map?.id || location.hash.slice(1).split(':')[0] || 'world',
      night: !!tilt('daynight')?.contains('is-night'),
      locked: !!tilt('daynight')?.contains('is-locked'),
      nightVision: control('night-vision')?.getAttribute('aria-pressed') === 'true',
      following: !!tilt('locate')?.contains('is-active'),
      canFly: !!control('fly') && !control('fly').hidden,
      editor: document.body.classList.contains('mcwws-mobile-editor') && !q('#mcwws-gis-layer-dialog')?.hidden,
      maps: maps.map(entry => typeof entry === 'string' ? entry : entry?.id || entry?.mapId || entry?.map?.id)
        .filter(id => id && id !== 'world_surface'),
      hash: location.hash
    };
  }

  function pressDayLock() {
    const button = control('daynight');
    if (!button || button.disabled) return false;
    const event = new PointerEvent('pointerdown', {bubbles:true, pointerType:'touch', pointerId:42, button:0});
    button.dispatchEvent(event);
    setTimeout(() => {
      button.dispatchEvent(new PointerEvent('pointerup',
        {bubbles:true, pointerType:'touch', pointerId:42, button:0}));
      button.click();
    }, 760);
    return true;
  }

  async function cacheInfo() {
    const names = typeof caches !== 'undefined' ? (await caches.keys()).filter(name => name.startsWith('mcwws-map-')) : [];
    let tiles = 0;
    for (const name of names.filter(name => name.includes('-tiles-'))) tiles += (await (await caches.open(name)).keys()).length;
    return {tiles, active: !!navigator.serviceWorker?.controller};
  }

  function keyInput(key, down) {
    const codes = {w:'KeyW',a:'KeyA',s:'KeyS',d:'KeyD',' ':'Space',Shift:'ShiftLeft'};
    if (!codes[key] || !tilt('fly')?.contains('is-active')) return false;
    const options = {key,code:codes[key],bubbles:true,shiftKey:key === 'Shift' && down};
    const target = q('#map-container canvas') || document.body;
    if (held.has(key)) clearTimeout(held.get(key));
    if (down) {
      target.dispatchEvent(new KeyboardEvent('keydown', options));
      held.set(key, setTimeout(() => {
        held.delete(key);
        target.dispatchEvent(new KeyboardEvent('keyup', options));
      }, 280));
    } else {
      held.delete(key);
      target.dispatchEvent(new KeyboardEvent('keyup', options));
    }
    return true;
  }

  document.addEventListener('click', event => {
    const pin = event.target?.closest?.('#mcwws-shop-pin-layer [data-shop-id]');
    if (!pin || document.body.classList.contains('mcwws-mobile-editor')) return;
    if (!window.MCWWSNativeMap?.marker) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    window.MCWWSNativeMap.marker(pin.dataset.shopId);
  }, true);

  window.MCWWSMobileRuntime = (action, value, flag) => {
    if (action === 'state') return read();
    if (action === 'click' && allowedControls.has(value)) {
      const button = control(value);
      if (!button || button.disabled || button.hidden) return false;
      button.click();
      return true;
    }
    if (action === 'dayLock') return pressDayLock();
    if (action === 'view' && typeof value === 'string') {
      if (!/^#[a-zA-Z0-9_.:-]+$/.test(value)) return false;
      location.hash = value;
      return true;
    }
    if (action === 'layerRead' || action === 'layerSet' || action === 'mapStyle' || action === 'editor') {
      if (!openLayers()) return false;
      if (action === 'editor') {
        document.body.classList.add('mcwws-mobile-editor');
        const button = q('#mcwws-gis-layer-dialog [data-action="toggle-gis-editor"]');
        if (button && !button.disabled && !button.textContent.includes('收起')) button.click();
        return true;
      }
      if (action === 'layerSet' && Object.hasOwn(switches, value)) {
        const input = q('#mcwws-gis-layer-dialog [data-gis-' + value + ']');
        if (input && !input.disabled && input.checked !== flag) input.click();
      }
      if (action === 'mapStyle' && (value === 'original' || value === 'simplified')) {
        q('#mcwws-gis-layer-dialog [data-map-mode="' + value + '"]')?.click();
      }
      const state = layerInfo();
      closeLayers();
      return state;
    }
    if (action === 'editorExit') {
      closeLayers();
      document.body.classList.remove('mcwws-mobile-editor');
      return true;
    }
    if (action === 'key') return keyInput(value, flag);
    if (action === 'cache' || action === 'clearCache') {
      cacheResult.busy = true;
      void (async () => {
        try {
          if (action === 'clearCache' && typeof caches !== 'undefined') {
            const names = (await caches.keys()).filter(name => name.startsWith('mcwws-map-tiles-'));
            await Promise.all(names.map(name => caches.delete(name)));
          }
          cacheResult = {...await cacheInfo(), busy:false};
        } catch {
          cacheResult = {tiles:0,active:false,busy:false};
        }
      })();
      return true;
    }
    if (action === 'cacheResult') return cacheResult;
    return false;
  };
  return true;
})()
`;

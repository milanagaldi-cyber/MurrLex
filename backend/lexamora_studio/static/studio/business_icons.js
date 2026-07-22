(() => {
  const icons = {
    add: '<path d="M12 5v14M5 12h14"/>',
    archive: '<path d="M4 7h16v13H4zM3 4h18v3H3zM9 11h6"/>',
    attach: '<path d="M9 12.5l5.7-5.7a3 3 0 014.2 4.2l-7.8 7.8a5 5 0 01-7.1-7.1l8.2-8.2"/>',
    audio: '<path d="M5 10v4h4l5 4V6l-5 4zM17 9a4 4 0 010 6M19 6a8 8 0 010 12"/>',
    back: '<path d="M10 5l-7 7 7 7M3 12h18"/>',
    chat: '<path d="M4 5h16v11H9l-5 4z"/>',
    clear: '<path d="M4 16l8-11 8 6-7 9H7zM13 20h8"/>',
    close: '<path d="M5 5l14 14M19 5L5 19"/>',
    collapse: '<path d="M5 15l7-7 7 7"/>',
    copy: '<rect x="8" y="8" width="12" height="12"/><path d="M16 8V4H4v12h4"/>',
    crop: '<path d="M7 3v14h14M3 7h14v14"/>',
    delete: '<path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5"/>',
    download: '<path d="M12 3v12M7 10l5 5 5-5M4 20h16"/>',
    down: '<path d="M5 9l7 7 7-7"/>',
    drag: '<circle cx="8" cy="6" r="1"/><circle cx="16" cy="6" r="1"/><circle cx="8" cy="12" r="1"/><circle cx="16" cy="12" r="1"/><circle cx="8" cy="18" r="1"/><circle cx="16" cy="18" r="1"/>',
    edit: '<path d="M4 20l4.5-1 10-10-3.5-3.5-10 10zM13.5 7l3.5 3.5"/>',
    expand: '<path d="M5 9l7 7 7-7"/>',
    export: '<path d="M12 3v12M7 10l5 5 5-5M5 20h14"/>',
    favorite: '<path d="M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9z"/>',
    favoriteFilled: '<path d="M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9z"/>',
    filter: '<path d="M3 5h18l-7 8v6l-4 2v-8z"/>',
    folder: '<path d="M3 6h7l2 2h9v11H3z"/>',
    fullscreen: '<path d="M9 4H4v5M15 4h5v5M9 20H4v-5M15 20h5v-5"/>',
    gridLarge: '<rect x="3" y="3" width="8" height="8"/><rect x="13" y="3" width="8" height="8"/><rect x="3" y="13" width="8" height="8"/><rect x="13" y="13" width="8" height="8"/>',
    gridSmall: '<path d="M3 3h5v5H3zM10 3h4v5h-4zM16 3h5v5h-5zM3 10h5v4H3zM10 10h4v4h-4zM16 10h5v4h-5zM3 16h5v5H3zM10 16h4v5h-4zM16 16h5v5h-5z"/>',
    hidden: '<path d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9 5.3A10.5 10.5 0 0112 5c5.5 0 9 7 9 7a17 17 0 01-2.1 3M6.2 6.2C4.2 7.7 3 12 3 12s3.5 7 9 7a10 10 0 004-.8"/>',
    image: '<rect x="3" y="4" width="18" height="16"/><circle cx="8" cy="9" r="2"/><path d="M4 18l5-5 3 3 3-4 5 6"/>',
    import: '<path d="M12 21V9M7 14l5-5 5 5M5 4h14"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 10v7M12 7h.01"/>',
    left: '<path d="M15 5l-7 7 7 7"/>',
    list: '<path d="M8 5h13M8 12h13M8 19h13M3 5h1M3 12h1M3 19h1"/>',
    join: '<path d="M4 7h6a4 4 0 014 4v6M4 17h6a4 4 0 004-4V7M17 12h4M19 10v4"/>',
    lock: '<rect x="5" y="10" width="14" height="11"/><path d="M8 10V7a4 4 0 018 0v3"/>',
    magic: '<path d="M4 20L17 7M14 4l1-2 1 2 2 1-2 1-1 2-1-2-2-1zM18 13l1-2 1 2 2 1-2 1-1 2-1-2-2-1zM5 6l1-2 1 2 2 1-2 1-1 2-1-2-2-1z"/>',
    menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
    microphone: '<rect x="9" y="3" width="6" height="12" rx="3"/><path d="M5 11a7 7 0 0014 0M12 18v3M8 21h8"/>',
    more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
    open: '<path d="M4 5h7l2 2h7v13H4zM8 12h8M12 8v8"/>',
    paste: '<path d="M9 5h6M9 3h6v4H9zM7 5H5v16h14V5h-2"/><path d="M9 12h6M9 16h6"/>',
    pause: '<path d="M8 5v14M16 5v14"/>',
    play: '<path d="M8 5l11 7-11 7z"/>',
    power: '<path d="M12 2v9M6.3 5.7a8 8 0 1011.4 0"/>',
    properties: '<path d="M4 4h16v16H4zM8 8h8M8 12h8M8 16h5"/>',
    redo: '<path d="M18 8l3 3-3 3M21 11h-9a6 6 0 00-6 6"/>',
    refresh: '<path d="M20 7v5h-5M4 17v-5h5M18.5 8A8 8 0 006 6l-2 3M5.5 16A8 8 0 0018 18l2-3"/>',
    remove: '<path d="M5 12h14"/>',
    restore: '<path d="M4 4v6h6M5.5 9A8 8 0 1120 14M12 8v5l3 2"/>',
    right: '<path d="M9 5l7 7-7 7"/>',
    save: '<path d="M4 3h13l3 3v15H4zM8 3v6h8V3M8 15h8v6H8z"/>',
    snap: '<path d="M6 3v10a6 6 0 0012 0V3M6 8h4M14 8h4"/>',
    search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5L21 21"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 00.3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 00-1.9-.3 1.7 1.7 0 00-1 1.6v.2h-4V21a1.7 1.7 0 00-1-1.6 1.7 1.7 0 00-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 00.3-1.9A1.7 1.7 0 003 14H2.8v-4H3a1.7 1.7 0 001.6-1 1.7 1.7 0 00-.3-1.9L4.2 7 7 4.2l.1.1A1.7 1.7 0 009 4.6 1.7 1.7 0 0010 3v-.2h4V3a1.7 1.7 0 001 1.6 1.7 1.7 0 001.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 00-.3 1.9 1.7 1.7 0 001.6 1h.2v4H21a1.7 1.7 0 00-1.6 1z"/>',
    share: '<circle cx="18" cy="5" r="2.5"/><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="19" r="2.5"/><path d="M8.2 10.8l7.6-4.5M8.2 13.2l7.6 4.5"/>',
    sort: '<path d="M8 5h12M8 12h9M8 19h6M4 4v16M2 18l2 2 2-2"/>',
    split: '<path d="M8 3v7a3 3 0 003 3h2M16 3v7a3 3 0 01-3 3h-2M8 21v-4a3 3 0 013-3h2M16 21v-4a3 3 0 00-3-3h-2"/>',
    stop: '<rect x="6" y="6" width="12" height="12"/>',
    theme: '<path d="M14 3a9 9 0 107 13 8 8 0 01-7-13z"/>',
    themeDark: '<path d="M14 3a9 9 0 107 13 8 8 0 01-7-13z"/>',
    themeLight: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4"/>',
    themeBusiness: '<path d="M4 7h16v13H4zM8 7V4h8v3M8 11h2M14 11h2M8 15h2M14 15h2M11 20v-4h2v4"/>',
    target: '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/>',
    translate: '<path d="M3 5h10M8 3v2M5 8c1 3 3 5 6 7M11 8c-1 3-3 5-6 7M14 20l3-8 3 8M15 17h4"/>',
    undo: '<path d="M6 8l-3 3 3 3M3 11h9a6 6 0 016 6"/>',
    unlock: '<rect x="5" y="10" width="14" height="11"/><path d="M16 10V7a4 4 0 00-7-2"/>',
    up: '<path d="M5 15l7-7 7 7"/>',
    upload: '<path d="M12 21V9M7 14l5-5 5 5M5 4h14"/>',
    users: '<path d="M16 20v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2M9 10a4 4 0 100-8 4 4 0 000 8zM22 20v-2a4 4 0 00-3-3.9M16 2.1a4 4 0 010 7.8"/>',
    video: '<rect x="3" y="5" width="14" height="14"/><path d="M17 10l4-3v10l-4-3z"/>',
    visible: '<path d="M3 12s3.5-7 9-7 9 7 9 7-3.5 7-9 7-9-7-9-7z"/><circle cx="12" cy="12" r="3"/>',
    zoomIn: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5L21 21M7.5 10.5h6M10.5 7.5v6"/>',
    zoomOut: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5L21 21M7.5 10.5h6"/>'
  };

  const rules = [
    ['users', /share workspace|workspace access|manage users|people with access/],
    ['menu', /application navigation|main menu|open menu/],
    ['theme', /switch theme|dark theme|light theme|business theme/],
    ['power', /toggle ai|ai enabled|ai paused/],
    ['gridLarge', /large (images|videos|cards|tiles)|large preview/],
    ['gridSmall', /small (images|videos|cards|tiles)|compact preview/],
    ['list', /^\s*list\s*$|list view/],
    ['collapse', /\bcollapse\b/],
    ['expand', /\bexpand\b/],
    ['fullscreen', /full ?screen/],
    ['crop', /\bcrop\b/],
    ['microphone', /microphone|voice input|speech recognition|record voice/],
    ['translate', /\btranslate\b|translation/],
    ['chat', /\bchat\b|ask assistant/],
    ['magic', /\bimprove\b|generate (image|video|comic)|ai suggestion|auto rough cut/],
    ['split', /\bsplit\b|cut at playhead/],
    ['join', /\bjoin\b|merge clips|group clips/],
    ['drag', /\bdrag\b|drag handle/],
    ['snap', /magnetic snapping|\bsnap\b/],
    ['target', /\bcenter\b|align .*playhead|reset position/],
    ['zoomIn', /increase .*scale|zoom in|enlarge/],
    ['zoomOut', /reduce .*scale|zoom out|shrink/],
    ['pause', /\bpause\b/],
    ['stop', /^\s*stop\b|stop playback|stop recording/],
    ['play', /\bplay\b|preview video/],
    ['hidden', /hide track|hidden|hide section/],
    ['visible', /show track|show section|visibility/],
    ['unlock', /\bunlock\b/],
    ['lock', /\block\b/],
    ['audio', /\bmute\b|\bunmute\b|play sound|speaker/],
    ['folder', /create folder|open folder|media folder/],
    ['properties', /\bproperties\b|view details|file details/],
    ['sort', /\bsort\b|order by/],
    ['filter', /\bfilter\b/],
    ['back', /\bback\b|return to/],
    ['up', /move .*\bup\b|track above|video up|previous item/],
    ['down', /move .*\bdown\b|track below|video down|next item/],
    ['left', /move .*\bleft\b|previous scene|previous scenes|previous (frame|keyframe)|one frame back/],
    ['right', /move .*\bright\b|next scene|next scenes|next (frame|keyframe)|one frame forward/],
    ['redo', /\bredo\b|restore improvement/],
    ['undo', /\bundo\b|cancel improvement/],
    ['copy', /\bcopy\b|duplicate|clone/],
    ['paste', /\bpaste\b|clipboard/],
    ['favorite', /favorite|starred|\bstar\b/],
    ['restore', /\brestore\b|recover|\bhistory\b/],
    ['archive', /\barchive\b/],
    ['delete', /\bdelete\b|trash|recycle bin|purge|permanently/],
    ['attach', /\battach\b|link image|link media/],
    ['remove', /\bremove\b|detach|revoke|minus/],
    ['close', /^\s*(close|cancel|dismiss)\b/],
    ['import', /\bimport\b|load context/],
    ['export', /\bexport\b|save as/],
    ['upload', /\bupload\b/],
    ['download', /\bdownload\b/],
    ['save', /\bsave\b|^\s*apply\b|confirm changes|^\s*confirm\b|^\s*done\b|set as avatar/],
    ['clear', /\bclear\b|erase/],
    ['edit', /\bedit\b|rename|change avatar|change cover/],
    ['settings', /\bsettings?\b|configure|preferences/],
    ['share', /\bshare\b|permissions?|^\s*send\b/],
    ['search', /\bsearch\b/],
    ['refresh', /\brefresh\b|retry|reload|replay/],
    ['open', /\bopen\b/],
    ['add', /\badd\b|\bcreate\b|\bnew\b/],
    ['info', /\binfo\b|information|help/],
    ['more', /more actions|more options/]
  ];

  window.LexamoraIconPack = Object.freeze({
    name: 'command-monoline-24',
    size: 24,
    icons: Object.freeze({...icons})
  });

  const selector = [
    'button',
    'a.button',
    'a.icon-button',
    'a[title][aria-label]',
    'summary[title]',
    '.icon-button',
    '.icon-tool',
    '.asset-action-icon',
    '.workspace-action-icon',
    '.dialog-close',
    '.timeline-scroll',
    '[role="button"]'
  ].join(',');
  const labelFor = element => [
    element.dataset?.businessCommand,
    element.getAttribute('aria-label'),
    element.getAttribute('title'),
    element.dataset?.readyLabel,
    element.textContent
  ].filter(Boolean).join(' ').replace(/\s+/g, ' ').trim().toLowerCase();

  const isIconOnly = element => {
    if (element.matches('.icon-button,.icon-tool,.asset-action-icon,.workspace-action-icon,.dialog-close,.timeline-scroll,.premium-crown-link,.theme-toggle,.section-add-button')) return true;
    const compact = (element.textContent || '').replace(/\s+/g, '');
    return compact.length <= 2 && (/^[A-Z][+\-]?$/.test(compact) || /^[+\-\u2212\u00d7x\u2190-\u21ff\u2303\u2304\u25a0-\u25ff\u2600-\u27ff]+$/u.test(compact));
  };

  const decorate = element => {
    if (!(element instanceof HTMLElement) || !element.matches(selector)) return;
    const explicit = element.dataset.businessCommand;
    if (!explicit && (element.matches('.language-code-button,[data-mic-language-code],[data-translation-language-code],.video-gallery-preview,.generation-job-visual,.asset-picker-card,.generation-reference-choice') || element.querySelector(':scope > video,:scope > img,:scope > picture'))) return;
    const match = explicit && icons[explicit] ? [explicit] : rules.find(([, pattern]) => pattern.test(labelFor(element)));
    if (!match) return;
    const [name] = match;
    element.classList.toggle('business-icon-only', isIconOnly(element));
    if (element.dataset.businessIcon === name && element.querySelector(':scope > .business-command-icon')) return;
    element.dataset.businessIcon = name;
    element.querySelector(':scope > .business-command-icon')?.remove();
    element.querySelectorAll(':scope > svg,:scope > [aria-hidden="true"]').forEach(legacy => {
      if (!legacy.classList.contains('business-command-icon')) legacy.classList.add('business-legacy-command-icon');
    });
    const icon = document.createElement('span');
    icon.className = 'business-command-icon command-icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.innerHTML = `<svg viewBox="0 0 24 24" focusable="false">${icons[name]}</svg>`;
    element.prepend(icon);
  };

  const decorateTree = root => {
    if (root instanceof HTMLElement) decorate(root);
    root.querySelectorAll?.(selector).forEach(decorate);
  };

  const start = () => {
    decorateTree(document);
    new MutationObserver(records => records.forEach(record => {
      if (record.type === 'attributes') decorate(record.target);
      record.addedNodes.forEach(node => { if (node.nodeType === Node.ELEMENT_NODE) decorateTree(node); });
    })).observe(document.body, {subtree:true, childList:true, attributes:true, attributeFilter:['title','aria-label','data-business-command']});
  };
  document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', start, {once:true}) : start();
})();

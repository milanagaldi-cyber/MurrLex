(() => {
  const icons = {
    add: '<path d="M12 5v14M5 12h14"/>',
    archive: '<path d="M4 7h16v13H4zM3 4h18v3H3zM9 11h6"/>',
    attach: '<path d="M9 12.5l5.7-5.7a3 3 0 014.2 4.2l-7.8 7.8a5 5 0 01-7.1-7.1l8.2-8.2"/>',
    close: '<path d="M5 5l14 14M19 5L5 19"/>',
    copy: '<rect x="8" y="8" width="12" height="12"/><path d="M16 8V4H4v12h4"/>',
    delete: '<path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5"/>',
    download: '<path d="M12 3v12M7 10l5 5 5-5M4 20h16"/>',
    edit: '<path d="M4 20l4.5-1 10-10-3.5-3.5-10 10zM13.5 7l3.5 3.5"/>',
    export: '<path d="M12 3v12M7 10l5 5 5-5M5 20h14"/>',
    favorite: '<path d="M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9z"/>',
    filter: '<path d="M3 5h18l-7 8v6l-4 2v-8z"/>',
    import: '<path d="M12 21V9M7 14l5-5 5 5M5 4h14"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 10v7M12 7h.01"/>',
    open: '<path d="M4 5h7l2 2h7v13H4zM8 12h8M12 8v8"/>',
    paste: '<path d="M9 5h6M9 3h6v4H9zM7 5H5v16h14V5h-2"/><path d="M9 12h6M9 16h6"/>',
    redo: '<path d="M18 8l3 3-3 3M21 11h-9a6 6 0 00-6 6"/>',
    refresh: '<path d="M20 7v5h-5M4 17v-5h5M18.5 8A8 8 0 006 6l-2 3M5.5 16A8 8 0 0018 18l2-3"/>',
    remove: '<path d="M5 12h14"/>',
    restore: '<path d="M4 4v6h6M5.5 9A8 8 0 1120 14M12 8v5l3 2"/>',
    save: '<path d="M4 3h13l3 3v15H4zM8 3v6h8V3M8 15h8v6H8z"/>',
    search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5L21 21"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9L7 7M17 17l2.1 2.1M19.1 4.9L17 7M7 17l-2.1 2.1"/>',
    share: '<circle cx="18" cy="5" r="2.5"/><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="19" r="2.5"/><path d="M8.2 10.8l7.6-4.5M8.2 13.2l7.6 4.5"/>',
    undo: '<path d="M6 8l-3 3 3 3M3 11h9a6 6 0 016 6"/>',
    upload: '<path d="M12 21V9M7 14l5-5 5 5M5 4h14"/>'
  };

  const rules = [
    ['redo', /\bredo\b|restore improvement/],
    ['undo', /\bundo\b|cancel improvement/],
    ['copy', /\bcopy\b|duplicate|clone/],
    ['paste', /\bpaste\b|clipboard/],
    ['restore', /\brestore\b|recover/],
    ['archive', /\barchive\b/],
    ['delete', /\bdelete\b|trash|purge|empty recycle|permanently/],
    ['remove', /\bremove\b|detach|revoke|minus/],
    ['close', /^\s*(close|cancel|dismiss)\b/],
    ['import', /\bimport\b/],
    ['export', /\bexport\b|save as/],
    ['upload', /\bupload\b/],
    ['download', /\bdownload\b/],
    ['save', /\bsave\b|apply changes|confirm changes/],
    ['edit', /\bedit\b|rename|change avatar|change cover|improve/],
    ['settings', /\bsettings?\b|configure|preferences/],
    ['share', /\bshare\b|access|permissions?/],
    ['search', /\bsearch\b/],
    ['filter', /\bfilter\b/],
    ['refresh', /\brefresh\b|retry|reload|replay/],
    ['favorite', /favorite|starred|\bstar\b/],
    ['attach', /\battach\b|link image|link media/],
    ['open', /\bopen\b|view details|properties/],
    ['add', /\badd\b|\bcreate\b|\bnew\b/],
    ['info', /\binfo\b|information|help/]
  ];

  const labelFor = element => [
    element.getAttribute('aria-label'),
    element.getAttribute('title'),
    element.dataset?.readyLabel,
    element.textContent
  ].filter(Boolean).join(' ').replace(/\s+/g, ' ').trim().toLowerCase();

  const decorate = element => {
    if (!(element instanceof HTMLElement) || !element.matches('button,a.button,.icon-button,[role="button"]')) return;
    const match = rules.find(([, pattern]) => pattern.test(labelFor(element)));
    if (!match) return;
    const [name] = match;
    if (element.dataset.businessIcon === name && element.querySelector(':scope > .business-command-icon')) return;
    element.dataset.businessIcon = name;
    element.querySelector(':scope > .business-command-icon')?.remove();
    const icon = document.createElement('span');
    icon.className = 'business-command-icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.innerHTML = `<svg viewBox="0 0 24 24" focusable="false">${icons[name]}</svg>`;
    element.prepend(icon);
  };

  const decorateTree = root => {
    if (root instanceof HTMLElement) decorate(root);
    root.querySelectorAll?.('button,a.button,.icon-button,[role="button"]').forEach(decorate);
  };

  const start = () => {
    decorateTree(document);
    new MutationObserver(records => records.forEach(record => {
      if (record.type === 'attributes') decorate(record.target);
      record.addedNodes.forEach(node => { if (node.nodeType === Node.ELEMENT_NODE) decorateTree(node); });
    })).observe(document.body, {subtree:true, childList:true, attributes:true, attributeFilter:['title','aria-label']});
  };
  document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', start, {once:true}) : start();
})();

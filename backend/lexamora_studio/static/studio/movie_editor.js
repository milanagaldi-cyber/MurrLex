(() => {
  const root = document.querySelector("[data-movie-editor]");
  if (!root) return;
  document.documentElement.classList.add("movie-editor-page-active");
  document.body.classList.add("movie-editor-page-active");

  const canEdit = root.dataset.canEdit === "true";
  const canExport = root.dataset.canExport === "true";
  const readJson = id => {
    try { return JSON.parse(document.getElementById(id)?.textContent || "[]"); }
    catch (_) { return []; }
  };
  let assets = readJson("movie-assets-data");
  let libraryAssets = readJson("movie-library-data");
  let renderJobs = readJson("movie-render-jobs-data");
  let movieEdits = readJson("movie-edits-data");
  let timelineId = root.dataset.timelineId || "";
  const saved = readJson("movie-timeline-data");
  let timeline = saved && Array.isArray(saved.tracks) ? saved : {schemaVersion: 1, tracks: []};
  timeline.mediaAssetIds = Array.isArray(timeline.mediaAssetIds) ? timeline.mediaAssetIds : [];
  timeline.mediaOrder = Array.isArray(timeline.mediaOrder) ? timeline.mediaOrder : [];
  timeline.mediaFolders = Array.isArray(timeline.mediaFolders) ? timeline.mediaFolders : [];
  let selectedId = null;
  let selectedIds = new Set();
  let selectedTrackId = null;
  let trackContextId = null;
  let clipClipboard = [];
  let selectedMediaIds = new Set();
  let selectedMediaFolderId = null;
  let mediaClipboardIds = [];
  let mediaClipboardMode = "copy";
  let mediaSelectionActive = false;
  let pasteTargetTrackId = null;
  const setMediaContextMode = mode => {
    if (!mediaContext) return;
    const free = mode === "free";
    ["select-all", "cut", "copy", "root", "download", "delete"].forEach(name => {
      const node = mediaContext.querySelector(`[data-media-context-${name}]`);
      if (node) node.hidden = free;
    });
    ["paste", "properties"].forEach(name => {
      const node = mediaContext.querySelector(`[data-media-context-${name}]`);
      if (node) node.hidden = false;
    });
  };
  let playheadMs = 0;
  let dirty = false;
  let snapping = true;
  let zoom = Math.max(1, Math.min(160, Number(localStorage.getItem("studio-movie-zoom")) || 24));
  let playing = false;
  let playbackFrame = 0;
  let playbackOrigin = 0;
  let playbackStartedAt = 0;
  let lastPlaybackSync = 0;
  let mediaPoll = null;
  let renderPoll = null;
  let historyUndo = [];
  let historyRedo = [];
  let savedSignature = "";
  let libraryScope = "project";
  let standalonePreviewAssetId = null;
  let currentMediaFolderId = null;
  let mediaContextAsset = null;
  let mediaContextFolderId = null;
  let pendingUploads = [];
  const mediaSortKey = `studio-movie-media-sort-${timelineId}`;
  const defaultTrackHeight = Math.max(21, Math.min(210, Number(localStorage.getItem("studio-movie-track-height")) || 63));
  let mediaView = ["list", "small", "large"].includes(localStorage.getItem("studio-movie-media-view")) ? localStorage.getItem("studio-movie-media-view") : "list";
  const previewZoomStorageKey = `studio-movie-preview-zoom-${timelineId}`;
  const storedPreviewZoom = localStorage.getItem(previewZoomStorageKey);
  let previewZoomManual = storedPreviewZoom !== null;
  let previewZoom = previewZoomManual
    ? Math.max(.05, Math.min(2, Number(storedPreviewZoom) || 1))
    : 1;
  let inspectorTab = "VIDEO";
  let previewGeometry = null;
  let autosaveTimer = null;
  let savePromise = null;
  let saveAgain = false;
  const settingSnapshots = new WeakMap();
  const PRECISION_MS = 10;

  const q = selector => root.querySelector(selector);
  const qa = selector => [...root.querySelectorAll(selector)];
  const updateMurrCutPageAnchor = () => {
    const main = root.closest("main");
    const left = Math.max(0, Math.round(main?.getBoundingClientRect().left || 0));
    root.style.setProperty("--murrcut-page-left", `${left}px`);
  };
  const bin = q("[data-movie-bin]");
  const tracksNode = q("[data-movie-tracks]");
  const headsNode = q("[data-track-heads]");
  const preview = q("[data-movie-preview]");
  const previewImage = q("[data-movie-preview-image]");
  const previewBody = q(".movie-preview-body");
  const previewStage = q("[data-preview-stage]");
  const previewLayers = q("[data-preview-layers]");
  const previewOutline = q("[data-preview-outline]");
  const previewEmpty = q("[data-preview-empty]");
  const previewStatus = q("[data-preview-status]");
  const previewScrub = q("[data-preview-scrub]");
  const inspector = q("[data-clip-inspector]");
  const inspectorActions = q("[data-inspector-actions]");
  const previewZoomInput = q("[data-preview-zoom]");
  const previewZoomValue = q("[data-preview-zoom-value]");
  const frameSizeInput = q("[data-frame-size]");
  const frameSizeValue = q("[data-frame-size-value]");
  const frameScaleValue = q("[data-frame-scale-value]");
  const stateNode = q("[data-movie-state]");
  const playheadLabel = q("[data-playhead-label]");
  const previewTime = q("[data-preview-time]");
  const playheadNode = q("[data-playhead]");
  const snapGuide = q("[data-snap-guide]");
  const marquee = q("[data-marquee]");
  const timelineScroll = q("[data-timeline-scroll]");
  const timelineCanvas = q("[data-timeline-canvas]");
  const ruler = q("[data-movie-ruler]");
  const historyPanel = q("[data-movie-history]");
  const historyList = q("[data-movie-history-list]");
  const renderPanel = q("[data-movie-renders]");
  const renderList = q("[data-render-list]");
  const zoomInput = q("[data-timeline-zoom]");
  const libraryDialog = q("[data-media-library-dialog]");
  const libraryGrid = q("[data-media-library-grid]");
  const mediaFoldersNode = q("[data-media-folders]");
  const mediaContext = q("[data-movie-media-context]");
  const folderDialog = q("[data-media-folder-dialog]");
  const trackContext = q("[data-track-context]");
  const editorLayout = q("[data-editor-layout]");
  const editorViewport = q("[data-editor-workspace-viewport]");
  const editorWorld = q("[data-editor-workspace-world]");
  const editorProjectHeader = q("[data-editor-project-header]");
  const tileCameraScrollbar = q("[data-tile-camera-scrollbar]");
  const tileCameraTrack = q("[data-tile-camera-track]");
  const tileCameraThumb = q("[data-tile-camera-thumb]");
  const editProjectsPanel = document.querySelector('[data-montage-gallery][data-gallery-key="draft-editor"]');
  const editorHeaderPanel = q('[data-panel-key="editorHeader"]');
  if (editProjectsPanel) {
    editProjectsPanel.dataset.panelKey = "editProjects";
    editProjectsPanel.classList.add("panel", "movie-collapsible", "movie-edit-projects-tile");
    const editProjectsDrag = editProjectsPanel.querySelector(":scope > summary") || editProjectsPanel;
    editProjectsDrag.dataset.panelDrag = "editProjects";
  }
  if (editorLayout) {
    if (editProjectsPanel) editorLayout.appendChild(editProjectsPanel);
    if (editorHeaderPanel) editorLayout.appendChild(editorHeaderPanel);
  }
  const layoutPanels = {
    editProjects: editProjectsPanel,
    editorHeader: editorHeaderPanel,
    toolbar: q('[data-panel-key="toolbar"]'),
    history: historyPanel,
    exports: renderPanel,
    media: q('[data-panel-key="media"]'),
    preview: q('[data-panel-key="preview"]'),
    inspector: q('[data-panel-key="inspector"]'),
    timeline: q('[data-panel-key="timeline"]'),
  };
  const assetMap = new Map();
  const audioPlayers = new Map();
  const visualPlayers = new Map();

  const layoutEngineVersion = "murrcut-tiles-v1";
  const layoutModeKey = `studio-movie-layout-mode-${layoutEngineVersion}-${timelineId}`;
  const layoutStateKey = mode => `studio-movie-layout-${layoutEngineVersion}-${timelineId}-${mode}`;
  const savedLayoutKey = `studio-movie-layout-saved-${layoutEngineVersion}-${timelineId}`;
  const floatingPanelsKey = `studio-movie-floating-panels-${layoutEngineVersion}-${timelineId}`;
  const layoutLockKey = `studio-movie-layout-locked-${layoutEngineVersion}-${timelineId}`;
  let layoutLocked = localStorage.getItem(layoutLockKey) === "true";
  let floatingPanels = {};
  const tilePanelKeys = ["editProjects", "editorHeader", "media", "preview", "inspector", "timeline"];
  const tileMinimums = {
    editProjects: {width: 560, height: 120},
    editorHeader: {width: 560, height: 48},
    media: {width: 180, height: 150},
    preview: {width: 300, height: 220},
    inspector: {width: 220, height: 120},
    timeline: {width: 420, height: 140},
  };
  const tileMaximumScale = key => ["media", "preview", "inspector", "timeline"].includes(key) ? 4 : 2;
  const tileEdgeSize = 18;
  const tileWorkspaceInset = Math.ceil(tileEdgeSize / 2) + 2;
  const tileSnapDistance = 12;
  let tileLayoutDefaults = null;
  let selectedTileKeys = new Set();
  const layoutDefaults = {
    columns: {mediaWidth: 250, inspectorWidth: 390, mediaHeight: 390, canvasHeight: 390, inspectorHeight: 390, timelineHeight: 320, timelineInsetLeft: 0, timelineInsetRight: 0},
    stacked: {mediaWidth: 250, inspectorWidth: 390, mediaHeight: 390, canvasHeight: 390, inspectorHeight: 390, timelineHeight: 320, timelineInsetLeft: 0, timelineInsetRight: 0},
  };
  const readLayoutGeometry = mode => {
    try {
      const stored = JSON.parse(localStorage.getItem(layoutStateKey(mode)) || "{}");
      return {...layoutDefaults[mode], ...stored};
    } catch (_) {
      return {...layoutDefaults[mode]};
    }
  };
  let editorLayoutMode = "columns";
  let editorLayouts = {
    columns: readLayoutGeometry("columns"),
    stacked: readLayoutGeometry("stacked"),
  };
  const previewDockKey = `studio-movie-preview-dock-${timelineId}`;
  const panelGapKey = `studio-movie-panel-gap-${timelineId}`;
  const workspacePatternKey = `studio-movie-workspace-pattern-${timelineId}`;
  const workspacePatterns = ["none", "grid", "dots", "stars", "hearts", "cats"];
  let previewDockMode = localStorage.getItem(previewDockKey) === "bottom" ? "bottom" : "center";
  let widePanelGaps = localStorage.getItem(panelGapKey) === "wide";
  let workspacePattern = workspacePatterns.includes(localStorage.getItem(workspacePatternKey))
    ? localStorage.getItem(workspacePatternKey)
    : "grid";
  const applyPanelGapMode = () => editorLayout?.classList.toggle("movie-wide-gaps", widePanelGaps);
  const applyWorkspacePattern = value => {
    workspacePattern = workspacePatterns.includes(value) ? value : "grid";
    root.dataset.workspacePattern = workspacePattern;
  };
  const applyEditorWallpaper = value => {
    root.classList.remove("has-editor-wallpaper");
    document.body.classList.remove("movie-editor-wallpaper-active");
    ["background-image", "background-position", "background-repeat", "background-size"].forEach(property => root.style.removeProperty(property));
    ["--movie-editor-wallpaper", "background-image", "background-position", "background-repeat", "background-size", "background-attachment"].forEach(property => document.body.style.removeProperty(property));
    root.style.removeProperty("--movie-editor-wallpaper");
  };
  applyEditorWallpaper(root.dataset.editorWallpaper);
  applyWorkspacePattern(workspacePattern);
  const applyPreviewDock = () => {
    previewBody?.classList.toggle("dock-bottom", previewDockMode === "bottom");
    const button = q("[data-preview-dock]");
    if (button) {
      button.classList.toggle("active", previewDockMode === "bottom");
      button.setAttribute("aria-pressed", previewDockMode === "bottom" ? "true" : "false");
      button.title = previewDockMode === "bottom" ? "Center canvas" : "Dock canvas near timeline";
    }
  };

  const workspacePanel = key => tilePanelKeys.includes(key);
  const tileRect = value => ({
    x: Number(value?.x || 0),
    y: Number(value?.y || 0),
    width: Number(value?.width || 0),
    height: Number(value?.height || 0),
  });
  const createDefaultTileLayout = () => {
    const viewportWidth = Number(editorViewport?.clientWidth || document.documentElement.clientWidth || root.clientWidth || 1240);
    const horizontalViewportGutter = viewportWidth >= 1080 ? 64 : 0;
    const availableWidth = clamp(
      viewportWidth - horizontalViewportGutter,
      960,
      1600,
    );
    const gap = 8;
    const mediaWidth = clamp(Math.round(availableWidth * .2), 220, 300);
    const inspectorWidth = clamp(Math.round(availableWidth * .27), 300, 430);
    const previewWidth = Math.max(360, availableWidth - mediaWidth - inspectorWidth - gap * 2);
    const editProjectsHeight = 220;
    const editorHeaderHeight = 58;
    const upperHeight = 390;
    const timelineHeight = 320;
    const editProjectsY = tileWorkspaceInset;
    const editorHeaderY = editProjectsY + editProjectsHeight + gap;
    const upperY = editorHeaderY + editorHeaderHeight + gap;
    const timelineY = upperY + upperHeight + gap;
    // Logical coordinates keep one full editor screen available on either side.
    // The DOM workspace is derived separately, so this reserve does not create
    // a scrollbar until a panel actually uses it.
    const reserveX = availableWidth + tileWorkspaceInset;
    const tiles = {
      editProjects: {x: reserveX, y: editProjectsY, width: availableWidth, height: editProjectsHeight},
      editorHeader: {x: reserveX, y: editorHeaderY, width: availableWidth, height: editorHeaderHeight},
      media: {x: reserveX, y: upperY, width: mediaWidth, height: upperHeight},
      preview: {x: reserveX + mediaWidth + gap, y: upperY, width: previewWidth, height: upperHeight},
      inspector: {x: reserveX + mediaWidth + gap + previewWidth + gap, y: upperY, width: inspectorWidth, height: upperHeight},
      timeline: {x: reserveX, y: timelineY, width: availableWidth, height: timelineHeight},
    };
    const defaultContentHeight = timelineY + timelineHeight + tileWorkspaceInset;
    const defaultFrameHeight = timelineY + timelineHeight - editProjectsY;
    const workspaceWidth = availableWidth * 3 + tileWorkspaceInset * 2;
    // The permanent lower reserve is two logical home-frame heights. It is
    // independent of browser viewport height, so browser zoom cannot resize it.
    const workspaceHeight = defaultContentHeight + Math.round(defaultFrameHeight * 2);
    const baseHeight = workspaceHeight;
    const defaults = {
      workspace: {
        width: Math.round(workspaceWidth),
        height: Math.round(workspaceHeight),
        baseWidth: availableWidth,
        baseHeight,
      },
      tiles,
    };
    tileLayoutDefaults = structuredClone(defaults);
    return defaults;
  };
  const normalizeTileLayout = value => {
    const defaults = tileLayoutDefaults || createDefaultTileLayout();
    const source = value?.tiles && value?.workspace ? value : defaults;
    const sourceBaseWidth = Number(source.workspace?.baseWidth || defaults.workspace.baseWidth);
    const horizontalRebase = defaults.workspace.baseWidth - sourceBaseWidth;
    const workspace = {
      width: Number(source.workspace?.width || defaults.workspace.width),
      height: defaults.workspace.height,
      baseWidth: defaults.workspace.baseWidth,
      baseHeight: defaults.workspace.baseHeight,
    };
    workspace.width = Math.max(defaults.workspace.width, workspace.width);
    workspace.height = Math.max(defaults.workspace.height, workspace.height);
    const baseLeft = defaults.tiles.editProjects.x;
    const baseRight = baseLeft + defaults.workspace.baseWidth;
    const horizontalReserve = Math.max(
      defaults.workspace.baseWidth,
      Number(editorViewport?.clientWidth || defaults.workspace.baseWidth),
    );
    const horizontalLeft = baseLeft - horizontalReserve;
    const horizontalRight = baseRight + horizontalReserve;
    const tiles = {};
    tilePanelKeys.forEach(key => {
      const fallback = defaults.tiles[key];
      const current = tileRect(source.tiles?.[key] || fallback);
      const minimum = tileMinimums[key];
      const maximumScale = tileMaximumScale(key);
      const width = clamp(current.width || fallback.width, minimum.width, Math.min(workspace.width - tileWorkspaceInset * 2, fallback.width * maximumScale));
      const height = clamp(current.height || fallback.height, minimum.height, Math.min(workspace.height - tileWorkspaceInset * 2, fallback.height * maximumScale));
      tiles[key] = {
        x: clamp(current.x + horizontalRebase, horizontalLeft, horizontalRight - width),
        y: clamp(current.y, tileWorkspaceInset, Math.max(tileWorkspaceInset, workspace.height - height - tileWorkspaceInset)),
        width,
        height,
      };
    });
    return {workspace, tiles};
  };
  const ensureTileLayout = () => {
    if (!tileLayoutDefaults) createDefaultTileLayout();
    const current = editorLayouts.columns;
    const normalized = normalizeTileLayout(current?.tiles && current?.workspace ? current : tileLayoutDefaults);
    editorLayouts.columns = {...current, ...normalized};
    editorLayouts.stacked = {...editorLayouts.stacked, ...normalized};
    return normalized;
  };
  let tileViewportGeometry = null;
  let tileAllocatedExtent = null;
  let tileRenderedTiles = null;
  let tileExtentCleanupTimer = 0;
  let tileExtentCleanupSuppressedUntil = 0;
  let tileMoveDragActive = false;
  let tileHomeCenterFrame = 0;
  let tileCenterAxisTimer = 0;
  let tileViewportScrollSettleTimer = 0;
  let tileViewportScrollbarDragPointerId = null;
  let tileViewportScrollbarDragChanged = false;
  let tileViewportHorizontalIdleTimer = 0;
  let tileViewportVerticalIdleTimer = 0;
  let tileViewportInitialCenterComplete = false;
  let tileCameraX = 0;
  let tileCameraTargetX = 0;
  let tileCameraVelocityX = 0;
  let tileCameraAnimationMaximumVelocity = 28;
  let tileCameraFrame = 0;
  let tileCameraTrackWidth = 1;
  let tileViewportHorizontalHotZoneHovered = false;
  let tileViewportVerticalHotZoneHovered = false;
  let tileViewportHorizontalRevealRequested = false;
  let pendingTileCameraLogicalCenterX = null;
  const tileViewportMinimumReveal = 48;
  const tileExtentCleanupMargin = 32;
  const tileViewportHorizontalActivationRatio = .12;
  const tileViewportHorizontalOverscanRatio = 1;
  const tileViewportScrollbarIdleDelay = 3000;
  const tileWorkspaceTopReveal = 32;
  const tileBounds = tiles => {
    const rects = tilePanelKeys.map(key => tiles[key]).filter(Boolean);
    return {
      left: Math.min(...rects.map(rect => rect.x)),
      right: Math.max(...rects.map(rect => rect.x + rect.width)),
      top: Math.min(...rects.map(rect => rect.y)),
      bottom: Math.max(...rects.map(rect => rect.y + rect.height)),
    };
  };
  const tilesFitDefaultDisplayZone = tiles => {
    if (!tiles) return false;
    const defaults = tileLayoutDefaults || createDefaultTileLayout();
    const home = tileBounds(defaults.tiles);
    const current = tileBounds(tiles);
    return (
      current.left >= home.left
      && current.right <= home.right
      && current.top >= home.top
      && current.bottom <= home.bottom
    );
  };
  const computeTileViewportGeometry = (tiles, workspace, allocatedExtent = null) => {
    const defaults = tileLayoutDefaults || createDefaultTileLayout();
    const bounds = tileBounds(tiles);
    const baseLeft = defaults.tiles.editProjects.x;
    const baseTop = defaults.tiles.editProjects.y;
    const baseWidth = workspace.baseWidth || defaults.workspace.baseWidth;
    const baseHeight = workspace.baseHeight || defaults.workspace.baseHeight;
    const workspaceWidth = Math.max(
      baseWidth,
      Number(workspace.width || defaults.workspace.width || baseWidth),
    );
    const baseRight = baseLeft + baseWidth;
    const viewportWidth = Math.max(1, editorViewport?.clientWidth || baseWidth);
    const viewportHeight = Math.max(
      1,
      (editorViewport?.clientHeight || baseHeight) - Number(editorLayout?.offsetTop || 0),
    );
    const coreWidth = Math.max(baseWidth, viewportWidth);
    const horizontalReserveLimit = Math.max(
      baseWidth,
      viewportWidth * tileViewportHorizontalOverscanRatio,
    );
    const requiredExtent = {
      left: Math.min(baseLeft, bounds.left),
      right: Math.max(baseRight, bounds.right),
      // Vertical space is fixed: the home layout plus two home-frame heights.
      bottom: Math.max(baseTop, Number(workspace.height || 0), bounds.bottom),
    };
    const extent = {
      left: clamp(
        Math.min(requiredExtent.left, Number(allocatedExtent?.left ?? requiredExtent.left)),
        baseLeft - horizontalReserveLimit,
        baseLeft,
      ),
      right: clamp(
        Math.max(requiredExtent.right, Number(allocatedExtent?.right ?? requiredExtent.right)),
        baseRight,
        baseRight + horizontalReserveLimit,
      ),
      bottom: Math.max(
        requiredExtent.bottom,
        Number(allocatedExtent?.bottom ?? requiredExtent.bottom),
      ),
    };
    const leftReserve = baseLeft - extent.left;
    const rightReserve = extent.right - baseRight;
    const occupiedHeight = Math.max(0, extent.bottom - baseTop);
    const centerX = Math.max(0, (coreWidth - baseWidth) / 2);
    const baseDisplayLeft = Math.max(centerX, leftReserve);
    const workspaceDisplayLeft = baseDisplayLeft - baseLeft;
    const workspaceCenterX = workspaceDisplayLeft + workspaceWidth / 2;
    return {
      offsetX: workspaceDisplayLeft,
      offsetY: tileWorkspaceTopReveal - baseTop,
      width: Math.ceil(Math.max(
        coreWidth,
        baseDisplayLeft + baseWidth + rightReserve,
        workspaceDisplayLeft + workspaceWidth,
        workspaceCenterX + viewportWidth / 2,
      )),
      height: Math.ceil(Math.max(
        viewportHeight,
        tileWorkspaceTopReveal + occupiedHeight,
      )),
      bounds,
      baseLeft,
      baseTop,
      baseWidth,
      baseHeight,
      workspaceWidth,
      workspaceCenterX,
      requiredExtent,
      extent,
    };
  };
  const tileWorkspaceViewportOrigin = () => ({
    x: Number(editorWorld?.offsetLeft || 0) + Number(editorLayout?.offsetLeft || 0),
    y: Number(editorWorld?.offsetTop || 0) + Number(editorLayout?.offsetTop || 0),
  });
  const tileCameraMaximumX = (geometry = tileViewportGeometry) => {
    if (!editorViewport || !geometry) return 0;
    const origin = tileWorkspaceViewportOrigin();
    return Math.max(0, origin.x + geometry.width - editorViewport.clientWidth);
  };
  const tileHomeCameraX = (geometry = tileViewportGeometry) => {
    if (!editorViewport || !geometry) return 0;
    const origin = tileWorkspaceViewportOrigin();
    return clamp(
      origin.x + geometry.workspaceCenterX - editorViewport.clientWidth / 2,
      0,
      tileCameraMaximumX(geometry),
    );
  };
  const renderTileCameraScrollbar = () => {
    if (!tileCameraTrack || !tileCameraThumb || !editorViewport) return;
    const trackWidth = tileCameraTrackWidth;
    const worldWidth = Math.max(editorViewport.clientWidth, tileCameraMaximumX() + editorViewport.clientWidth);
    const thumbWidth = clamp(
      trackWidth * editorViewport.clientWidth / worldWidth,
      Math.min(48, trackWidth),
      trackWidth,
    );
    const travel = Math.max(0, trackWidth - thumbWidth);
    const maximum = tileCameraMaximumX();
    const thumbX = maximum > 0 ? tileCameraX / maximum * travel : 0;
    tileCameraThumb.style.width = `${thumbWidth}px`;
    tileCameraThumb.style.setProperty("--murrcut-camera-thumb-x", `${thumbX}px`);
  };
  const positionTileCameraScrollbar = () => {
    if (!tileCameraScrollbar || !editorViewport) return;
    const viewportBounds = editorViewport.getBoundingClientRect();
    tileCameraScrollbar.style.left = `${viewportBounds.left}px`;
    tileCameraScrollbar.style.bottom = `${Math.max(0, window.innerHeight - viewportBounds.bottom)}px`;
    tileCameraScrollbar.style.width = `${viewportBounds.width}px`;
    tileCameraTrackWidth = Math.max(1, viewportBounds.width - 16);
  };
  const renderTileCamera = () => {
    if (editorLayout) {
      editorLayout.style.setProperty("--murrcut-camera-translate-x", `${-tileCameraX}px`);
    }
    if (editorProjectHeader) {
      const homeCameraX = tileViewportGeometry ? tileHomeCameraX(tileViewportGeometry) : tileCameraX;
      editorProjectHeader.style.setProperty(
        "--murrcut-header-camera-translate-x",
        `${homeCameraX - tileCameraX}px`,
      );
    }
    if (tileCameraScrollbar) {
      tileCameraScrollbar.setAttribute("aria-valuemax", String(Math.round(tileCameraMaximumX())));
      tileCameraScrollbar.setAttribute("aria-valuenow", String(Math.round(tileCameraX)));
    }
    renderTileCameraScrollbar();
  };
  const stopTileCameraAnimation = () => {
    if (tileCameraFrame) cancelAnimationFrame(tileCameraFrame);
    tileCameraFrame = 0;
    tileCameraVelocityX = 0;
    tileCameraTargetX = tileCameraX;
  };
  const setTileCameraX = (value, {immediate = true, maxVelocity = 28} = {}) => {
    const next = clamp(Number(value) || 0, 0, tileCameraMaximumX());
    tileCameraTargetX = next;
    if (!immediate) {
      tileCameraAnimationMaximumVelocity = Math.max(1, Number(maxVelocity) || 28);
      if (!tileCameraFrame) {
        const step = () => {
          tileCameraFrame = 0;
          const distance = tileCameraTargetX - tileCameraX;
          tileCameraVelocityX = (tileCameraVelocityX + distance * .12) * .74;
          tileCameraVelocityX = clamp(
            tileCameraVelocityX,
            -tileCameraAnimationMaximumVelocity,
            tileCameraAnimationMaximumVelocity,
          );
          const nextX = clamp(tileCameraX + tileCameraVelocityX, 0, tileCameraMaximumX());
          const reachedTarget = (
            Math.abs(distance) < .18
            || (distance > 0 && nextX >= tileCameraTargetX)
            || (distance < 0 && nextX <= tileCameraTargetX)
          );
          tileCameraX = reachedTarget ? tileCameraTargetX : nextX;
          if (reachedTarget) tileCameraVelocityX = 0;
          if (
            reachedTarget
            || (
              Math.abs(tileCameraTargetX - tileCameraX) < .18
              && Math.abs(tileCameraVelocityX) < .18
            )
          ) {
            tileCameraX = tileCameraTargetX;
            tileCameraVelocityX = 0;
            renderTileCamera();
            syncTileViewportHorizontalAccess(tileRenderedTiles);
            settleTileViewportAfterScroll(80);
            scheduleTileExtentCleanup(160);
            return;
          }
          renderTileCamera();
          tileCameraFrame = requestAnimationFrame(step);
        };
        tileCameraFrame = requestAnimationFrame(step);
      }
      return;
    }
    stopTileCameraAnimation();
    tileCameraAnimationMaximumVelocity = 28;
    tileCameraX = next;
    tileCameraTargetX = next;
    renderTileCamera();
  };
  const tileLogicalViewportCenter = geometry => {
    if (!editorViewport || !geometry) return null;
    const origin = tileWorkspaceViewportOrigin();
    return {
      x: tileCameraX + editorViewport.clientWidth / 2 - origin.x - geometry.offsetX,
      y: editorViewport.scrollTop + editorViewport.clientHeight / 2 - origin.y - geometry.offsetY,
    };
  };
  const fullTileHorizontalOverscanExtent = (geometry = tileViewportGeometry) => {
    if (!geometry) return null;
    const reserve = Math.max(
      geometry.baseWidth,
      Number(editorViewport?.clientWidth || geometry.baseWidth)
        * tileViewportHorizontalOverscanRatio,
    );
    return {
      left: geometry.baseLeft - reserve,
      right: geometry.baseLeft + geometry.baseWidth + reserve,
    };
  };
  const syncTileViewportHorizontalAccess = (tiles = tileRenderedTiles) => {
    if (!editorViewport || !tileViewportGeometry) return;
    const currentTiles = tiles || ensureTileLayout().tiles;
    const centered = Math.abs(tileCameraX - tileHomeCameraX(tileViewportGeometry)) <= 1;
    const active = (
      tileCameraMaximumX() > 1
      && (
        tileMoveDragActive
        || document.body.classList.contains("movie-panel-interacting")
        || tileViewportHorizontalRevealRequested
        || (
          tileViewportInitialCenterComplete
          && (!tilesFitDefaultDisplayZone(currentTiles) || !centered)
        )
      )
    );
    editorViewport.classList.toggle("movie-workspace-horizontal-active", Boolean(active));
    tileCameraScrollbar?.classList.toggle("is-active", Boolean(active));
    renderTileCameraScrollbar();
  };
  const scrollTileViewportToLogicalCenter = logicalCenter => {
    if (!editorViewport || !tileViewportGeometry || !logicalCenter) return;
    const geometry = tileViewportGeometry;
    const origin = tileWorkspaceViewportOrigin();
    setTileCameraX(clamp(
      logicalCenter.x + origin.x + geometry.offsetX - editorViewport.clientWidth / 2,
      0,
      tileCameraMaximumX(geometry),
    ));
    editorViewport.scrollTop = clamp(
      logicalCenter.y + origin.y + geometry.offsetY - editorViewport.clientHeight / 2,
      0,
      Math.max(0, editorViewport.scrollHeight - editorViewport.clientHeight),
    );
  };
  const centerTileWorkspaceView = () => {
    if (!editorViewport) return;
    const layout = ensureTileLayout();
    const geometry = tileViewportGeometry || computeTileViewportGeometry(layout.tiles, layout.workspace);
    tileViewportGeometry = geometry;
    setTileCameraX(tileHomeCameraX(geometry));
    tileViewportInitialCenterComplete = true;
    syncTileViewportHorizontalAccess(layout.tiles);
  };
  const ensureTileWorkspaceCoversViewport = () => {
    if (!editorViewport || !editorLayout) return 0;
    updateMurrCutPageAnchor();
    const layout = ensureTileLayout();
    const defaults = tileLayoutDefaults || createDefaultTileLayout();
    const usesHomeGeometry = tilePanelKeys.every(key => {
      const current = layout.tiles[key];
      const home = defaults.tiles[key];
      return current && home && ["x", "y", "width", "height"].every(property =>
        Math.abs(Number(current[property]) - Number(home[property])) < 1
      );
    });
    const preserveViewport = (
      Number.isFinite(pendingTileCameraLogicalCenterX)
      || !usesHomeGeometry
      || tileCameraX > 2
      || editorViewport.scrollTop > 2
    );
    applyTileRects(layout.tiles, {preserveViewport});
    if (Number.isFinite(pendingTileCameraLogicalCenterX)) {
      const verticalCenter = tileLogicalViewportCenter(tileViewportGeometry)?.y;
      scrollTileViewportToLogicalCenter({
        x: pendingTileCameraLogicalCenterX,
        y: verticalCenter,
      });
      pendingTileCameraLogicalCenterX = null;
      syncTileViewportHorizontalAccess(layout.tiles);
    }
    if (!preserveViewport) {
      setTileCameraX(0);
      editorViewport.scrollTop = 0;
    }
    return 0;
  };
  const fitEditorViewportToWindow = () => {
    if (!editorViewport) return;
    editorViewport.scrollLeft = 0;
    ["width", "max-width", "margin-left", "margin-right"].forEach(property =>
      editorViewport.style.removeProperty(property)
    );
    const visualViewport = window.visualViewport;
    const browserTop = Math.max(0, Number(visualViewport?.offsetTop || 0));
    const browserBottom = browserTop + Number(
      visualViewport?.height || document.documentElement.clientHeight || window.innerHeight,
    );
    const viewportTop = Math.max(browserTop, editorViewport.getBoundingClientRect().top);
    editorViewport.style.setProperty(
      "height",
      `${Math.max(320, Math.floor(browserBottom - viewportTop))}px`,
      "important",
    );
    positionTileCameraScrollbar();
    ensureTileWorkspaceCoversViewport();
    scheduleTileExtentCleanup(220);
  };
  let initialTileWorkspaceCentered = false;
  const scheduleTileWorkspaceCenter = ({force = false} = {}) => {
    if (!force && initialTileWorkspaceCentered) return;
    initialTileWorkspaceCentered = true;
    requestAnimationFrame(() => requestAnimationFrame(centerTileWorkspaceView));
  };
  let editorViewportFitFrame = 0;
  const scheduleEditorViewportFit = () => {
    cancelAnimationFrame(editorViewportFitFrame);
    editorViewportFitFrame = requestAnimationFrame(() => {
      fitEditorViewportToWindow();
    });
  };
  const cloneLayoutState = () => ({mode: "columns", layouts: structuredClone(editorLayouts)});
  const panelUsesDefaultGeometry = (key, tiles = null) => {
    if (!workspacePanel(key)) return true;
    const current = tiles?.[key] || ensureTileLayout().tiles[key];
    const defaults = tileLayoutDefaults.tiles[key];
    return ["x", "y", "width", "height"].every(property =>
      Math.abs(Number(current[property]) - Number(defaults[property])) < 1
    );
  };
  const refreshPanelHomeButtons = (tiles = null) => {
    Object.entries(layoutPanels).forEach(([key, panel]) => {
      const button = panel?.querySelector(".movie-window-home");
      if (!button) return;
      button.disabled = panelUsesDefaultGeometry(key, tiles);
    });
  };
  const resetPanelGeometry = key => {
    if (!workspacePanel(key)) {
      refreshPanelHomeButtons();
      return;
    }
    const layout = ensureTileLayout();
    layout.tiles[key] = nearestAvailableTileHomeRect(
      key,
      layout.tiles,
      layout.workspace,
    );
    editorLayouts.columns = {...editorLayouts.columns, ...layout};
    applyEditorLayout(cloneLayoutState());
  };
  const persistLayout = mode => {
    localStorage.setItem(layoutModeKey, editorLayoutMode);
    if (mode && editorLayouts[mode]) localStorage.setItem(layoutStateKey(mode), JSON.stringify(editorLayouts[mode]));
  };
  const clampLayoutGeometry = (mode, geometry) => {
    const width = Math.max(760, editorLayout?.clientWidth || root.parentElement?.clientWidth || innerWidth);
    const mediaMaximum = Math.min(500, Math.max(250, width - 720));
    const inspectorMaximum = Math.min(780, Math.max(390, width - 620));
    const timelineInsetMaximum = Math.min(320, Math.floor(width * .2));
    return {
      mediaWidth: clamp(Number(geometry.mediaWidth), 180, mediaMaximum),
      inspectorWidth: clamp(Number(geometry.inspectorWidth), 220, inspectorMaximum),
      mediaHeight: clamp(Number(geometry.mediaHeight ?? 390), 150, 780),
      canvasHeight: clamp(Number(geometry.canvasHeight ?? 390), 220, 780),
      inspectorHeight: clamp(Number(geometry.inspectorHeight ?? 390), 120, 780),
      timelineHeight: clamp(Number(geometry.timelineHeight || 320), 140, 640),
      timelineInsetLeft: clamp(Number(geometry.timelineInsetLeft || 0), -timelineInsetMaximum, timelineInsetMaximum),
      timelineInsetRight: clamp(Number(geometry.timelineInsetRight || 0), -timelineInsetMaximum, timelineInsetMaximum),
    };
  };
  const applyEditorLayout = (state = cloneLayoutState(), {persist = true, refresh = true} = {}) => {
    if (!editorLayout) return;
    if (state.layouts) {
      editorLayouts = {
        columns: {...layoutDefaults.columns, ...(state.layouts.columns || {})},
        stacked: {...layoutDefaults.stacked, ...(state.layouts.stacked || {})},
      };
    }
    floatingPanels = {};
    editorLayoutMode = "columns";
    const layout = ensureTileLayout();
    editorLayout.dataset.layout = editorLayoutMode;
    editorLayout.style.setProperty("--tile-edge-size", `${tileEdgeSize}px`);
    editorLayout.style.marginLeft = "0";
    editorLayout.style.marginRight = "0";
    applyTileRects(layout.tiles);
    layoutPanels.media?.classList.remove("media-minimized");
    qa("[data-layout-mode]").forEach(button => {
      const active = button.dataset.layoutMode === editorLayoutMode;
      button.classList.toggle("active", active);
      button.setAttribute("aria-pressed", active ? "true" : "false");
    });
    if (persist) persistLayout(editorLayoutMode);
    Object.entries(layoutPanels).forEach(([key, panel]) => {
      if (!panel) return;
      panel.classList.remove("movie-panel-floating", "movie-panel-dragging", "movie-panel-snapping");
      if (!workspacePanel(key)) {
        ["position", "left", "right", "top", "bottom", "width", "z-index", "transform"].forEach(property => panel.style.removeProperty(property));
      }
    });
    refreshPanelHomeButtons();
    if (refresh) requestAnimationFrame(() => {
      applyPreviewZoom();
      updatePreviewGeometry();
      renderTimeline();
    });
  };

  const panelHomes = new Map();
  const floatingWorkspaceSpacer = document.createElement("i");
  floatingWorkspaceSpacer.className = "movie-floating-workspace-spacer";
  floatingWorkspaceSpacer.setAttribute("aria-hidden", "true");
  document.documentElement.classList.remove("movie-editor-windowing");
  document.body.classList.remove("movie-editor-windowing", "movie-panel-interacting");
  const workspaceExtent = {width: 0, height: 0};
  const pageRect = node => {
    const rect = node.getBoundingClientRect();
    return {
      left: rect.left + scrollX,
      top: rect.top + scrollY,
      right: rect.right + scrollX,
      bottom: rect.bottom + scrollY,
      width: rect.width,
      height: rect.height,
    };
  };
  let activeEditorPointerFinish = null;
  const releasePointerCaptureSafely = (node, pointerId) => {
    try {
      if (node?.hasPointerCapture?.(pointerId)) node.releasePointerCapture(pointerId);
    } catch (_) {
      // Pointer capture can already be gone after blur, cancel, or DOM movement.
    }
  };
  const capturePointerSafely = (node, pointerId) => {
    try {
      node?.setPointerCapture?.(pointerId);
      return true;
    } catch (_) {
      // A browser can reject capture while layout changes move the node.
      return false;
    }
  };
  const forceEditorPointerCleanup = () => {
    document.body.classList.remove("movie-panel-interacting");
    document.querySelectorAll(".movie-panel-moving,.movie-panel-resizing,.dragging,.resizing").forEach(node =>
      node.classList.remove("movie-panel-moving", "movie-panel-resizing", "dragging", "resizing")
    );
    previewStage?.classList.remove("snap-top", "snap-right", "snap-bottom", "snap-left");
  };
  const registerEditorPointerFinish = finish => {
    const previous = activeEditorPointerFinish;
    if (previous && previous !== finish) {
      activeEditorPointerFinish = null;
      try {
        previous();
      } finally {
        forceEditorPointerCleanup();
      }
    }
    activeEditorPointerFinish = finish;
  };
  const clearEditorPointerFinish = finish => {
    if (activeEditorPointerFinish === finish) activeEditorPointerFinish = null;
  };
  const finishActiveEditorPointer = () => {
    const finish = activeEditorPointerFinish;
    activeEditorPointerFinish = null;
    try {
      finish?.();
    } finally {
      forceEditorPointerCleanup();
    }
  };
  const floatingSideAllowance = () => Math.max(
    Number(layoutDefaults.columns.mediaWidth || 250),
    Number(layoutDefaults.stacked.mediaWidth || 250),
  );
  const clampFloatingRect = rect => {
    const viewportLeft = scrollX;
    const viewportTop = scrollY;
    const viewportWidth = document.documentElement.clientWidth;
    const viewportHeight = document.documentElement.clientHeight;
    const allowance = floatingSideAllowance();
    const width = clamp(Number(rect.width || 0), 140, viewportWidth + allowance * 2);
    const height = clamp(Number(rect.height || 0), 52, viewportHeight * 2);
    const visibleWidth = Math.min(140, width);
    const visibleHeight = Math.min(52, height);
    const sideOverflow = Math.min(allowance, Math.max(0, width - visibleWidth));
    const minimumLeft = viewportLeft - sideOverflow;
    const maximumLeft = Math.max(
      minimumLeft,
      viewportLeft + viewportWidth - width + sideOverflow,
    );
    const minimumTop = Math.max(4, viewportTop);
    const maximumTop = Math.max(minimumTop, viewportTop + viewportHeight - visibleHeight);
    const left = clamp(Number(rect.left || 0), minimumLeft, maximumLeft);
    const top = clamp(Number(rect.top || 0), minimumTop, maximumTop);
    return {
      ...rect,
      width,
      height,
      left,
      top,
      right: left + width,
      bottom: top + height,
    };
  };
  const updateFloatingWorkspaceExtent = () => {
    const rootBounds = pageRect(root);
    const viewportWidth = document.documentElement.clientWidth;
    const viewportHeight = document.documentElement.clientHeight;
    const timelineHeight = Number(layoutPanels.timeline?.getBoundingClientRect().height || 0);
    const timelineReserve = clamp(timelineHeight * .5, 120, 320);
    const floating = Object.values(layoutPanels).filter(panel => panel?.classList.contains("movie-panel-floating"));
    const floatingRects = floating.map(pageRect);
    const allowance = floatingSideAllowance();
    const tallestPanel = Math.max(timelineHeight, ...floatingRects.map(rect => rect.height), 360);
    const maximumRight = viewportWidth + allowance + 48;
    const maximumBottom = viewportHeight + tallestPanel + timelineReserve;
    let right = viewportWidth;
    let bottom = Math.max(viewportHeight, rootBounds.bottom + timelineReserve);
    floatingRects.forEach(rect => {
      right = Math.max(right, Math.min(maximumRight, rect.right + 24));
      bottom = Math.max(bottom, Math.min(maximumBottom, rect.bottom + 24));
    });
    workspaceExtent.width = Math.ceil(right);
    workspaceExtent.height = Math.ceil(bottom);
    floatingWorkspaceSpacer.style.width = "0px";
    floatingWorkspaceSpacer.style.height = bottom > viewportHeight ? `${workspaceExtent.height}px` : "0px";
    document.body.style.removeProperty("min-width");
    document.body.style.setProperty("min-height", `${workspaceExtent.height}px`, "important");
  };
  // Moving the document while a panel is being dragged changes the pointer's
  // page coordinates and used to launch floating panels across the workspace.
  // The workspace grows around the panel instead, so the normal page scrollbars
  // remain available without mutating the drag coordinate system.
  const autoScrollWorkspace = () => {};
  const flashWindowAction = element => {
    if (!element) return;
    element.classList.remove("movie-window-action-flash");
    void element.offsetWidth;
    element.classList.add("movie-window-action-flash");
    setTimeout(() => element.classList.remove("movie-window-action-flash"), 620);
  };
  let lastLayoutLockWarning = 0;
  const flashLayoutLock = () => {
    flashWindowAction(q("[data-layout-lock]"));
    if (Date.now() - lastLayoutLockWarning > 900) {
      lastLayoutLockWarning = Date.now();
      toast("Layout is locked", "warning");
    }
  };
  const rememberPanelHome = (key, panel) => {
    if (panelHomes.has(key)) return panelHomes.get(key);
    const placeholder = document.createElement("div");
    placeholder.className = "movie-panel-placeholder";
    placeholder.dataset.panelPlaceholder = key;
    placeholder.hidden = true;
    panel.parentNode.insertBefore(placeholder, panel);
    const home = {parent: panel.parentNode, placeholder, rect: pageRect(panel)};
    panelHomes.set(key, home);
    return home;
  };
  const clearPanelSnapFeedback = () => {
    Object.values(layoutPanels).filter(Boolean).forEach(panel => {
      panel.classList.remove(
        "movie-panel-snap-active",
        "movie-panel-snap-target",
        "movie-panel-home-snap",
        "movie-panel-edge-snap",
      );
    });
  };
  const persistFloatingPanels = () => localStorage.setItem(floatingPanelsKey, JSON.stringify(floatingPanels));
  let floatingGeometryFrame = 0;
  const refreshFloatingGeometry = () => {
    cancelAnimationFrame(floatingGeometryFrame);
    floatingGeometryFrame = requestAnimationFrame(() => {
      updateFloatingWorkspaceExtent();
      applyPreviewZoom();
      updatePreviewGeometry();
      renderTimeline();
    });
  };
  const setPanelFloatingState = (key, panel, rect) => {
    floatingPanels[key] = {
      left: Math.round(rect.left),
      top: Math.round(rect.top),
      width: Math.round(rect.width),
      height: Math.round(rect.height),
    };
  };
  function dockFloatingPanel(key, {persist = true} = {}) {
    const panel = layoutPanels[key];
    const home = panelHomes.get(key);
    if (!panel || !home) return;
    const homeButton = panel.querySelector(".movie-window-home");
    flashWindowAction(homeButton);
    panel.classList.remove(
      "movie-panel-floating",
      "movie-panel-moving",
      "movie-panel-resizing",
      "movie-panel-snap-active",
      "movie-panel-snap-target",
      "movie-panel-home-snap",
      "movie-panel-edge-snap",
    );
    panel.classList.add("movie-panel-docked");
    ["left", "top", "width", "height"].forEach(property => panel.style.removeProperty(property));
    home.parent.insertBefore(panel, home.placeholder.nextSibling);
    home.rect = pageRect(panel);
    home.placeholder.hidden = true;
    delete floatingPanels[key];
    panel.querySelector(".movie-window-home")?.setAttribute("disabled", "disabled");
    if (key === "toolbar") panel.hidden = true;
    if (persist) {
      persistFloatingPanels();
      persistLayout(editorLayoutMode);
    }
    cancelAnimationFrame(floatingGeometryFrame);
    floatingGeometryFrame = 0;
    updateFloatingWorkspaceExtent();
    requestAnimationFrame(() => {
      applyPreviewZoom();
      updatePreviewGeometry();
      renderTimeline();
      updateFloatingWorkspaceExtent();
    });
  }
  function floatPanel(key, geometry, {persist = true, refresh = true} = {}) {
    const panel = layoutPanels[key];
    if (!panel) return;
    const home = rememberPanelHome(key, panel);
    const fallback = pageRect(panel);
    if (!panel.classList.contains("movie-panel-floating")) home.rect = fallback;
    const minimum = panelMinimumSize(key);
    let rect = {
      left: Number(geometry?.left ?? fallback.left),
      top: Number(geometry?.top ?? fallback.top),
      width: Math.max(minimum.width, Number(geometry?.width ?? fallback.width)),
      height: Math.max(minimum.height, Number(geometry?.height ?? fallback.height)),
    };
    rect = clampFloatingRect(rect);
    home.placeholder.hidden = true;
    panel.hidden = false;
    if (panel.parentElement !== document.body) document.body.appendChild(panel);
    panel.classList.add("movie-panel-floating");
    panel.classList.remove("movie-panel-docked");
    panel.style.setProperty("left", `${rect.left}px`, "important");
    panel.style.setProperty("top", `${rect.top}px`, "important");
    panel.style.setProperty("width", `${rect.width}px`, "important");
    panel.style.setProperty("height", `${rect.height}px`, "important");
    panel.querySelector(".movie-window-home")?.removeAttribute("disabled");
    setPanelFloatingState(key, panel, rect);
    refreshPanelHomeButtons();
    if (refresh) refreshFloatingGeometry();
    if (persist) persistFloatingPanels();
  }
  function applyFloatingPanels() {
    Object.entries(layoutPanels).forEach(([key, panel]) => {
      if (!panel) return;
      rememberPanelHome(key, panel);
      if (floatingPanels[key]) floatPanel(key, floatingPanels[key], {persist: false});
      else if (key === "toolbar") panel.hidden = true;
      else if (panel.classList.contains("movie-panel-floating")) dockFloatingPanel(key, {persist: false});
    });
  }
  const panelSnapTargets = activeKey => Object.entries(layoutPanels)
    .filter(([key, panel]) => key !== activeKey && panel && !panel.hidden)
    .map(([key, panel]) => ({key, panel, rect: pageRect(panel)}));
  const nearestPanelSnap = (value, candidates, tolerance = 14) => {
    let result = {value, target: null, snapped: false};
    let best = tolerance + 1;
    candidates.forEach(candidate => {
      const distance = Math.abs(value - candidate.value);
      if (distance <= tolerance && distance < best) {
        best = distance;
        result = {value: candidate.value, target: candidate.target || null, snapped: true};
      }
    });
    return result;
  };
  const showPanelSnapFeedback = (panel, snaps, {home = false} = {}) => {
    clearPanelSnapFeedback();
    if (!snaps.some(snap => snap?.snapped)) return;
    panel.classList.add("movie-panel-snap-active", home ? "movie-panel-home-snap" : "movie-panel-edge-snap");
    snaps.forEach(snap => snap?.target?.classList.add("movie-panel-snap-target"));
  };
  const panelMinimumSize = key => ({
    editProjects: {width: 560, height: 120},
    editorHeader: {width: 560, height: 48},
    media: {width: 180, height: 150},
    preview: {width: 300, height: 220},
    inspector: {width: 220, height: 120},
    timeline: {width: 480, height: 180},
    editorHeader: {width: 560, height: 48},
    toolbar: {width: 220, height: 48},
    history: {width: 320, height: 80},
    exports: {width: 320, height: 80},
  }[key] || {width: 240, height: 86});
  const ensureWindowResizeHandles = (key, panel) => {
    if (!panel || panel.querySelector(":scope > .movie-window-resize-handle")) return;
    ["n", "ne", "e", "se", "s", "sw", "w", "nw"].forEach(direction => {
      const handle = document.createElement("i");
      handle.className = `movie-window-resize-handle ${direction}`;
      handle.dataset.windowResize = direction;
      handle.setAttribute("aria-hidden", "true");
      panel.appendChild(handle);
      handle.addEventListener("pointerdown", event => {
        if (!canEdit || event.button !== 0) return;
        if (layoutLocked) {
          event.preventDefault();
          event.stopPropagation();
          flashLayoutLock();
          return;
        }
        event.preventDefault();
        event.stopPropagation();
        remember();
        if (!panel.classList.contains("movie-panel-floating")) floatPanel(key, pageRect(panel), {persist: false, refresh: false});
        const origin = pageRect(panel);
        const startX = event.clientX;
        const startY = event.clientY;
        const minimum = panelMinimumSize(key);
        const minimumWidth = minimum.width;
        const minimumHeight = panel.classList.contains("collapsed") ? panel.offsetHeight : minimum.height;
        const pointerId = event.pointerId;
        panel.classList.add("movie-panel-resizing");
        document.body.classList.add("movie-panel-interacting");
        capturePointerSafely(handle, pointerId);
        let finished = false;
        let previewRefreshFrame = 0;
        const move = next => {
          const dx = next.clientX - startX;
          const dy = next.clientY - startY;
          let left = origin.left;
          let top = origin.top;
          let width = origin.width;
          let height = origin.height;
          if (direction.includes("e")) width = Math.max(minimumWidth, origin.width + dx);
          if (direction.includes("s")) height = Math.max(minimumHeight, origin.height + dy);
          if (direction.includes("w")) {
            width = Math.max(minimumWidth, origin.width - dx);
            left = origin.right - width;
          }
          if (direction.includes("n")) {
            height = Math.max(minimumHeight, origin.height - dy);
            top = origin.bottom - height;
          }
          const targets = panelSnapTargets(key);
          const horizontalEdge = direction.includes("w") ? left : left + width;
          const verticalEdge = direction.includes("n") ? top : top + height;
          const horizontal = nearestPanelSnap(horizontalEdge, [
            {value: 4},
            ...targets.flatMap(target => [
              {value: target.rect.left, target: target.panel},
              {value: target.rect.right, target: target.panel},
            ]),
          ]);
          const vertical = nearestPanelSnap(verticalEdge, [
            {value: 4},
            ...targets.flatMap(target => [
              {value: target.rect.top, target: target.panel},
              {value: target.rect.bottom, target: target.panel},
            ]),
          ]);
          if ((direction.includes("w") || direction.includes("e")) && horizontal.snapped) {
            if (direction.includes("w")) {
              width += left - horizontal.value;
              left = horizontal.value;
            } else {
              width = horizontal.value - left;
            }
          }
          if ((direction.includes("n") || direction.includes("s")) && vertical.snapped) {
            if (direction.includes("n")) {
              height += top - vertical.value;
              top = vertical.value;
            } else {
              height = vertical.value - top;
            }
          }
          width = Math.max(minimumWidth, width);
          height = Math.max(minimumHeight, height);
          const bounded = clampFloatingRect({left, top, width, height});
          left = bounded.left;
          top = bounded.top;
          panel.style.setProperty("left", `${left}px`, "important");
          panel.style.setProperty("top", `${top}px`, "important");
          panel.style.setProperty("width", `${width}px`, "important");
          panel.style.setProperty("height", `${height}px`, "important");
          setPanelFloatingState(key, panel, {left, top, width, height});
          showPanelSnapFeedback(panel, [horizontal, vertical]);
          if (key === "preview" && !previewRefreshFrame) {
            previewRefreshFrame = requestAnimationFrame(() => {
              previewRefreshFrame = 0;
              applyPreviewZoom();
              updatePreviewGeometry();
            });
          }
        };
        const finish = () => {
          if (finished) return;
          finished = true;
          clearEditorPointerFinish(finish);
          if (previewRefreshFrame) cancelAnimationFrame(previewRefreshFrame);
          handle.onpointermove = null;
          handle.onpointerup = null;
          handle.onpointercancel = null;
          handle.onlostpointercapture = null;
          releasePointerCaptureSafely(handle, pointerId);
          panel.classList.remove("movie-panel-resizing");
          document.body.classList.remove("movie-panel-interacting");
          clearPanelSnapFeedback();
          updateFloatingWorkspaceExtent();
          applyPreviewZoom();
          updatePreviewGeometry();
          renderTimeline();
          persistFloatingPanels();
          historyRedo = [];
          updateHistoryButtons();
        };
        handle.onpointermove = move;
        handle.onpointerup = finish;
        handle.onpointercancel = finish;
        handle.onlostpointercapture = finish;
        registerEditorPointerFinish(finish);
      });
    });
  };
  const ensurePanelChrome = (key, panel) => {
    if (!panel) return;
    panel.classList.add("movie-collapsible");
    const handle = panel.matches(`[data-panel-drag="${key}"]`)
      ? panel
      : panel.querySelector(`[data-panel-drag="${key}"]`) || panel.firstElementChild || panel;
    if (!handle.dataset.panelDrag) handle.dataset.panelDrag = key;
    let actions = panel.querySelector(":scope > .movie-window-actions")
      || handle.querySelector(":scope > .movie-window-actions");
    if (!actions) {
      actions = document.createElement("span");
      actions.className = "movie-window-actions";
      handle.appendChild(actions);
    }
    if (key === "preview" && actions.parentElement !== panel) {
      panel.appendChild(actions);
    }
    let home = actions.querySelector(".movie-window-home");
    if (!home) {
      home = document.createElement("button");
      home.type = "button";
      home.className = "icon-button secondary movie-window-home";
      home.dataset.businessCommand = "home";
      home.title = "Return panel to its default position";
      home.setAttribute("aria-label", home.title);
      home.disabled = !panel.classList.contains("movie-panel-floating");
      home.addEventListener("click", event => {
        event.stopPropagation();
        remember();
        if (key === "toolbar") {
          panel.hidden = true;
          panel.classList.remove("movie-panel-floating", "movie-panel-moving", "movie-panel-resizing");
          ["left", "top", "width", "height"].forEach(property => panel.style.removeProperty(property));
          delete floatingPanels.toolbar;
          persistFloatingPanels();
        } else {
          resetPanelGeometry(key);
          requestAnimationFrame(() => focusTileOnHomePosition(key));
        }
        historyRedo = [];
        updateHistoryButtons();
      });
      actions.appendChild(home);
    }
    if (key === "toolbar" && !actions.querySelector("[data-toolbar-height-step]")) {
      [-1, 1].forEach(direction => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "icon-button secondary movie-toolbar-height-step";
        button.dataset.toolbarHeightStep = String(direction);
        button.dataset.businessCommand = direction < 0 ? "previous" : "next";
        button.title = direction < 0 ? "Make toolbar shorter" : "Make toolbar taller";
        button.setAttribute("aria-label", button.title);
        button.addEventListener("click", event => {
          event.stopPropagation();
          if (layoutLocked) return flashLayoutLock();
          remember();
          const rect = pageRect(panel);
          const height = Math.max(72, rect.height + direction * 44);
          if (!panel.classList.contains("movie-panel-floating")) {
            floatPanel(key, {...rect, height}, {persist: false});
          } else {
            panel.style.setProperty("height", `${height}px`, "important");
            setPanelFloatingState(key, panel, {...rect, height});
          }
          persistFloatingPanels();
          updateFloatingWorkspaceExtent();
          historyRedo = [];
          updateHistoryButtons();
        });
        actions.appendChild(button);
      });
    }
    let toggle = panel.querySelector(":scope > [data-panel-drag] [data-panel-toggle]");
    if (!toggle) {
      toggle = document.createElement("button");
      toggle.type = "button";
      toggle.className = "icon-button secondary movie-panel-toggle";
      toggle.dataset.panelToggle = "";
      toggle.dataset.businessCommand = "remove";
      toggle.title = `Collapse ${key}`;
      toggle.setAttribute("aria-label", toggle.title);
      actions.appendChild(toggle);
    } else if (toggle.parentElement !== actions) {
      actions.appendChild(toggle);
    }
    if (!workspacePanel(key)) ensureWindowResizeHandles(key, panel);
  };
  const initializeFloatingToolbar = () => {
    const toolbar = layoutPanels.toolbar?.querySelector("[data-floating-toolbar]");
    if (!toolbar || toolbar.childElementCount) return;
    const tools = [
      ["[data-auto-cut]", "auto", "Auto rough cut"],
      ["[data-editor-undo]", "undo", "Undo"],
      ["[data-editor-redo]", "redo", "Redo"],
      ['[data-keyframe-step="-1"]', "previous", "Previous keyframe"],
      ['[data-keyframe-step="1"]', "next", "Next keyframe"],
      ["[data-preview-stop]", "stop", "Stop"],
      ['[data-preview-step="-1"]', "previous", "Previous frame"],
      ['[data-preview-step="1"]', "next", "Next frame"],
      ['[data-add-track="VIDEO"]', "add", "Add video track"],
      ['[data-add-track="AUDIO"]', "add", "Add audio track"],
      ["[data-delete-track]", "delete", "Delete selected track"],
      ['[data-clip-nudge="-1"]', "previous", "Move selected left"],
      ["[data-timeline-split]", "edit", "Split at playhead"],
      ['[data-clip-nudge="1"]', "next", "Move selected right"],
      ["[data-timeline-snap]", "attach", "Toggle snapping"],
      ["[data-clip-copy]", "copy", "Copy selected clips"],
      ["[data-clip-paste]", "paste", "Paste clips"],
      ["[data-clip-join]", "link", "Join clips"],
      ["[data-clip-render-toolbar]", "export", "Render selected clip"],
    ];
    tools.forEach(([selector, command, title]) => {
      if (!q(selector)) return;
      const button = document.createElement("button");
      button.type = "button";
      button.className = "icon-button secondary";
      button.dataset.toolProxy = selector;
      button.dataset.businessCommand = command;
      button.title = title;
      button.setAttribute("aria-label", title);
      button.addEventListener("click", () => q(selector)?.click());
      toolbar.appendChild(button);
    });
  };
  const initializeProjectFullscreen = () => {
    const host = q(".movie-actions");
    if (!host || host.querySelector("[data-project-fullscreen]")) return;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "theme-toggle icon-button movie-project-fullscreen";
    button.dataset.projectFullscreen = "";
    button.dataset.businessCommand = "fullscreen";
    button.title = "Open the editor in browser full screen";
    button.setAttribute("aria-label", button.title);
    button.addEventListener("click", async event => {
      event.stopPropagation();
      try {
        if (document.fullscreenElement) await document.exitFullscreen();
        else await document.documentElement.requestFullscreen();
      } catch (error) {
        toast(error.message || "Full screen is unavailable", "error");
      }
    });
    host.insertBefore(button, host.firstChild);
    const home = document.createElement("button");
    home.type = "button";
    home.className = "icon-button secondary";
    home.dataset.editorHome = "";
    home.dataset.businessCommand = "home";
    home.title = "Reset the editor workspace";
    home.setAttribute("aria-label", home.title);
    home.addEventListener("click", () => q("[data-layout-reset]")?.click());
    host.insertBefore(home, button.nextSibling);
    document.addEventListener("fullscreenchange", () => {
      button.classList.toggle("active", Boolean(document.fullscreenElement));
      button.title = document.fullscreenElement ? "Exit full screen" : "Open the editor in browser full screen";
      button.setAttribute("aria-label", button.title);
    });
    window.refreshLexamoraIcons?.(host);
  };
  const applyLayoutLockState = () => {
    document.body.classList.toggle("movie-layout-locked", layoutLocked);
    root.classList.toggle("movie-layout-locked", layoutLocked);
    const button = q("[data-layout-lock]");
    if (!button) return;
    button.dataset.businessCommand = layoutLocked ? "lock" : "unlock";
    button.title = layoutLocked ? "Unlock layout editing" : "Lock layout editing";
    button.setAttribute("aria-label", button.title);
    button.setAttribute("aria-pressed", layoutLocked ? "true" : "false");
    window.refreshLexamoraIcons?.(button);
  };
  const openFloatingToolbar = () => {
    const panel = layoutPanels.toolbar;
    const previewPanel = layoutPanels.preview;
    if (!panel || !previewPanel) return;
    const anchor = pageRect(previewPanel);
    const width = Math.min(820, Math.max(480, anchor.width * .8));
    floatPanel("toolbar", {
      left: Math.max(4, anchor.left + 14),
      top: Math.max(4, anchor.top + 46),
      width,
      height: 96,
    });
    ensurePanelChrome("toolbar", panel);
    window.refreshLexamoraIcons?.(panel);
  };
  let canvasPopout = null;
  let canvasPopoutTimer = null;
  const closeCanvasPopoutSync = () => {
    if (canvasPopoutTimer) clearInterval(canvasPopoutTimer);
    canvasPopoutTimer = null;
    canvasPopout = null;
  };
  const syncCanvasPopout = () => {
    if (!canvasPopout || canvasPopout.closed) {
      closeCanvasPopoutSync();
      return;
    }
    const host = canvasPopout.document.getElementById("canvas-host");
    if (!host || !previewStage) return;
    const clone = previewStage.cloneNode(true);
    clone.removeAttribute("style");
    clone.querySelectorAll(".movie-preview-source-outline,.movie-preview-empty,.movie-canvas-snap-edge").forEach(node => node.remove());
    const originalVideos = [...previewStage.querySelectorAll("video")];
    const clonedVideos = [...clone.querySelectorAll("video")];
    clonedVideos.forEach((video, index) => {
      const original = originalVideos[index];
      if (!original) return;
      video.src = original.currentSrc || original.src;
      video.currentTime = original.currentTime || 0;
      video.muted = original.muted;
      if (!original.paused) video.play().catch(() => {});
    });
    host.replaceChildren(clone);
  };
  const openCanvasPopout = () => {
    if (canvasPopout && !canvasPopout.closed) {
      canvasPopout.focus();
      return;
    }
    canvasPopout = window.open("", `lexamora-canvas-${root.dataset.timelineId || "preview"}`, "popup=yes,width=960,height=900,resizable=yes,scrollbars=yes");
    if (!canvasPopout) {
      toast("Allow pop-ups to open Canvas on another screen", "error");
      return;
    }
    const {width, height} = outputDimensions();
    canvasPopout.document.open();
    canvasPopout.document.write(`<!doctype html><html><head><title>Lexamora Canvas</title><style>
      *{box-sizing:border-box}html,body{margin:0;min-width:100%;min-height:100%;background:#07111b;color:#fff;font-family:Arial,sans-serif;overflow:auto}
      body{display:grid;place-items:center;padding:18px}
      #canvas-host{position:relative;width:min(96vw,calc(96vh * ${width} / ${height}));aspect-ratio:${width}/${height};overflow:hidden;background:#000;border:2px solid #2fc5ff}
      .movie-preview-stage{position:relative!important;width:100%!important;height:100%!important;min-width:0!important;min-height:0!important;max-width:none!important;max-height:none!important;transform:none!important;overflow:hidden!important;background:#000!important}
      .movie-preview-layers{position:absolute;inset:0;overflow:hidden}
      .movie-preview-layer,[data-movie-preview]{position:absolute;max-width:none!important;max-height:none!important;transform-origin:center center}
    </style></head><body><main id="canvas-host"></main></body></html>`);
    canvasPopout.document.close();
    canvasPopout.addEventListener("beforeunload", closeCanvasPopoutSync, {once: true});
    syncCanvasPopout();
    canvasPopoutTimer = setInterval(syncCanvasPopout, 180);
  };
  const initializeCanvasControls = () => {
    const controls = q(".movie-preview-controls");
    const frameControls = q("[data-canvas-frame-controls]");
    const frameSize = q(".movie-frame-size-stepper");
    const fps = q("[data-movie-fps]");
    if (!controls) return;
    if (fps && frameSize) fps.insertAdjacentElement("afterend", frameSize);
    // Frame controls belong to the Canvas window. Keeping them beside the
    // stage prevents them from collapsing into the global preview toolbar.
    const actionHost = layoutPanels.preview?.querySelector(":scope > .movie-window-actions") || controls;
    let toolbarButton = actionHost.querySelector("[data-floating-toolbar-open]");
    if (!toolbarButton) {
      toolbarButton = document.createElement("button");
      toolbarButton.type = "button";
      toolbarButton.className = "icon-button secondary";
      toolbarButton.dataset.floatingToolbarOpen = "";
      toolbarButton.dataset.businessCommand = "settings";
      toolbarButton.title = "Open floating toolbar";
      toolbarButton.setAttribute("aria-label", toolbarButton.title);
      toolbarButton.addEventListener("click", openFloatingToolbar);
      actionHost.appendChild(toolbarButton);
    }
    let popoutButton = actionHost.querySelector("[data-canvas-popout]");
    if (!popoutButton) {
      popoutButton = document.createElement("button");
      popoutButton.type = "button";
      popoutButton.className = "icon-button secondary";
      popoutButton.dataset.canvasPopout = "";
      popoutButton.dataset.businessCommand = "open";
      popoutButton.title = "Open Canvas in a separate window";
      popoutButton.setAttribute("aria-label", popoutButton.title);
      popoutButton.addEventListener("click", openCanvasPopout);
      actionHost.appendChild(popoutButton);
    }
  };
  const rectanglesOverlap = (left, right, tolerance = .5) =>
    left.x < right.x + right.width - tolerance
    && left.x + left.width > right.x + tolerance
    && left.y < right.y + right.height - tolerance
    && left.y + left.height > right.y + tolerance;
  const tileLayoutHasOverlap = (tiles, activeKey = null) => {
    const keys = activeKey ? [activeKey] : tilePanelKeys;
    return keys.some(key => tilePanelKeys.some(otherKey =>
      key !== otherKey && rectanglesOverlap(tiles[key], tiles[otherKey])
    ));
  };
  const cloneTiles = tiles => Object.fromEntries(tilePanelKeys.map(key => [key, tileRect(tiles[key])]));
  const nearestAvailableTileHomeRect = (key, tiles, workspace) => {
    const home = tileRect(tileLayoutDefaults.tiles[key]);
    const current = tileRect(tiles[key]);
    const maximumX = Math.max(tileWorkspaceInset, workspace.width - home.width - tileWorkspaceInset);
    const maximumY = Math.max(tileWorkspaceInset, workspace.height - home.height - tileWorkspaceInset);
    const xCandidates = new Set();
    const yCandidates = new Set();
    const addX = value => xCandidates.add(clamp(Math.round(value), tileWorkspaceInset, maximumX));
    const addY = value => yCandidates.add(clamp(Math.round(value), tileWorkspaceInset, maximumY));
    [home.x, current.x, tileWorkspaceInset, maximumX].forEach(addX);
    [home.y, current.y, tileWorkspaceInset, maximumY].forEach(addY);
    tilePanelKeys.filter(otherKey => otherKey !== key).forEach(otherKey => {
      const other = tiles[otherKey];
      addX(Math.floor(other.x - home.width));
      addX(Math.ceil(other.x + other.width));
      addY(Math.floor(other.y - home.height));
      addY(Math.ceil(other.y + other.height));
    });
    let nearest = null;
    xCandidates.forEach(x => yCandidates.forEach(y => {
      const rect = {...home, x, y};
      const occupied = tilePanelKeys.some(
        otherKey => otherKey !== key && rectanglesOverlap(rect, tiles[otherKey]),
      );
      if (occupied) return;
      const dx = x - home.x;
      const dy = y - home.y;
      const score = dx * dx + dy * dy;
      const axialDistance = Math.abs(dx) + Math.abs(dy);
      if (
        !nearest
        || score < nearest.score
        || (score === nearest.score && axialDistance < nearest.axialDistance)
      ) {
        nearest = {rect, score, axialDistance};
      }
    }));
    return nearest?.rect || current;
  };
  const syncTileSelectionClasses = () => {
    tilePanelKeys.forEach(key => {
      const panel = layoutPanels[key];
      if (!panel) return;
      const selected = selectedTileKeys.has(key);
      panel.classList.toggle("movie-tile-selected", selected);
      panel.setAttribute("aria-selected", selected ? "true" : "false");
    });
  };
  const setSelectedTileKeys = keys => {
    selectedTileKeys = new Set(keys.filter(key => workspacePanel(key)));
    syncTileSelectionClasses();
  };
  const clearTileSelection = () => {
    setSelectedTileKeys([]);
  };
  const temporaryTileSelectionTimers = new WeakMap();
  const setTemporaryTileSelection = (panel, active, linger = 0) => {
    if (!panel) return;
    const timer = temporaryTileSelectionTimers.get(panel);
    if (timer) window.clearTimeout(timer);
    temporaryTileSelectionTimers.delete(panel);
    if (active) {
      panel.classList.add("movie-tile-pressed");
      return;
    }
    if (linger > 0) {
      temporaryTileSelectionTimers.set(panel, window.setTimeout(() => {
        panel.classList.remove("movie-tile-pressed");
        temporaryTileSelectionTimers.delete(panel);
      }, linger));
      return;
    }
    panel.classList.remove("movie-tile-pressed");
  };
  const tileDisplayScale = () => {
    const widthRatio = Number(window.outerWidth) > 0 && Number(window.innerWidth) > 0
      ? window.outerWidth / window.innerWidth
      : 1;
    const visualScale = Number(window.visualViewport?.scale || 1);
    return Math.min(
      Number.isFinite(widthRatio) ? widthRatio : 1,
      Number.isFinite(visualScale) ? visualScale : 1,
    );
  };
  const syncTileWorkspaceBoundary = (geometry, workspace) => {
    if (!editorLayout || !geometry || !workspace) return;
    editorLayout.classList.toggle(
      "movie-workspace-boundary-visible",
      tileDisplayScale() <= .55,
    );
    editorLayout.style.setProperty(
      "--murrcut-workspace-boundary-left",
      `${geometry.offsetX}px`,
    );
    editorLayout.style.setProperty(
      "--murrcut-workspace-boundary-top",
      `${geometry.offsetY}px`,
    );
    editorLayout.style.setProperty(
      "--murrcut-workspace-boundary-width",
      `${workspace.width}px`,
    );
    editorLayout.style.setProperty(
      "--murrcut-workspace-boundary-height",
      `${workspace.height}px`,
    );
  };
  const tileGroupBounds = (tiles, keys) => {
    const rects = keys.map(key => tiles[key]).filter(Boolean);
    if (!rects.length) return null;
    const left = Math.min(...rects.map(rect => rect.x));
    const top = Math.min(...rects.map(rect => rect.y));
    const right = Math.max(...rects.map(rect => rect.x + rect.width));
    const bottom = Math.max(...rects.map(rect => rect.y + rect.height));
    return {x: left, y: top, width: right - left, height: bottom - top, right, bottom};
  };
  const tileLayoutHasSelectionOverlap = (tiles, keys) => {
    const selected = new Set(keys);
    return keys.some(key => tilePanelKeys.some(otherKey =>
      key !== otherKey && !selected.has(otherKey) && rectanglesOverlap(tiles[key], tiles[otherKey])
    ));
  };
  const applyTileRects = (tiles, {preserveViewport = false, scheduleCleanup = true} = {}) => {
    const layout = ensureTileLayout();
    tileRenderedTiles = cloneTiles(tiles);
    const previousLogicalCenter = tileLogicalViewportCenter(tileViewportGeometry);
    const requiredGeometry = computeTileViewportGeometry(tiles, layout.workspace);
    const requiredExtent = requiredGeometry.requiredExtent;
    const overscanExtent = fullTileHorizontalOverscanExtent(requiredGeometry);
    const baseRight = requiredGeometry.baseLeft + requiredGeometry.baseWidth;
    const previousExtent = tileAllocatedExtent || {
      ...requiredExtent,
      left: Math.min(requiredExtent.left, overscanExtent.left),
      right: Math.max(requiredExtent.right, overscanExtent.right),
    };
    const symmetricHorizontalReserve = Math.max(
      requiredGeometry.baseLeft - overscanExtent.left,
      requiredGeometry.baseLeft - requiredExtent.left,
      requiredExtent.right - baseRight,
      requiredGeometry.baseLeft - previousExtent.left,
      previousExtent.right - baseRight,
    );
    tileAllocatedExtent = {
      left: requiredGeometry.baseLeft - symmetricHorizontalReserve,
      right: baseRight + symmetricHorizontalReserve,
      bottom: Math.max(previousExtent.bottom, requiredExtent.bottom),
    };
    const geometry = computeTileViewportGeometry(tiles, layout.workspace, tileAllocatedExtent);
    tileAllocatedExtent = geometry.extent;
    tileViewportGeometry = geometry;
    syncTileWorkspaceBoundary(geometry, layout.workspace);
    if (editorWorld) {
      editorWorld.style.setProperty("--murrcut-world-width", `${geometry.width}px`);
      editorWorld.style.setProperty("--murrcut-world-anchor-x", `${geometry.baseLeft + geometry.offsetX}px`);
      editorWorld.style.setProperty("--murrcut-base-width", `${geometry.baseWidth}px`);
    }
    editorLayout.style.setProperty("--tile-workspace-width", `${geometry.width}px`);
    editorLayout.style.setProperty("--tile-workspace-height", `${geometry.height}px`);
    editorLayout.style.setProperty("width", `${geometry.width}px`, "important");
    editorLayout.style.setProperty("height", `${geometry.height}px`, "important");
    tilePanelKeys.forEach(key => {
      const panel = layoutPanels[key];
      const rect = tiles[key];
      if (!panel || !rect) return;
      panel.style.setProperty("left", `${rect.x + geometry.offsetX}px`, "important");
      panel.style.setProperty("top", `${rect.y + geometry.offsetY}px`, "important");
      panel.style.setProperty("width", `${rect.width}px`, "important");
      panel.style.setProperty("height", `${rect.height}px`, "important");
      panel.style.removeProperty("right");
      panel.style.removeProperty("bottom");
      panel.dataset.tileX = String(Math.round(rect.x));
      panel.dataset.tileY = String(Math.round(rect.y));
    });
    syncTileSelectionClasses();
    refreshPanelHomeButtons(tiles);
    const timelineShell = q("[data-timeline-shell]");
    if (timelineShell) timelineShell.style.setProperty("height", `${Math.max(100, tiles.timeline.height - 46)}px`, "important");
    if (preserveViewport) scrollTileViewportToLogicalCenter(previousLogicalCenter);
    else {
      tileCameraX = clamp(tileCameraX, 0, tileCameraMaximumX());
      tileCameraTargetX = clamp(tileCameraTargetX, 0, tileCameraMaximumX());
      renderTileCamera();
    }
    syncTileViewportHorizontalAccess(tiles);
    if (scheduleCleanup && document.body.classList.contains("movie-panel-interacting")) {
      scheduleTileExtentCleanup(90);
    }
  };
  const tileLogicalViewportBounds = (geometry, cameraX = tileCameraX) => {
    if (!editorViewport || !geometry) return null;
    const origin = tileWorkspaceViewportOrigin();
    const left = cameraX - origin.x - geometry.offsetX;
    const top = editorViewport.scrollTop - origin.y - geometry.offsetY;
    return {
      left,
      right: left + editorViewport.clientWidth,
      top,
      bottom: top + editorViewport.clientHeight,
    };
  };
  const tilesFitHorizontalCameraView = (tiles, geometry, cameraX) => {
    const visible = tileLogicalViewportBounds(geometry, cameraX);
    if (!visible || !tiles) return false;
    const bounds = tileBounds(tiles);
    return (
      bounds.left >= visible.left - 1
      && bounds.right <= visible.right + 1
    );
  };
  const balancedCameraTargetForTiles = (
    tiles,
    geometry,
    {requireCurrentVisibility = true} = {},
  ) => {
    if (!tiles || !geometry) return null;
    const bounds = tileBounds(tiles);
    const origin = tileWorkspaceViewportOrigin();
    const outerCenterX = (bounds.left + bounds.right) / 2;
    const centeredCameraX = clamp(
      origin.x + geometry.offsetX + outerCenterX - editorViewport.clientWidth / 2,
      0,
      tileCameraMaximumX(geometry),
    );
    if (
      (
        requireCurrentVisibility
        && !tilesFitHorizontalCameraView(tiles, geometry, tileCameraX)
      )
      || !tilesFitHorizontalCameraView(tiles, geometry, centeredCameraX)
    ) return null;
    return centeredCameraX;
  };
  const cleanupTileWorkspaceExtent = () => {
    tileExtentCleanupTimer = 0;
    if (!editorViewport || !tileViewportGeometry || !tileAllocatedExtent) return;
    const layout = ensureTileLayout();
    const renderedTiles = tileRenderedTiles || layout.tiles;
    const required = computeTileViewportGeometry(renderedTiles, layout.workspace).requiredExtent;
    const visible = tileLogicalViewportBounds(tileViewportGeometry);
    if (!visible) return;
    const next = {...tileAllocatedExtent};
    const overscanExtent = fullTileHorizontalOverscanExtent(tileViewportGeometry);
    const baseRight = tileViewportGeometry.baseLeft + tileViewportGeometry.baseWidth;
    const symmetricHorizontalReserve = Math.max(
      tileViewportGeometry.baseLeft - overscanExtent.left,
      tileViewportGeometry.baseLeft - required.left,
      required.right - baseRight,
    );
    const safeLeft = tileViewportGeometry.baseLeft - symmetricHorizontalReserve;
    const safeRight = baseRight + symmetricHorizontalReserve;
    const safeBottom = Math.max(
      required.bottom,
      visible.top + tileViewportMinimumReveal + tileExtentCleanupMargin,
    );
    const verticalStep = Math.max(48, editorViewport.clientHeight * .12);
    next.left = safeLeft;
    next.right = safeRight;
    if (safeBottom < next.bottom) next.bottom = Math.max(safeBottom, next.bottom - verticalStep);
    const changed = ["left", "right", "bottom"].some(
      side => Math.abs(Number(next[side]) - Number(tileAllocatedExtent[side])) >= 1
    );
    if (!changed) return;
    tileAllocatedExtent = next;
    applyTileRects(renderedTiles, {preserveViewport: true, scheduleCleanup: false});
    const hasMore = (
      next.left < safeLeft - 1
      || next.right > safeRight + 1
      || next.bottom > safeBottom + 1
    );
    if (hasMore) {
      scheduleTileExtentCleanup(
        document.body.classList.contains("movie-panel-interacting") ? 90 : 140
      );
    }
  };
  const scheduleTileExtentCleanup = (delay = 160) => {
    if (tileExtentCleanupTimer) return;
    const suppressionDelay = Math.max(0, tileExtentCleanupSuppressedUntil - Date.now() + 10);
    tileExtentCleanupTimer = setTimeout(() => {
      tileExtentCleanupTimer = 0;
      if (
        tileMoveDragActive
        || tileHomeCenterFrame
        || tileCameraFrame
        || document.body.classList.contains("movie-panel-interacting")
        || Date.now() < tileExtentCleanupSuppressedUntil
      ) {
        scheduleTileExtentCleanup(120);
        return;
      }
      cleanupTileWorkspaceExtent();
    }, Math.max(0, delay, suppressionDelay));
  };
  const hideTileCenterAxis = () => {
    if (tileCenterAxisTimer) clearTimeout(tileCenterAxisTimer);
    tileCenterAxisTimer = 0;
    const axis = editorLayout?.querySelector(":scope > .movie-tile-center-axis");
    if (axis) {
      axis.classList.remove("active");
      axis.hidden = true;
    }
  };
  const flashTileCenterAxis = logicalX => {
    if (!editorLayout || !tileViewportGeometry) return;
    let axis = editorLayout.querySelector(":scope > .movie-tile-center-axis");
    if (!axis) {
      axis = document.createElement("i");
      axis.className = "movie-tile-center-axis";
      axis.setAttribute("aria-hidden", "true");
      editorLayout.appendChild(axis);
    }
    if (tileCenterAxisTimer) clearTimeout(tileCenterAxisTimer);
    axis.style.left = `${logicalX + tileViewportGeometry.offsetX}px`;
    axis.hidden = false;
    axis.classList.remove("active");
    void axis.offsetWidth;
    axis.classList.add("active");
    tileCenterAxisTimer = window.setTimeout(hideTileCenterAxis, 520);
  };
  const cancelTileHomeCenter = () => {
    if (tileHomeCenterFrame) cancelAnimationFrame(tileHomeCenterFrame);
    tileHomeCenterFrame = 0;
    stopTileCameraAnimation();
    hideTileCenterAxis();
  };
  const settleTileViewportOnHomeAxis = ({
    tiles = null,
    workspace = null,
    fallbackBottom = null,
    force = false,
    toleranceRatio = .05,
  } = {}) => {
    if (!editorViewport || !tileViewportGeometry) return false;
    const layout = ensureTileLayout();
    const finalTiles = tiles || tileRenderedTiles || layout.tiles;
    const finalWorkspace = workspace || layout.workspace;
    if (!tilesFitDefaultDisplayZone(finalTiles)) {
      cancelTileHomeCenter();
      const cleanupDelay = Math.max(
        180,
        tileExtentCleanupSuppressedUntil - Date.now() + 10,
      );
      window.setTimeout(() => scheduleTileExtentCleanup(0), cleanupDelay);
      return false;
    }
    const homeAxis = tileViewportGeometry.baseLeft + tileViewportGeometry.baseWidth / 2;
    const horizontalOverflow = tileCameraMaximumX();
    const homeCameraX = tileHomeCameraX(tileViewportGeometry);
    const axisDistance = Math.abs(tileCameraX - homeCameraX);
    if (horizontalOverflow <= 1 || axisDistance <= 1) {
      cancelTileHomeCenter();
      if (horizontalOverflow > 1) {
        const cleanupDelay = Math.max(
          180,
          tileExtentCleanupSuppressedUntil - Date.now() + 10,
        );
        window.setTimeout(() => scheduleTileExtentCleanup(0), cleanupDelay);
      }
      return false;
    }
    const focusTolerance = Math.max(1, editorViewport.clientWidth * toleranceRatio);
    const scrollbarCenterDistance = axisDistance;
    const scrollbarTolerance = Math.max(1, horizontalOverflow * toleranceRatio);
    const focusWithinTolerance = axisDistance <= focusTolerance;
    const scrollbarWithinTolerance = scrollbarCenterDistance <= scrollbarTolerance;
    if (
      !force && !focusWithinTolerance && !scrollbarWithinTolerance
    ) {
      const cleanupDelay = Math.max(
        180,
        tileExtentCleanupSuppressedUntil - Date.now() + 10,
      );
      window.setTimeout(() => scheduleTileExtentCleanup(0), cleanupDelay);
      return false;
    }
    cancelTileHomeCenter();
    if (tileExtentCleanupTimer) clearTimeout(tileExtentCleanupTimer);
    tileExtentCleanupTimer = 0;
    tileExtentCleanupSuppressedUntil = Date.now() + 700;
    const startLeft = tileCameraX;
    const targetLeft = homeCameraX;
    const startedAt = performance.now();
    const duration = 240;
    const step = now => {
      const progress = clamp((now - startedAt) / duration, 0, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setTileCameraX(startLeft + (targetLeft - startLeft) * eased);
      if (progress < 1) {
        tileHomeCenterFrame = requestAnimationFrame(step);
        return;
      }
      tileHomeCenterFrame = 0;
      const homeGeometry = computeTileViewportGeometry(finalTiles, finalWorkspace);
      const verticalCenter = tileLogicalViewportCenter(tileViewportGeometry)?.y;
      tileAllocatedExtent = {
        ...(tileAllocatedExtent || homeGeometry.requiredExtent),
        left: homeGeometry.requiredExtent.left,
        right: homeGeometry.requiredExtent.right,
        bottom: Math.max(
          homeGeometry.requiredExtent.bottom,
          Number(tileAllocatedExtent?.bottom || fallbackBottom || homeGeometry.requiredExtent.bottom),
        ),
      };
      applyTileRects(finalTiles, {preserveViewport: true, scheduleCleanup: false});
      scrollTileViewportToLogicalCenter({x: homeAxis, y: verticalCenter});
      setTileCameraX(tileHomeCameraX(tileViewportGeometry));
      syncTileViewportHorizontalAccess(finalTiles);
      flashTileCenterAxis(homeAxis);
      tileExtentCleanupSuppressedUntil = Date.now() + 120;
      window.setTimeout(() => scheduleTileExtentCleanup(0), 130);
    };
    tileHomeCenterFrame = requestAnimationFrame(step);
    return true;
  };
  const focusTileOnHomePosition = key => {
    if (!editorViewport || !tileViewportGeometry || !workspacePanel(key)) return false;
    const layout = ensureTileLayout();
    const rect = layout.tiles[key];
    if (!rect) return false;
    cancelTileHomeCenter();
    if (tileExtentCleanupTimer) clearTimeout(tileExtentCleanupTimer);
    tileExtentCleanupTimer = 0;
    tileExtentCleanupSuppressedUntil = Date.now() + 700;
    const geometry = tileViewportGeometry;
    const origin = tileWorkspaceViewportOrigin();
    const homeAxis = geometry.baseLeft + geometry.baseWidth / 2;
    const startLeft = tileCameraX;
    const targetLeft = tileHomeCameraX(geometry);
    const startTop = editorViewport.scrollTop;
    const targetTop = clamp(
      rect.y + rect.height / 2 + origin.y + geometry.offsetY - editorViewport.clientHeight / 2,
      0,
      Math.max(0, editorViewport.scrollHeight - editorViewport.clientHeight),
    );
    const startedAt = performance.now();
    const duration = 240;
    revealTileViewportScrollbars();
    const step = now => {
      const progress = clamp((now - startedAt) / duration, 0, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setTileCameraX(startLeft + (targetLeft - startLeft) * eased);
      editorViewport.scrollTop = startTop + (targetTop - startTop) * eased;
      if (progress < 1) {
        tileHomeCenterFrame = requestAnimationFrame(step);
        return;
      }
      tileHomeCenterFrame = 0;
      setTileCameraX(targetLeft);
      editorViewport.scrollTop = targetTop;
      syncTileViewportHorizontalAccess(layout.tiles);
      flashTileCenterAxis(homeAxis);
      tileExtentCleanupSuppressedUntil = Date.now() + 120;
      window.setTimeout(() => scheduleTileExtentCleanup(0), 130);
      scheduleTileViewportScrollbarIdle();
    };
    tileHomeCenterFrame = requestAnimationFrame(step);
    return true;
  };
  const revealTileViewportHorizontalScrollbar = () => {
    if (tileViewportHorizontalIdleTimer) clearTimeout(tileViewportHorizontalIdleTimer);
    tileViewportHorizontalIdleTimer = 0;
    tileViewportHorizontalRevealRequested = true;
    tileCameraScrollbar?.classList.remove("movie-scrollbars-idle");
    syncTileViewportHorizontalAccess(tileRenderedTiles);
    scheduleTileViewportHorizontalIdle();
  };
  const revealTileViewportVerticalScrollbar = () => {
    if (!editorViewport) return;
    if (tileViewportVerticalIdleTimer) clearTimeout(tileViewportVerticalIdleTimer);
    tileViewportVerticalIdleTimer = 0;
    editorViewport.classList.remove("movie-scrollbars-idle");
    scheduleTileViewportVerticalIdle();
  };
  const revealTileViewportScrollbars = () => {
    revealTileViewportHorizontalScrollbar();
    revealTileViewportVerticalScrollbar();
  };
  const scheduleTileViewportHorizontalIdle = () => {
    if (tileViewportHorizontalIdleTimer) clearTimeout(tileViewportHorizontalIdleTimer);
    tileViewportHorizontalIdleTimer = window.setTimeout(() => {
      tileViewportHorizontalIdleTimer = 0;
      if (
        tileMoveDragActive
        || tileViewportScrollbarDragPointerId !== null
        || document.body.classList.contains("movie-panel-interacting")
        || tileViewportHorizontalHotZoneHovered
        || tileCameraScrollbar?.matches(":hover")
      ) {
        scheduleTileViewportHorizontalIdle();
        return;
      }
      tileViewportHorizontalRevealRequested = false;
      tileCameraScrollbar?.classList.add("movie-scrollbars-idle");
      syncTileViewportHorizontalAccess(tileRenderedTiles);
    }, tileViewportScrollbarIdleDelay);
  };
  const scheduleTileViewportVerticalIdle = () => {
    if (!editorViewport) return;
    if (tileViewportVerticalIdleTimer) clearTimeout(tileViewportVerticalIdleTimer);
    tileViewportVerticalIdleTimer = window.setTimeout(() => {
      tileViewportVerticalIdleTimer = 0;
      if (
        tileMoveDragActive
        || document.body.classList.contains("movie-panel-interacting")
        || tileViewportVerticalHotZoneHovered
      ) {
        scheduleTileViewportVerticalIdle();
        return;
      }
      editorViewport.classList.add("movie-scrollbars-idle");
    }, tileViewportScrollbarIdleDelay);
  };
  const scheduleTileViewportScrollbarIdle = () => {
    scheduleTileViewportHorizontalIdle();
    scheduleTileViewportVerticalIdle();
  };
  const settleTileViewportAfterScroll = (delay = 140) => {
    if (tileViewportScrollSettleTimer) clearTimeout(tileViewportScrollSettleTimer);
    tileViewportScrollSettleTimer = window.setTimeout(() => {
      tileViewportScrollSettleTimer = 0;
      if (
        tileMoveDragActive
        || tileHomeCenterFrame
        || tileCameraFrame
        || document.body.classList.contains("movie-panel-interacting")
      ) {
        settleTileViewportAfterScroll(Math.max(80, delay));
        return;
      }
      settleTileViewportOnHomeAxis({toleranceRatio: .05});
    }, delay);
  };
  tileCameraScrollbar?.addEventListener("pointerenter", () => {
    revealTileViewportHorizontalScrollbar();
  });
  tileCameraScrollbar?.addEventListener("pointerleave", () => {
    if (tileViewportScrollbarDragPointerId === null) scheduleTileViewportHorizontalIdle();
  });
  tileCameraScrollbar?.addEventListener("pointerdown", event => {
    if (event.button !== 0 || !tileCameraTrack || !tileCameraThumb) return;
    event.preventDefault();
    event.stopPropagation();
    cancelTileHomeCenter();
    revealTileViewportHorizontalScrollbar();
    tileViewportScrollbarDragPointerId = event.pointerId;
    tileViewportScrollbarDragChanged = false;
    if (tileViewportScrollSettleTimer) clearTimeout(tileViewportScrollSettleTimer);
    tileViewportScrollSettleTimer = 0;
    const pointerId = event.pointerId;
    const thumbBounds = tileCameraThumb.getBoundingClientRect();
    const grabOffset = tileCameraThumb.contains(event.target)
      ? event.clientX - thumbBounds.left
      : thumbBounds.width / 2;
    const update = next => {
      const trackBounds = tileCameraTrack.getBoundingClientRect();
      const thumbWidth = tileCameraThumb.getBoundingClientRect().width;
      const travel = Math.max(0, trackBounds.width - thumbWidth);
      const fraction = travel > 0
        ? clamp((next.clientX - trackBounds.left - grabOffset) / travel, 0, 1)
        : 0;
      const previous = tileCameraX;
      setTileCameraX(tileCameraMaximumX() * fraction);
      if (Math.abs(tileCameraX - previous) >= .5) tileViewportScrollbarDragChanged = true;
    };
    const finish = finishEvent => {
      if (finishEvent?.pointerId != null && finishEvent.pointerId !== pointerId) return;
      window.removeEventListener("pointermove", update, true);
      window.removeEventListener("pointerup", finish, true);
      window.removeEventListener("pointercancel", finish, true);
      releasePointerCaptureSafely(tileCameraScrollbar, pointerId);
      const changed = tileViewportScrollbarDragChanged;
      tileViewportScrollbarDragPointerId = null;
      tileViewportScrollbarDragChanged = false;
      if (changed) settleTileViewportAfterScroll(0);
      scheduleTileViewportHorizontalIdle();
    };
    capturePointerSafely(tileCameraScrollbar, pointerId);
    window.addEventListener("pointermove", update, true);
    window.addEventListener("pointerup", finish, true);
    window.addEventListener("pointercancel", finish, true);
    update(event);
  });
  tileCameraScrollbar?.addEventListener("keydown", event => {
    const maximum = tileCameraMaximumX();
    const smallStep = Math.max(24, editorViewport.clientWidth * .05);
    const largeStep = Math.max(120, editorViewport.clientWidth * .2);
    let next = null;
    if (event.key === "ArrowLeft") next = tileCameraX - smallStep;
    else if (event.key === "ArrowRight") next = tileCameraX + smallStep;
    else if (event.key === "PageUp") next = tileCameraX - largeStep;
    else if (event.key === "PageDown") next = tileCameraX + largeStep;
    else if (event.key === "Home") next = 0;
    else if (event.key === "End") next = maximum;
    if (next == null) return;
    event.preventDefault();
    revealTileViewportHorizontalScrollbar();
    setTileCameraX(next);
    settleTileViewportAfterScroll(80);
    scheduleTileViewportHorizontalIdle();
  });
  editorViewport?.addEventListener("wheel", event => {
    if (event.ctrlKey) return;
    const horizontalIntent = (
      event.shiftKey
      || Math.abs(event.deltaX) > Math.abs(event.deltaY)
    );
    if (horizontalIntent) {
      const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY)
        ? event.deltaX
        : event.deltaY;
      if (!delta) return;
      event.preventDefault();
      revealTileViewportHorizontalScrollbar();
      setTileCameraX(tileCameraX + delta);
      syncTileViewportHorizontalAccess(tileRenderedTiles);
      settleTileViewportAfterScroll(100);
      scheduleTileViewportHorizontalIdle();
      return;
    }
    if (!event.deltaY) return;
    revealTileViewportVerticalScrollbar();
    scheduleTileViewportVerticalIdle();
  }, {passive: false});
  editorViewport?.addEventListener("scroll", () => {
    revealTileViewportVerticalScrollbar();
    scheduleTileViewportVerticalIdle();
  }, {passive: true});
  editorViewport?.addEventListener("pointermove", event => {
    const bounds = editorViewport.getBoundingClientRect();
    const inVerticalScrollbarZone = bounds.right - event.clientX <= 28;
    const inHorizontalScrollbarZone = bounds.bottom - event.clientY <= 28;
    if (inVerticalScrollbarZone) {
      tileViewportVerticalHotZoneHovered = true;
      revealTileViewportVerticalScrollbar();
    } else if (tileViewportVerticalHotZoneHovered) {
      tileViewportVerticalHotZoneHovered = false;
      scheduleTileViewportVerticalIdle();
    }
    if (inHorizontalScrollbarZone) {
      tileViewportHorizontalHotZoneHovered = true;
      revealTileViewportHorizontalScrollbar();
    } else if (tileViewportHorizontalHotZoneHovered) {
      tileViewportHorizontalHotZoneHovered = false;
      scheduleTileViewportHorizontalIdle();
    }
  }, {passive: true});
  editorViewport?.addEventListener("pointerleave", () => {
    if (tileViewportHorizontalHotZoneHovered) {
      tileViewportHorizontalHotZoneHovered = false;
      scheduleTileViewportHorizontalIdle();
    }
    if (tileViewportVerticalHotZoneHovered) {
      tileViewportVerticalHotZoneHovered = false;
      scheduleTileViewportVerticalIdle();
    }
  }, {passive: true});
  const syncTileBottomEdgePointerPriority = event => {
    if (!tileCameraScrollbar || !editorLayout) return;
    const overBottomResizeEdge = [...editorLayout.querySelectorAll(
      ".movie-tile-edge.s,.movie-tile-edge.sw,.movie-tile-edge.se"
    )].some(edge => {
      const bounds = edge.getBoundingClientRect();
      return (
        event.clientX >= bounds.left
        && event.clientX <= bounds.right
        && event.clientY >= bounds.top
        && event.clientY <= bounds.bottom
      );
    });
    tileCameraScrollbar.classList.toggle("tile-resize-priority", overBottomResizeEdge);
  };
  window.addEventListener("pointermove", syncTileBottomEdgePointerPriority, {
    capture: true,
    passive: true,
  });
  window.addEventListener("blur", () => {
    tileCameraScrollbar?.classList.remove("tile-resize-priority");
  });
  window.addEventListener("pointerup", () => scheduleTileExtentCleanup(180), {passive: true});
  const nearestTileSnap = (value, candidates) => {
    let nearest = null;
    candidates.forEach(candidate => {
      const distance = Math.abs(candidate.value - value);
      if (distance > tileSnapDistance || (nearest && nearest.distance <= distance)) return;
      nearest = {...candidate, distance};
    });
    return nearest;
  };
  const ensureTileGuides = () => {
    if (!editorLayout) return {};
    let vertical = editorLayout.querySelector(":scope > .movie-tile-snap-guide.vertical");
    let horizontal = editorLayout.querySelector(":scope > .movie-tile-snap-guide.horizontal");
    if (!vertical) {
      vertical = document.createElement("i");
      vertical.className = "movie-tile-snap-guide vertical";
      editorLayout.appendChild(vertical);
    }
    if (!horizontal) {
      horizontal = document.createElement("i");
      horizontal.className = "movie-tile-snap-guide horizontal";
      editorLayout.appendChild(horizontal);
    }
    return {vertical, horizontal};
  };
  const hideTileGuides = () => {
    editorLayout?.querySelectorAll(":scope > .movie-tile-snap-guide").forEach(guide => {
      guide.hidden = true;
    });
  };
  const showTileGuides = ({x = null, y = null} = {}) => {
    const {vertical, horizontal} = ensureTileGuides();
    const offsetX = tileViewportGeometry?.offsetX || 0;
    const offsetY = tileViewportGeometry?.offsetY || 0;
    if (vertical) {
      vertical.hidden = !Number.isFinite(x);
      if (Number.isFinite(x)) vertical.style.left = `${x + offsetX}px`;
    }
    if (horizontal) {
      horizontal.hidden = !Number.isFinite(y);
      if (Number.isFinite(y)) horizontal.style.top = `${y + offsetY}px`;
    }
  };
  const snapMovingTile = (key, rect, tiles, workspace, horizontalExtent = null) => {
    const horizontalLeft = Number(horizontalExtent?.left ?? tileWorkspaceInset);
    const horizontalRight = Number(
      horizontalExtent?.right ?? workspace.width - tileWorkspaceInset,
    );
    const xCandidates = [
      {value: horizontalLeft, guide: horizontalLeft},
      {value: horizontalRight - rect.width, guide: horizontalRight},
    ];
    const yCandidates = [
      {value: tileWorkspaceInset, guide: tileWorkspaceInset},
      {value: workspace.height - rect.height - tileWorkspaceInset, guide: workspace.height - tileWorkspaceInset},
    ];
    tilePanelKeys.filter(otherKey => otherKey !== key).forEach(otherKey => {
      const other = tiles[otherKey];
      const right = other.x + other.width;
      const bottom = other.y + other.height;
      xCandidates.push(
        {value: other.x, guide: other.x},
        {value: right, guide: right},
        {value: other.x - rect.width, guide: other.x},
        {value: right - rect.width, guide: right},
      );
      yCandidates.push(
        {value: other.y, guide: other.y},
        {value: bottom, guide: bottom},
        {value: other.y - rect.height, guide: other.y},
        {value: bottom - rect.height, guide: bottom},
      );
    });
    const xSnap = nearestTileSnap(rect.x, xCandidates);
    const ySnap = nearestTileSnap(rect.y, yCandidates);
    return {
      rect: {
        ...rect,
        x: clamp(xSnap?.value ?? rect.x, horizontalLeft, horizontalRight - rect.width),
        y: clamp(ySnap?.value ?? rect.y, tileWorkspaceInset, workspace.height - rect.height - tileWorkspaceInset),
      },
      guide: {x: xSnap?.guide ?? null, y: ySnap?.guide ?? null},
    };
  };
  const snapMovingTileGroup = (
    keys,
    dx,
    dy,
    tiles,
    workspace,
    horizontalExtent = null,
  ) => {
    const selected = new Set(keys);
    const bounds = tileGroupBounds(tiles, keys);
    if (!bounds) return {dx: 0, dy: 0, guide: {x: null, y: null}};
    const horizontalLeft = Number(horizontalExtent?.left ?? tileWorkspaceInset);
    const horizontalRight = Number(
      horizontalExtent?.right ?? workspace.width - tileWorkspaceInset,
    );
    const clampedDx = clamp(dx, horizontalLeft - bounds.x, horizontalRight - bounds.right);
    const clampedDy = clamp(dy, tileWorkspaceInset - bounds.y, workspace.height - tileWorkspaceInset - bounds.bottom);
    const xCandidates = [
      {value: horizontalLeft - bounds.x, guide: horizontalLeft},
      {value: horizontalRight - bounds.right, guide: horizontalRight},
    ];
    const yCandidates = [
      {value: tileWorkspaceInset - bounds.y, guide: tileWorkspaceInset},
      {value: workspace.height - tileWorkspaceInset - bounds.bottom, guide: workspace.height - tileWorkspaceInset},
    ];
    tilePanelKeys.filter(otherKey => !selected.has(otherKey)).forEach(otherKey => {
      const other = tiles[otherKey];
      const otherRight = other.x + other.width;
      const otherBottom = other.y + other.height;
      xCandidates.push(
        {value: other.x - bounds.x, guide: other.x},
        {value: otherRight - bounds.x, guide: otherRight},
        {value: other.x - bounds.right, guide: other.x},
        {value: otherRight - bounds.right, guide: otherRight},
      );
      yCandidates.push(
        {value: other.y - bounds.y, guide: other.y},
        {value: otherBottom - bounds.y, guide: otherBottom},
        {value: other.y - bounds.bottom, guide: other.y},
        {value: otherBottom - bounds.bottom, guide: otherBottom},
      );
    });
    const xSnap = nearestTileSnap(clampedDx, xCandidates);
    const ySnap = nearestTileSnap(clampedDy, yCandidates);
    return {
      dx: clamp(xSnap?.value ?? clampedDx, horizontalLeft - bounds.x, horizontalRight - bounds.right),
      dy: clamp(ySnap?.value ?? clampedDy, tileWorkspaceInset - bounds.y, workspace.height - tileWorkspaceInset - bounds.bottom),
      guide: {x: xSnap?.guide ?? null, y: ySnap?.guide ?? null},
    };
  };
  const snapResizeEdge = (key, edge, value, tiles, workspace) => {
    const horizontal = edge === "e" || edge === "w";
    const candidates = horizontal
      ? [
          {value: tileWorkspaceInset, guide: tileWorkspaceInset},
          {value: workspace.width - tileWorkspaceInset, guide: workspace.width - tileWorkspaceInset},
        ]
      : [
          {value: tileWorkspaceInset, guide: tileWorkspaceInset},
          {value: workspace.height - tileWorkspaceInset, guide: workspace.height - tileWorkspaceInset},
        ];
    tilePanelKeys.filter(otherKey => otherKey !== key).forEach(otherKey => {
      const other = tiles[otherKey];
      if (horizontal) {
        candidates.push({value: other.x, guide: other.x}, {value: other.x + other.width, guide: other.x + other.width});
      } else {
        candidates.push({value: other.y, guide: other.y}, {value: other.y + other.height, guide: other.y + other.height});
      }
    });
    return nearestTileSnap(value, candidates);
  };
  const pushTileFromObstacle = (rect, obstacle, edge, workspace, minimum) => {
    const next = {...rect};
    if (edge === "e") {
      next.x = obstacle.x + obstacle.width;
      if (next.x + next.width > workspace.width - tileWorkspaceInset) next.width = workspace.width - tileWorkspaceInset - next.x;
    } else if (edge === "w") {
      const right = obstacle.x;
      next.x = Math.max(tileWorkspaceInset, right - next.width);
      next.width = right - next.x;
    } else if (edge === "s") {
      next.y = obstacle.y + obstacle.height;
      if (next.y + next.height > workspace.height - tileWorkspaceInset) next.height = workspace.height - tileWorkspaceInset - next.y;
    } else {
      const bottom = obstacle.y;
      next.y = Math.max(tileWorkspaceInset, bottom - next.height);
      next.height = bottom - next.y;
    }
    if (next.width < minimum.width || next.height < minimum.height) return null;
    return next;
  };
  const resolveResizeCollisions = (activeKey, candidate, edge, sourceTiles, workspace) => {
    const tiles = cloneTiles(sourceTiles);
    tiles[activeKey] = candidate;
    const queue = [activeKey];
    const processed = new Set();
    let attempts = 0;
    while (queue.length && attempts < 64) {
      const obstacleKey = queue.shift();
      const obstacle = tiles[obstacleKey];
      for (const otherKey of tilePanelKeys) {
        attempts += 1;
        if (otherKey === obstacleKey || otherKey === activeKey) continue;
        const pair = `${obstacleKey}:${otherKey}`;
        if (processed.has(pair) || !rectanglesOverlap(obstacle, tiles[otherKey])) continue;
        processed.add(pair);
        const pushed = pushTileFromObstacle(tiles[otherKey], obstacle, edge, workspace, tileMinimums[otherKey]);
        if (!pushed) return null;
        tiles[otherKey] = pushed;
        queue.push(otherKey);
      }
    }
    if (attempts >= 64 || tileLayoutHasOverlap(tiles)) return null;
    return tiles;
  };
  const resizeTileCandidate = (key, edge, origin, dx, dy, tiles, workspace) => {
    const minimum = tileMinimums[key];
    const defaults = tileLayoutDefaults.tiles[key];
    const maximumScale = tileMaximumScale(key);
    const maximumWidth = Math.min(workspace.width, defaults.width * maximumScale);
    const maximumHeight = Math.min(workspace.height, defaults.height * maximumScale);
    const next = {...origin};
    let guide = {x: null, y: null};
    const horizontalEdge = edge.includes("e") ? "e" : edge.includes("w") ? "w" : null;
    const verticalEdge = edge.includes("s") ? "s" : edge.includes("n") ? "n" : null;
    if (horizontalEdge === "e") {
      const raw = clamp(origin.x + origin.width + dx, origin.x + minimum.width, Math.min(workspace.width - tileWorkspaceInset, origin.x + maximumWidth));
      const snap = snapResizeEdge(key, horizontalEdge, raw, tiles, workspace);
      const right = clamp(snap?.value ?? raw, origin.x + minimum.width, Math.min(workspace.width - tileWorkspaceInset, origin.x + maximumWidth));
      next.width = right - origin.x;
      guide.x = snap?.guide ?? null;
    } else if (horizontalEdge === "w") {
      const raw = clamp(origin.x + dx, Math.max(tileWorkspaceInset, origin.x + origin.width - maximumWidth), origin.x + origin.width - minimum.width);
      const snap = snapResizeEdge(key, horizontalEdge, raw, tiles, workspace);
      next.x = clamp(snap?.value ?? raw, Math.max(tileWorkspaceInset, origin.x + origin.width - maximumWidth), origin.x + origin.width - minimum.width);
      next.width = origin.x + origin.width - next.x;
      guide.x = snap?.guide ?? null;
    }
    if (verticalEdge === "s") {
      const raw = clamp(origin.y + origin.height + dy, origin.y + minimum.height, Math.min(workspace.height - tileWorkspaceInset, origin.y + maximumHeight));
      const snap = snapResizeEdge(key, verticalEdge, raw, tiles, workspace);
      const bottom = clamp(snap?.value ?? raw, origin.y + minimum.height, Math.min(workspace.height - tileWorkspaceInset, origin.y + maximumHeight));
      next.height = bottom - origin.y;
      guide.y = snap?.guide ?? null;
    } else if (verticalEdge === "n") {
      const raw = clamp(origin.y + dy, Math.max(tileWorkspaceInset, origin.y + origin.height - maximumHeight), origin.y + origin.height - minimum.height);
      const snap = snapResizeEdge(key, verticalEdge, raw, tiles, workspace);
      next.y = clamp(snap?.value ?? raw, Math.max(tileWorkspaceInset, origin.y + origin.height - maximumHeight), origin.y + origin.height - minimum.height);
      next.height = origin.y + origin.height - next.y;
      guide.y = snap?.guide ?? null;
    }
    return {rect: next, guide};
  };
  const ensureTileEdges = (key, panel) => {
    if (!workspacePanel(key) || !panel) return;
    panel.querySelectorAll(":scope > .movie-tile-edge").forEach(handle => handle.remove());
    ["n", "e", "s", "w", "nw", "ne", "sw", "se"].forEach(edge => {
      const handle = document.createElement("i");
      handle.className = `movie-tile-edge ${edge}`;
      handle.dataset.tileEdge = edge;
      handle.dataset.tileKey = key;
      handle.title = `Resize ${key}`;
      panel.appendChild(handle);
    });
  };
  const bindPanelDragging = () => {
    initializeProjectFullscreen();
    Object.entries(layoutPanels).forEach(([key, panel]) => {
      if (!panel) return;
      rememberPanelHome(key, panel);
      ensurePanelChrome(key, panel);
      ensureTileEdges(key, panel);
    });
    initializeCanvasControls();
    window.refreshLexamoraIcons?.(root);
    if (editProjectsPanel) window.refreshLexamoraIcons?.(editProjectsPanel);
    localStorage.removeItem(floatingPanelsKey);

    const scrollEditorViewportTowardPointer = (
      pointer,
      movingBounds = null,
      {maxStep = 44, horizontal = true, vertical = true} = {},
    ) => {
      if (!editorViewport || !pointer) return false;
      const bounds = editorViewport.getBoundingClientRect();
      const edgeZone = Math.min(112, Math.max(56, bounds.width * .08));
      const axisStep = (position, start, end) => {
        if (position < start + edgeZone) {
          const pressure = clamp((start + edgeZone - position) / edgeZone, 0, 1);
          const smoothPressure = pressure * pressure * (3 - 2 * pressure);
          return -maxStep * smoothPressure;
        }
        if (position > end - edgeZone) {
          const pressure = clamp((position - (end - edgeZone)) / edgeZone, 0, 1);
          const smoothPressure = pressure * pressure * (3 - 2 * pressure);
          return maxStep * smoothPressure;
        }
        return 0;
      };
      const beforeLeft = tileCameraX;
      const beforeTop = editorViewport.scrollTop;
      const logicalViewport = tileLogicalViewportBounds(tileViewportGeometry);
      let horizontalPosition = pointer.clientX;
      if (movingBounds && logicalViewport) {
        const movingScreenLeft = bounds.left + movingBounds.x - logicalViewport.left;
        const movingScreenRight = bounds.left + movingBounds.right - logicalViewport.left;
        if (movingScreenRight > bounds.right - edgeZone) {
          horizontalPosition = Math.max(horizontalPosition, movingScreenRight);
        } else if (movingScreenLeft < bounds.left + edgeZone) {
          horizontalPosition = Math.min(horizontalPosition, movingScreenLeft);
        }
      }
      const maxTop = Math.max(0, editorViewport.scrollHeight - editorViewport.clientHeight);
      if (horizontal) {
        setTileCameraX(clamp(
          beforeLeft + axisStep(horizontalPosition, bounds.left, bounds.right),
          0,
          tileCameraMaximumX(),
        ));
      }
      if (vertical) {
        editorViewport.scrollTop = clamp(
          beforeTop + axisStep(pointer.clientY, bounds.top, bounds.bottom),
          0,
          maxTop,
        );
      }
      return tileCameraX !== beforeLeft || editorViewport.scrollTop !== beforeTop;
    };

    const tileDragBlockedSelector = [
      "button",
      "input",
      "select",
      "textarea",
      "a",
      "label",
      "summary",
      "[role='button']",
      "[contenteditable='true']",
      "[data-business-command]",
      "[data-business-icon]",
      ".icon-button",
      ".icon-tool",
      ".business-command-icon",
      "svg",
      ".movie-bin-item",
      ".movie-bin-folder-tile",
      ".movie-tile-edge",
      ".movie-window-resize-handle",
      "[data-panel-width-resizer]",
      "[data-panel-height-resizer]",
      "[data-timeline-edge-resizer]",
      "[data-timeline-height-resizer]",
      "[data-timeline-bottom-resizer]",
    ].join(",");
    const pointerNearTileEdge = (event, panel, inset = 14) => {
      const bounds = panel.getBoundingClientRect();
      return (
        event.clientX <= bounds.left + inset
        || event.clientX >= bounds.right - inset
        || event.clientY <= bounds.top + inset
        || event.clientY >= bounds.bottom - inset
      );
    };
    const pointerOnElementScrollbar = (event, element) => {
      if (!element) return false;
      const bounds = element.getBoundingClientRect();
      const verticalWidth = Math.max(0, element.offsetWidth - element.clientWidth);
      const horizontalHeight = Math.max(0, element.offsetHeight - element.clientHeight);
      return (
        (verticalWidth > 0 && event.clientX >= bounds.right - verticalWidth)
        || (horizontalHeight > 0 && event.clientY >= bounds.bottom - horizontalHeight)
      );
    };

    tilePanelKeys.forEach(key => {
      const panel = layoutPanels[key];
      const handle = panel?.matches(`[data-panel-drag="${key}"]`)
        ? panel
        : panel?.querySelector(`[data-panel-drag="${key}"]`);
      if (!panel || !handle || handle.dataset.tileDragBound === "true") return;
      handle.dataset.tileDragBound = "true";
      const beginTileDrag = event => {
        if (
          !canEdit
          || event.button !== 0
          || event.defaultPrevented
          || event.target.closest(tileDragBlockedSelector)
        ) return false;
        if (layoutLocked) {
          event.preventDefault();
          flashLayoutLock();
          return false;
        }
        event.stopPropagation();
        const layout = ensureTileLayout();
        const startTiles = cloneTiles(layout.tiles);
        const additiveSelection = event.ctrlKey || event.metaKey;
        const selectedAtPointerDown = selectedTileKeys.has(key);
        if (additiveSelection) {
          if (!selectedAtPointerDown) setSelectedTileKeys([...selectedTileKeys, key]);
        } else if (!selectedAtPointerDown) {
          setSelectedTileKeys([key]);
        }
        setTemporaryTileSelection(panel, true);
        const movingKeys = selectedTileKeys.has(key) ? [...selectedTileKeys] : [key];
        const startX = event.clientX;
        const startY = event.clientY;
        const startCameraX = tileCameraX;
        const startScrollTop = editorViewport?.scrollTop || 0;
        const pointerId = event.pointerId;
        if (!tileViewportGeometry) applyTileRects(startTiles);
        const dragGeometry = tileViewportGeometry;
        const dragOverscanExtent = fullTileHorizontalOverscanExtent(dragGeometry);
        const dragVisibleExtent = tileLogicalViewportBounds(dragGeometry);
        const dragHorizontalExtent = {
          left: Math.min(dragOverscanExtent.left, dragVisibleExtent?.left ?? Infinity),
          right: Math.max(dragOverscanExtent.right, dragVisibleExtent?.right ?? -Infinity),
        };
        const dragCameraMaximumX = tileCameraMaximumX(dragGeometry);
        const dragCameraSpeedMultiplier = 2;
        let active = false;
        let currentTiles = startTiles;
        let latestPointer = event;
        let dragFrame = 0;
        let previousDragPointerX = startX;
        let dragPointerDirectionX = 0;
        cancelTileHomeCenter();
        tileViewportScrollbarDragPointerId = null;
        tileViewportScrollbarDragChanged = false;
        if (tileViewportScrollSettleTimer) clearTimeout(tileViewportScrollSettleTimer);
        tileViewportScrollSettleTimer = 0;
        tileRenderedTiles = cloneTiles(startTiles);
        capturePointerSafely(handle, pointerId);
        const renderMovingTiles = guide => {
          tileRenderedTiles = cloneTiles(currentTiles);
          movingKeys.forEach(movingKey => {
            const panel = layoutPanels[movingKey];
            const rect = currentTiles[movingKey];
            const origin = startTiles[movingKey];
            if (!panel || !rect || !origin) return;
            panel.style.setProperty("--murrcut-tile-drag-x", `${rect.x - origin.x}px`);
            panel.style.setProperty("--murrcut-tile-drag-y", `${rect.y - origin.y}px`);
            panel.dataset.tileX = String(Math.round(rect.x));
            panel.dataset.tileY = String(Math.round(rect.y));
          });
          const invalidDrop = tileLayoutHasSelectionOverlap(currentTiles, movingKeys);
          movingKeys.forEach(movingKey => layoutPanels[movingKey]?.classList.toggle("movie-tile-drop-invalid", invalidDrop));
          showTileGuides(guide);
        };
        const updatePosition = (next, {render = true} = {}) => {
          const rawDx = next.clientX - startX + tileCameraX - startCameraX;
          const rawDy = next.clientY - startY + (editorViewport?.scrollTop || 0) - startScrollTop;
          if (!active && Math.hypot(rawDx, rawDy) < 4) return false;
          if (!active) {
            remember();
            window.getSelection()?.removeAllRanges();
            document.body.classList.add("movie-panel-interacting");
            tileMoveDragActive = true;
            revealTileViewportScrollbars();
            syncTileViewportHorizontalAccess(startTiles);
            if (tileExtentCleanupTimer) clearTimeout(tileExtentCleanupTimer);
            tileExtentCleanupTimer = 0;
          }
          active = true;
          const dx = rawDx;
          const dy = rawDy;
          const snapped = movingKeys.length > 1
            ? snapMovingTileGroup(
                movingKeys,
                dx,
                dy,
                startTiles,
                layout.workspace,
                dragHorizontalExtent,
              )
            : (() => {
                const origin = startTiles[key];
                const raw = {
                  ...origin,
                  x: clamp(
                    origin.x + dx,
                    dragHorizontalExtent.left,
                    dragHorizontalExtent.right - origin.width,
                  ),
                  y: clamp(origin.y + dy, tileWorkspaceInset, layout.workspace.height - origin.height - tileWorkspaceInset),
                };
                const single = snapMovingTile(
                  key,
                  raw,
                  startTiles,
                  layout.workspace,
                  dragHorizontalExtent,
                );
                return {dx: single.rect.x - origin.x, dy: single.rect.y - origin.y, guide: single.guide};
              })();
          const movement = snapped;
          currentTiles = cloneTiles(startTiles);
          movingKeys.forEach(movingKey => {
            currentTiles[movingKey] = {
              ...currentTiles[movingKey],
              x: startTiles[movingKey].x + movement.dx,
              y: startTiles[movingKey].y + movement.dy,
            };
            layoutPanels[movingKey]?.classList.add("movie-tile-moving");
          });
          if (render) renderMovingTiles(movement.guide);
          return true;
        };
        const driveVirtualCamera = () => {
          if (!editorViewport || !dragGeometry) return false;
          const movingBounds = tileGroupBounds(currentTiles, movingKeys);
          const visibleBounds = tileLogicalViewportBounds(dragGeometry);
          if (!movingBounds || !visibleBounds) return false;
          const triggerDistance = (
            editorViewport.clientWidth * tileViewportHorizontalActivationRatio
          );
          const rightGap = visibleBounds.right - movingBounds.right;
          const leftGap = movingBounds.x - visibleBounds.left;
          let pressure = 0;
          if (rightGap < triggerDistance) {
            pressure = clamp((triggerDistance - rightGap) / triggerDistance, 0, 1);
          } else if (leftGap < triggerDistance) {
            pressure = -clamp((triggerDistance - leftGap) / triggerDistance, 0, 1);
          }
          const magnitude = Math.abs(pressure);
          const smoothPressure = magnitude * magnitude * (3 - 2 * magnitude);
          if (!pressure) return false;
          if (
            (pressure > 0 && dragPointerDirectionX < 0)
            || (pressure < 0 && dragPointerDirectionX > 0)
          ) return false;
          const nextCameraX = clamp(
            tileCameraX + Math.sign(pressure) * 15 * dragCameraSpeedMultiplier * smoothPressure,
            0,
            dragCameraMaximumX,
          );
          if (Math.abs(nextCameraX - tileCameraX) < .01) return false;
          setTileCameraX(nextCameraX);
          return true;
        };
        const continueDragFrame = () => {
          dragFrame = 0;
          if (!latestPointer || !updatePosition(latestPointer, {render: false})) return;
          const cameraMoved = driveVirtualCamera();
          const verticalMoved = scrollEditorViewportTowardPointer(
            latestPointer,
            tileGroupBounds(currentTiles, movingKeys),
            {maxStep: 24, horizontal: false},
          );
          updatePosition(latestPointer);
          if (cameraMoved || verticalMoved) {
            dragFrame = requestAnimationFrame(continueDragFrame);
          }
        };
        const move = next => {
          latestPointer = next;
          const pointerDeltaX = next.clientX - previousDragPointerX;
          if (Math.abs(pointerDeltaX) >= .25) {
            dragPointerDirectionX = Math.sign(pointerDeltaX);
            previousDragPointerX = next.clientX;
          }
          if (active || Math.hypot(next.clientX - startX, next.clientY - startY) >= 4) next.preventDefault();
          if (!dragFrame) dragFrame = requestAnimationFrame(continueDragFrame);
        };
        let finished = false;
        const finish = finishEvent => {
          if (finishEvent?.pointerId != null && finishEvent.pointerId !== pointerId) return;
          if (finished) return;
          finished = true;
          clearEditorPointerFinish(finish);
          window.removeEventListener("pointermove", move, true);
          window.removeEventListener("pointerup", finish, true);
          window.removeEventListener("pointercancel", finish, true);
          if (dragFrame) cancelAnimationFrame(dragFrame);
          dragFrame = 0;
          if (Number.isFinite(finishEvent?.clientX) && Number.isFinite(finishEvent?.clientY)) {
            latestPointer = finishEvent;
          }
          if (latestPointer) updatePosition(latestPointer, {render: false});
          setTileCameraX(tileCameraX);
          releasePointerCaptureSafely(handle, pointerId);
          movingKeys.forEach(movingKey => {
            const movingPanel = layoutPanels[movingKey];
            movingPanel?.classList.remove("movie-tile-moving", "movie-tile-drop-invalid");
            movingPanel?.style.removeProperty("--murrcut-tile-drag-x");
            movingPanel?.style.removeProperty("--murrcut-tile-drag-y");
          });
          document.body.classList.remove("movie-panel-interacting");
          tileMoveDragActive = false;
          hideTileGuides();
          setTemporaryTileSelection(panel, false, active ? 0 : 180);
          if (!active) {
            if (additiveSelection && selectedAtPointerDown) {
              setSelectedTileKeys(
                [...selectedTileKeys].filter(selectedKey => selectedKey !== key)
              );
            }
            syncTileViewportHorizontalAccess(startTiles);
            scheduleTileViewportScrollbarIdle();
            scheduleTileExtentCleanup(180);
            return;
          }
          const invalidDrop = tileLayoutHasSelectionOverlap(currentTiles, movingKeys);
          const finalTiles = invalidDrop ? startTiles : currentTiles;
          if (invalidDrop) {
            toast("This space is occupied");
          } else {
            editorLayouts.columns = {...editorLayouts.columns, workspace: layout.workspace, tiles: currentTiles};
            persistLayout("columns");
          }
          if (invalidDrop) {
            if (tileExtentCleanupTimer) clearTimeout(tileExtentCleanupTimer);
            tileExtentCleanupTimer = 0;
            tileExtentCleanupSuppressedUntil = Date.now() + 300;
          }
          applyTileRects(finalTiles, {preserveViewport: true, scheduleCleanup: false});
          const balancedCameraTarget = balancedCameraTargetForTiles(
            finalTiles,
            tileViewportGeometry,
          );
          if (invalidDrop) {
            const cleanupDelay = Math.max(
              180,
              tileExtentCleanupSuppressedUntil - Date.now() + 10,
            );
            window.setTimeout(() => scheduleTileExtentCleanup(0), cleanupDelay);
          }
          syncTileViewportHorizontalAccess(finalTiles);
          if (Number.isFinite(balancedCameraTarget)) {
            setTileCameraX(
              balancedCameraTarget,
              {
                immediate: false,
                maxVelocity: 14,
              },
            );
            settleTileViewportAfterScroll(80);
            scheduleTileExtentCleanup(100);
          } else {
            settleTileViewportAfterScroll(120);
            scheduleTileExtentCleanup(180);
          }
          scheduleTileViewportScrollbarIdle();
          applyPreviewZoom();
          updatePreviewGeometry();
          renderTimeline();
          historyRedo = [];
          updateHistoryButtons();
        };
        window.addEventListener("pointermove", move, true);
        window.addEventListener("pointerup", finish, true);
        window.addEventListener("pointercancel", finish, true);
        registerEditorPointerFinish(finish);
        return true;
      };

      if (panel.dataset.tileDirectDragBound !== "true") {
        panel.dataset.tileDirectDragBound = "true";
        panel.addEventListener("pointerdown", event => {
          if (
            !canEdit
            || event.button !== 0
            || event.defaultPrevented
            || event.target.closest(tileDragBlockedSelector)
            || (
              key === "media"
              && pointerOnElementScrollbar(event, event.target.closest("[data-movie-bin]"))
            )
            || pointerNearTileEdge(event, panel)
          ) return;
          beginTileDrag(event);
        });
      }

      if (panel.dataset.tileHomeDblclickBound !== "true") {
        panel.dataset.tileHomeDblclickBound = "true";
        panel.addEventListener("dblclick", event => {
          if (
            !canEdit
            || event.button !== 0
            || event.target.closest(tileDragBlockedSelector)
            || pointerNearTileEdge(event, panel)
          ) return;
          event.preventDefault();
          event.stopPropagation();
          panel.querySelector(".movie-window-home")?.click();
        });
      }
    });

    if (editorLayout && editorLayout.dataset.tileMarqueeBound !== "true") {
      editorLayout.dataset.tileMarqueeBound = "true";
      const pointerToTilePoint = event => {
        const bounds = editorLayout.getBoundingClientRect();
        return {
          x: event.clientX - bounds.left - (tileViewportGeometry?.offsetX || 0),
          y: event.clientY - bounds.top - (tileViewportGeometry?.offsetY || 0),
        };
      };
      editorLayout.addEventListener("pointerdown", event => {
        if (!canEdit || event.button !== 0) return;
        if (event.target.closest("[data-panel-key],.movie-tile-edge,.movie-tile-snap-guide")) return;
        if (layoutLocked) {
          event.preventDefault();
          flashLayoutLock();
          return;
        }
        event.preventDefault();
        const layout = ensureTileLayout();
        const start = pointerToTilePoint(event);
        const preserved = event.ctrlKey || event.metaKey || event.shiftKey ? new Set(selectedTileKeys) : new Set();
        const marquee = document.createElement("i");
        marquee.className = "movie-tile-marquee";
        editorLayout.appendChild(marquee);
        const pointerId = event.pointerId;
        let active = false;
        capturePointerSafely(editorLayout, pointerId);
        const update = next => {
          const current = pointerToTilePoint(next);
          const left = Math.min(start.x, current.x);
          const top = Math.min(start.y, current.y);
          const right = Math.max(start.x, current.x);
          const bottom = Math.max(start.y, current.y);
          if (!active && Math.hypot(current.x - start.x, current.y - start.y) < 4) return;
          active = true;
          marquee.style.left = `${left + (tileViewportGeometry?.offsetX || 0)}px`;
          marquee.style.top = `${top + (tileViewportGeometry?.offsetY || 0)}px`;
          marquee.style.width = `${right - left}px`;
          marquee.style.height = `${bottom - top}px`;
          const box = {x: left, y: top, width: right - left, height: bottom - top};
          const nextSelection = new Set(preserved);
          tilePanelKeys.forEach(tileKey => {
            if (rectanglesOverlap(box, layout.tiles[tileKey], 0)) nextSelection.add(tileKey);
          });
          setSelectedTileKeys([...nextSelection]);
        };
        const finish = finishEvent => {
          if (finishEvent?.pointerId != null && finishEvent.pointerId !== pointerId) return;
          window.removeEventListener("pointermove", update, true);
          window.removeEventListener("pointerup", finish, true);
          window.removeEventListener("pointercancel", finish, true);
          releasePointerCaptureSafely(editorLayout, pointerId);
          marquee.remove();
          if (!active) clearTileSelection();
        };
        window.addEventListener("pointermove", update, true);
        window.addEventListener("pointerup", finish, true);
        window.addEventListener("pointercancel", finish, true);
      });
    }

    editorLayout?.querySelectorAll(".movie-tile-edge").forEach(handle => {
      if (handle.dataset.tileResizeBound === "true") return;
      handle.dataset.tileResizeBound = "true";
      handle.addEventListener("pointerdown", event => {
        if (!canEdit || event.button !== 0) return;
        if (layoutLocked) {
          event.preventDefault();
          flashLayoutLock();
          return;
        }
        event.preventDefault();
        event.stopPropagation();
        const key = handle.dataset.tileKey;
        const edge = handle.dataset.tileEdge;
        const panel = layoutPanels[key];
        const layout = ensureTileLayout();
        const startTiles = cloneTiles(layout.tiles);
        const origin = {...startTiles[key]};
        const startX = event.clientX;
        const startY = event.clientY;
        const horizontalEdge = edge.includes("e") ? "e" : edge.includes("w") ? "w" : null;
        const verticalEdge = edge.includes("s") ? "s" : edge.includes("n") ? "n" : null;
        const horizontalSide = horizontalEdge === "e" ? 1 : horizontalEdge === "w" ? -1 : 0;
        const pointerId = event.pointerId;
        let active = false;
        let currentTiles = startTiles;
        let latestPointer = event;
        let autoScrollFrame = 0;
        let observedCameraX = tileCameraX;
        let collisionPushEdge = null;
        const pointerToLogicalPoint = pointer => {
          const visible = tileLogicalViewportBounds(tileViewportGeometry);
          const viewportBounds = editorViewport?.getBoundingClientRect();
          if (!visible || !viewportBounds) return null;
          return {
            x: visible.left + pointer.clientX - viewportBounds.left,
            y: visible.top + pointer.clientY - viewportBounds.top,
          };
        };
        const startPointerLogical = pointerToLogicalPoint(event);
        cancelTileHomeCenter();
        capturePointerSafely(handle, pointerId);
        const driveVirtualResizeCamera = () => {
          if (!editorViewport || !horizontalSide) return;
          const movingBounds = tileGroupBounds(currentTiles, [key]);
          const visibleBounds = tileLogicalViewportBounds(tileViewportGeometry);
          if (!movingBounds || !visibleBounds) return;
          const triggerDistance = (
            editorViewport.clientWidth * tileViewportHorizontalActivationRatio
          );
          const gap = horizontalSide > 0
            ? visibleBounds.right - movingBounds.right
            : movingBounds.x - visibleBounds.left;
          const pressure = gap < triggerDistance
            ? clamp((triggerDistance - gap) / triggerDistance, 0, 1)
            : 0;
          if (pressure) {
            const smoothPressure = pressure * pressure * (3 - 2 * pressure);
            setTileCameraX(
              tileCameraX + horizontalSide * 9 * smoothPressure,
            );
          }
        };
        const updateSize = next => {
          const pointerLogical = pointerToLogicalPoint(next);
          if (!pointerLogical || !startPointerLogical) return;
          const dx = pointerLogical.x - startPointerLogical.x;
          const dy = pointerLogical.y - startPointerLogical.y;
          if (!active && Math.hypot(next.clientX - startX, next.clientY - startY) < 2) return;
          if (!active) {
            remember();
            revealTileViewportScrollbars();
          }
          active = true;
          const pointerDx = next.clientX - startX;
          const pointerDy = next.clientY - startY;
          if (!collisionPushEdge) {
            collisionPushEdge = horizontalEdge && verticalEdge
              ? (Math.abs(pointerDx) >= Math.abs(pointerDy) ? horizontalEdge : verticalEdge)
              : horizontalEdge || verticalEdge;
          }
          const resized = resizeTileCandidate(
            key,
            edge,
            origin,
            dx,
            dy,
            startTiles,
            layout.workspace,
          );
          const resolved = resolveResizeCollisions(
            key,
            resized.rect,
            collisionPushEdge,
            startTiles,
            layout.workspace,
          );
          if (!resolved) return;
          currentTiles = resolved;
          panel?.classList.add("movie-tile-resizing");
          handle.classList.add("active");
          document.body.classList.add("movie-panel-interacting");
          showTileGuides(resized.guide);
          applyTileRects(currentTiles);
          applyPreviewZoom();
          updatePreviewGeometry();
        };
        const continueAutoScroll = () => {
          autoScrollFrame = 0;
          if (!active || !latestPointer) return;
          driveVirtualResizeCamera();
          const verticalMoved = scrollEditorViewportTowardPointer(
            latestPointer,
            tileGroupBounds(currentTiles, [key]),
            {maxStep: 18, horizontal: false},
          );
          const horizontalMoved = Math.abs(tileCameraX - observedCameraX) >= .01;
          observedCameraX = tileCameraX;
          if (horizontalMoved || verticalMoved) updateSize(latestPointer);
          autoScrollFrame = requestAnimationFrame(continueAutoScroll);
        };
        const move = next => {
          latestPointer = next;
          updateSize(next);
          if (active && !autoScrollFrame) autoScrollFrame = requestAnimationFrame(continueAutoScroll);
        };
        let finished = false;
        const finish = finishEvent => {
          if (finishEvent?.pointerId != null && finishEvent.pointerId !== pointerId) return;
          if (finished) return;
          finished = true;
          clearEditorPointerFinish(finish);
          window.removeEventListener("pointermove", move, true);
          window.removeEventListener("pointerup", finish, true);
          window.removeEventListener("pointercancel", finish, true);
          if (autoScrollFrame) cancelAnimationFrame(autoScrollFrame);
          autoScrollFrame = 0;
          releasePointerCaptureSafely(handle, pointerId);
          panel?.classList.remove("movie-tile-resizing");
          handle.classList.remove("active");
          document.body.classList.remove("movie-panel-interacting");
          hideTileGuides();
          if (!active) {
            scheduleTileViewportScrollbarIdle();
            return;
          }
          editorLayouts.columns = {...editorLayouts.columns, workspace: layout.workspace, tiles: currentTiles};
          persistLayout("columns");
          applyTileRects(currentTiles);
          const balancedCameraTarget = balancedCameraTargetForTiles(
            currentTiles,
            tileViewportGeometry,
          );
          if (Number.isFinite(balancedCameraTarget)) {
            setTileCameraX(balancedCameraTarget, {
              immediate: false,
              maxVelocity: 14,
            });
          } else {
            setTileCameraX(tileCameraX);
          }
          settleTileViewportAfterScroll(120);
          scheduleTileViewportScrollbarIdle();
          scheduleTileExtentCleanup(120);
          applyPreviewZoom();
          updatePreviewGeometry();
          renderTimeline();
          historyRedo = [];
          updateHistoryButtons();
        };
        window.addEventListener("pointermove", move, true);
        window.addEventListener("pointerup", finish, true);
        window.addEventListener("pointercancel", finish, true);
        registerEditorPointerFinish(finish);
      });
    });

    if (root.dataset.panelSnapCleanupBound !== "true") {
      root.dataset.panelSnapCleanupBound = "true";
      document.addEventListener("pointerup", finishActiveEditorPointer, true);
      document.addEventListener("pointercancel", finishActiveEditorPointer, true);
      window.addEventListener("blur", finishActiveEditorPointer);
      document.addEventListener("visibilitychange", () => {
        if (document.hidden) finishActiveEditorPointer();
      });
    }
  };

  const toast = (message, type) => window.studioToast ? window.studioToast(String(message).replace(/\.$/, ""), type) : console.info(message);
  const csrfToken = () => document.cookie.match(/csrftoken=([^;]+)/)?.[1] || "";
  const uid = () => crypto.randomUUID ? crypto.randomUUID() : `clip-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const clamp = (value, minimum, maximum) => Math.max(minimum, Math.min(maximum, value));
  const quantize = value => Math.round(Number(value || 0) / PRECISION_MS) * PRECISION_MS;
  const frameMs = () => 1000 / Math.max(1, Number(q("[data-movie-fps]").value) || 25);
  const quantizeFrame = value => Math.round(Math.round(Number(value || 0) / frameMs()) * frameMs());
  const clock = ms => {
    const total = Math.max(0, Math.round(ms));
    const minutes = Math.floor(total / 60000);
    const seconds = Math.floor(total % 60000 / 1000);
    const millis = total % 1000;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
  };
  const requestJson = async (url, options = {}) => {
    const response = await fetch(url, options);
    const text = await response.text();
    let data = {};
    try { data = text ? JSON.parse(text) : {}; }
    catch (_) { data = {error: text.slice(0, 300) || `HTTP ${response.status}`}; }
    if (!response.ok) throw new Error(data.error || data.detail || `HTTP ${response.status}`);
    return data;
  };
  const clipKind = clip => assetMap.get(clip.assetId)?.kind || "VIDEO";
  const timelineEnd = () => Math.max(0, ...timeline.tracks.flatMap(track =>
    track.clips.filter(clip => ["VIDEO", "IMAGE"].includes(clipKind(clip))).map(clip => clip.start + clip.duration)
  ));
  const visibleDuration = () => Math.max(60000, timelineEnd() + 15000);
  const assetDuration = clip => Math.max(200, assetMap.get(clip.assetId)?.durationMs || clip.sourceStart + clip.duration);
  const findClip = id => {
    for (const track of timeline.tracks) {
      const clip = track.clips.find(item => item.id === id);
      if (clip) return {clip, track};
    }
    return null;
  };
  const activeClip = (kind, at) => {
    for (const track of timeline.tracks) {
      if (kind === "AUDIO" && track.muted) continue;
      const clip = [...track.clips].reverse().find(item => clipKind(item) === kind && at >= item.start && at < item.start + item.duration);
      if (clip) return {clip, track};
    }
    return null;
  };
  const activeVisualClips = at => {
    const active = [];
    timeline.tracks.forEach((track, trackIndex) => {
      if (track.hidden) return;
      track.clips.forEach((clip, clipIndex) => {
        if (!["VIDEO", "IMAGE"].includes(clipKind(clip))) return;
        if (at >= clip.start && at < clip.start + clip.duration) active.push({clip, track, trackIndex, clipIndex});
      });
    });
    return active.sort((left, right) => right.trackIndex - left.trackIndex || left.clipIndex - right.clipIndex);
  };
  const currentState = () => ({
    timeline: structuredClone(timeline),
    title: q("[data-movie-title]").value,
    aspectRatio: q("[data-movie-ratio]").value,
    resolution: q("[data-movie-resolution]").value,
    fps: q("[data-movie-fps]").value,
    selectedId,
    editorLayout: cloneLayoutState(),
  });
  const signature = state => {
    const value = state || currentState();
    const {editorLayout: _editorLayout, ...projectState} = value;
    return JSON.stringify({ ...projectState, selectedId: null });
  };
  const updateHistoryButtons = () => {
    qa("[data-editor-undo]").forEach(button => { button.disabled = !canEdit || !historyUndo.length; });
    qa("[data-editor-redo]").forEach(button => { button.disabled = !canEdit || !historyRedo.length; });
  };
  const updateDirty = () => {
    dirty = canEdit && signature() !== savedSignature;
    stateNode.textContent = dirty ? "Unsaved changes" : "Saved";
    stateNode.classList.remove("error");
  };
  const remember = () => {
    if (!canEdit) return;
    historyUndo.push(currentState());
    if (historyUndo.length > 100) historyUndo.shift();
    historyRedo = [];
    updateHistoryButtons();
  };
  const applyState = state => {
    stopPlayback();
    timeline = structuredClone(state.timeline);
    selectedId = state.selectedId || null;
    selectedIds = new Set(selectedId ? [selectedId] : []);
    q("[data-movie-title]").value = state.title;
    q("[data-movie-ratio]").value = state.aspectRatio;
    q("[data-movie-resolution]").value = state.resolution;
    q("[data-movie-fps]").value = state.fps;
    if (state.editorLayout) applyEditorLayout(state.editorLayout);
    applyCanvas();
    normalizeTimeline();
    renderTimeline();
    renderInspector();
    updateDirty();
    updateHistoryButtons();
    scheduleAutosave();
  };
  const undo = () => {
    if (!historyUndo.length) return;
    historyRedo.push(currentState());
    applyState(historyUndo.pop());
  };
  const redo = () => {
    if (!historyRedo.length) return;
    historyUndo.push(currentState());
    applyState(historyRedo.pop());
  };
  const mutate = callback => {
    if (!canEdit) return;
    remember();
    callback();
    normalizeTimeline();
    updateDirty();
    scheduleAutosave();
  };

  const scheduleAutosave = (delay = 700) => {
    if (!canEdit) return;
    clearTimeout(autosaveTimer);
    autosaveTimer = setTimeout(() => { if (dirty) saveTimeline(true); }, delay);
  };

  function normalizeTimeline() {
    timeline.schemaVersion = 1;
    timeline.tracks = Array.isArray(timeline.tracks) ? timeline.tracks : [];
    const usedMedia = timeline.tracks.flatMap(track => (track.clips || []).map(clip => clip.assetId));
    timeline.mediaAssetIds = Array.isArray(timeline.mediaAssetIds) ? timeline.mediaAssetIds : [...new Set(usedMedia)];
    usedMedia.forEach(id => { if (id && !timeline.mediaAssetIds.includes(id)) timeline.mediaAssetIds.push(id); });
    timeline.mediaOrder = Array.isArray(timeline.mediaOrder) ? timeline.mediaOrder.filter(id => timeline.mediaAssetIds.includes(id)) : [];
    timeline.mediaAssetIds.forEach(id => { if (!timeline.mediaOrder.includes(id)) timeline.mediaOrder.push(id); });
    timeline.tracks.forEach((track, trackIndex) => {
      track.id ||= uid();
      track.kind = ["VIDEO", "AUDIO", "TEXT"].includes(track.kind) ? track.kind : "VIDEO";
      track.name ||= `${track.kind === "VIDEO" ? "Video" : "Audio"} ${trackIndex + 1}`;
      track.muted = Boolean(track.muted);
      track.locked = Boolean(track.locked);
      track.hidden = Boolean(track.hidden);
      track.height = clamp(Number(track.height || defaultTrackHeight), 21, 210);
      track.displayMode = ["CLIPS", "WAVEFORM"].includes(track.displayMode) ? track.displayMode : "CLIPS";
      track.loudnessGuide = clamp(Number(track.loudnessGuide ?? 1), 0, 2);
      track.clips = Array.isArray(track.clips) ? track.clips : [];
      track.clips.forEach(clip => {
        clip.start = Math.max(0, quantize(clip.start));
        clip.sourceStart = Math.max(0, quantize(clip.sourceStart));
        clip.speed = clamp(Number(clip.speed ?? 1), .1, 8);
        const available = Math.max(200, (assetDuration(clip) - clip.sourceStart) / clip.speed);
        clip.duration = clamp(quantize(clip.duration || 200), 200, available);
        clip.volume = clamp(Number(clip.volume ?? 1), 0, 2);
        clip.scale = clamp(Number(clip.scale ?? 1), .05, 8);
        clip.positionX = clamp(Number(clip.positionX ?? 0), -7680, 7680);
        clip.positionY = clamp(Number(clip.positionY ?? 0), -7680, 7680);
        clip.positionUnit = "PIXELS";
        clip.sourceAssetId ||= clip.assetId;
        clip.speedMethod = ["FRAME_SAMPLE", "FRAME_BLEND", "OPTICAL_FLOW"].includes(clip.speedMethod) ? clip.speedMethod : "FRAME_SAMPLE";
        clip.fadeIn = clamp(Number(clip.fadeIn || 0), 0, 2000);
        clip.fadeOut = clamp(Number(clip.fadeOut || 0), 0, 2000);
        clip.audioCleanup = ["NONE", "VOICE", "DENOISE"].includes(clip.audioCleanup) ? clip.audioCleanup : "NONE";
        clip.opacity = clamp(Number(clip.opacity ?? 1), 0, 1);
        clip.blur = clamp(Number(clip.blur || 0), 0, 40);
        clip.sharpen = clamp(Number(clip.sharpen || 0), 0, 5);
        clip.brightness = clamp(Number(clip.brightness || 0), -1, 1);
        clip.contrast = clamp(Number(clip.contrast ?? 1), 0, 3);
        clip.saturation = clamp(Number(clip.saturation ?? 1), 0, 3);
        clip.gamma = clamp(Number(clip.gamma ?? 1), .1, 3);
        clip.volumeKeyframes = Array.isArray(clip.volumeKeyframes) ? clip.volumeKeyframes.map(point => ({
          time: clamp(Number(point.time || 0), 0, clip.duration),
          value: clamp(Number(point.value ?? 1), 0, 2),
        })).sort((a, b) => a.time - b.time) : [];
      });
    });
  }

  function applyPreviewZoom() {
    const body = q(".movie-preview-body");
    if (!body || !previewStage) return;
    const output = outputDimensions();
    const availableWidth = Math.max(40, body.clientWidth - 80);
    const availableHeight = Math.max(40, body.clientHeight - 12);
    const fitZoom = Math.min(availableWidth / output.width, availableHeight / output.height);
    if (!previewZoomManual) previewZoom = clamp(fitZoom, .05, 2);
    previewStage.style.width = `${Math.max(40, output.width * previewZoom)}px`;
    previewStage.style.height = `${Math.max(40, output.height * previewZoom)}px`;
    previewStage.style.aspectRatio = `${output.width} / ${output.height}`;
    if (previewZoomInput) previewZoomInput.value = previewZoom;
    if (previewZoomValue && document.activeElement !== previewZoomValue) previewZoomValue.value = String(Math.round(previewZoom * 100));
  }

  function syncFrameSizeControls() {
    if (!frameSizeInput || !frameSizeValue || !frameScaleValue) return;
    const found = selectedId ? findClip(selectedId) : null;
    const asset = found ? assetMap.get(found.clip.assetId) : null;
    const available = Boolean(found && asset && ["VIDEO", "IMAGE"].includes(asset.kind) && !found.track.locked);
    frameSizeInput.disabled = !available;
    frameSizeValue.disabled = !available;
    frameScaleValue.disabled = !available;
    q("[data-frame-size-down]")?.toggleAttribute("disabled", !available);
    q("[data-frame-size-up]")?.toggleAttribute("disabled", !available);
    if (!available) {
      frameScaleValue.value = "100";
      frameSizeValue.value = String(outputDimensions().width);
      return;
    }
    const sourceWidth = Math.max(1, Number(asset.width) || outputDimensions().width);
    const scale = clamp(Number(found.clip.scale || 1), .05, 8);
    const width = Math.max(1, Math.round(sourceWidth * scale));
    if (document.activeElement !== frameSizeInput) frameSizeInput.value = String(scale);
    if (document.activeElement !== frameScaleValue) frameScaleValue.value = String(Math.round(scale * 100));
    frameSizeValue.value = String(width);
  }

  function setSelectedFrameScale(scale, {rememberChange = true} = {}) {
    const found = selectedId ? findClip(selectedId) : null;
    const asset = found ? assetMap.get(found.clip.assetId) : null;
    if (!found || !asset || found.track.locked || !["VIDEO", "IMAGE"].includes(asset.kind)) return false;
    const target = clamp(Number(scale) || 1, .05, 8);
    if (Math.abs(Number(found.clip.scale || 1) - target) < .0001) return false;
    if (rememberChange) remember();
    found.clip.scale = target;
    if (rememberChange) {
      historyRedo = [];
      updateDirty();
      updateHistoryButtons();
      scheduleAutosave();
    }
    updatePreviewGeometry();
    renderInspector();
    return true;
  }

  function applyCanvas() {
    const ratio = q("[data-movie-ratio]").value || "16:9";
    const resolution = q("[data-movie-resolution]");
    const choices = {
      "16:9": ["640x360", "854x480", "1280x720", "1920x1080", "2560x1440", "3840x2160"],
      "9:16": ["360x640", "480x854", "720x1280", "1080x1920", "1440x2560", "2160x3840"],
      "1:1": ["480x480", "640x640", "720x720", "1080x1080", "1440x1440", "2160x2160"],
    };
    const previous = resolution.value;
    resolution.replaceChildren(...choices[ratio].map(value => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      return option;
    }));
    resolution.value = choices[ratio].includes(previous) ? previous : choices[ratio][Math.min(3, choices[ratio].length - 1)];
    const output = outputDimensions();
    previewStage.style.aspectRatio = `${output.width} / ${output.height}`;
    previewStage.dataset.ratio = ratio;
    applyPreviewZoom();
    updatePreviewGeometry();
  }

  function previewClipData() {
    const selected = findClip(selectedId);
    if (selected && ["VIDEO", "IMAGE"].includes(clipKind(selected.clip))) {
      return {...selected, asset: assetMap.get(selected.clip.assetId)};
    }
    if (standalonePreviewAssetId) return {asset: assetMap.get(standalonePreviewAssetId), clip: null, track: null};
    const active = activeVisualClips(playheadMs).at(-1);
    return active ? {...active, asset: assetMap.get(active.clip.assetId)} : {asset: null, clip: null, track: null};
  }

  const outputDimensions = () => {
    const value = q("[data-movie-resolution]").value || "1920x1080";
    const [width, height] = value.split("x").map(Number);
    return {width: width || 1920, height: height || 1080};
  };

  function clipGeometry(asset, clip) {
    const output = outputDimensions();
    const sourceWidth = asset?.width || 0;
    const sourceHeight = asset?.height || 0;
    const exactCanvas = sourceWidth === output.width && sourceHeight === output.height;
    const sourceRatio = sourceWidth && sourceHeight ? sourceWidth / sourceHeight : output.width / output.height;
    if (!clip) {
      const outputRatio = output.width / output.height;
      const widthPx = sourceRatio >= outputRatio ? output.width : output.height * sourceRatio;
      const heightPx = sourceRatio >= outputRatio ? output.width / sourceRatio : output.height;
      return {
        output, sourceRatio, widthPx, heightPx, exactCanvas,
        width: widthPx / output.width * 100,
        height: heightPx / output.height * 100,
        left: 50,
        top: 50,
      };
    }
    const scale = clamp(Number(clip?.scale ?? 1), .05, 8);
    // Clip scale is based on the source's native pixels. This makes the selected
    // output resolution a real canvas instead of silently stretching every clip
    // to cover it at 100%.
    const widthPx = (sourceWidth || output.width) * scale;
    const heightPx = (sourceHeight || output.height) * scale;
    const x = Number(clip?.positionX || 0);
    const y = Number(clip?.positionY || 0);
    return {
      output, sourceRatio, widthPx, heightPx, exactCanvas,
      width: widthPx / output.width * 100,
      height: heightPx / output.height * 100,
      left: 50 + x / output.width * 100,
      top: 50 - y / output.height * 100,
    };
  }

  function applyGeometry(node, asset, clip) {
    const geometry = clipGeometry(asset, clip);
    const stageWidth = Math.max(1, previewStage.clientWidth);
    const stageHeight = Math.max(1, previewStage.clientHeight);
    // Matching source/output canvases must remain edge-to-edge at every UI zoom.
    // A symmetric one-pixel display overscan prevents fractional CSS rounding
    // from exposing the stage background without changing render geometry.
    const seamGuard = geometry.exactCanvas && Number(clip?.scale ?? 1) === 1
      && Number(clip?.positionX || 0) === 0 && Number(clip?.positionY || 0) === 0 ? 1 : 0;
    node.style.width = `${geometry.width / 100 * stageWidth + seamGuard * 2}px`;
    node.style.height = `${geometry.height / 100 * stageHeight + seamGuard * 2}px`;
    // X/Y are persisted correctly, so make them authoritative over legacy
    // preview layout rules as well as over the base video defaults.
    node.style.setProperty("left", `${geometry.left / 100 * stageWidth}px`, "important");
    node.style.setProperty("top", `${geometry.top / 100 * stageHeight}px`, "important");
    node.style.transform = "translate(-50%,-50%)";
    node.style.opacity = String(clamp(Number(clip?.opacity ?? 1), 0, 1));
    node.style.filter = `brightness(${Math.max(0, 1 + Number(clip?.brightness || 0))}) contrast(${Number(clip?.contrast ?? 1)}) saturate(${Number(clip?.saturation ?? 1)}) blur(${Number(clip?.blur || 0) / 4}px)`;
    return geometry;
  }

  function applyStandalonePreviewGeometry(asset = assetMap.get(standalonePreviewAssetId)) {
    const output = outputDimensions();
    const mediaNode = asset?.kind === "IMAGE" ? previewImage : preview;
    const sourceWidth = Number(asset?.width || mediaNode?.naturalWidth || preview.videoWidth || 0);
    const sourceHeight = Number(asset?.height || mediaNode?.naturalHeight || preview.videoHeight || 0);
    const exactCanvas = sourceWidth === output.width && sourceHeight === output.height;
    const guard = exactCanvas ? 1 : 0;
    mediaNode.style.setProperty("width", `calc(100% + ${guard * 2}px)`, "important");
    mediaNode.style.setProperty("height", `calc(100% + ${guard * 2}px)`, "important");
    mediaNode.style.setProperty("left", `${-guard}px`, "important");
    mediaNode.style.setProperty("top", `${-guard}px`, "important");
    mediaNode.style.setProperty("transform", "none", "important");
    mediaNode.style.setProperty("object-fit", exactCanvas ? "fill" : "contain", "important");
  }

  function renderMediaPreviewSelection() {
    qa(".movie-bin-item").forEach(item => {
      const selected = item.dataset.assetId === standalonePreviewAssetId;
      item.classList.toggle("previewing", selected);
      item.setAttribute("aria-selected", selected ? "true" : "false");
    });
  }

  function leaveStandalonePreview() {
    if (!standalonePreviewAssetId) return;
    standalonePreviewAssetId = null;
    preview.pause();
    preview.hidden = true;
    previewImage.hidden = true;
    preview.removeAttribute("src");
    previewImage.removeAttribute("src");
    renderMediaPreviewSelection();
  }

  function updateLayerSnapGuides() {
    const vertical = q('[data-layer-snap-guide="vertical"]');
    const horizontal = q('[data-layer-snap-guide="horizontal"]');
    [vertical, horizontal].forEach(guide => {
      guide?.classList.remove("active");
      if (guide) guide.style.removeProperty("--guide-position");
    });
    if (!selectedId || standalonePreviewAssetId) return;
    const selected = visualPlayers.get(selectedId);
    if (!selected || selected.hidden) return;
    const stage = previewStage.getBoundingClientRect();
    const source = selected.getBoundingClientRect();
    const tolerance = 3;
    visualPlayers.forEach((node, clipId) => {
      if (clipId === selectedId || node.hidden) return;
      const target = node.getBoundingClientRect();
      const verticalPairs = [
        [source.left, target.left], [source.left, target.right],
        [source.right, target.left], [source.right, target.right],
      ];
      const horizontalPairs = [
        [source.top, target.top], [source.top, target.bottom],
        [source.bottom, target.top], [source.bottom, target.bottom],
      ];
      const x = verticalPairs.find(([first, second]) => Math.abs(first - second) <= tolerance);
      const y = horizontalPairs.find(([first, second]) => Math.abs(first - second) <= tolerance);
      if (x && vertical) {
        vertical.style.setProperty("--guide-position", `${x[1] - stage.left}px`);
        vertical.classList.add("active");
      }
      if (y && horizontal) {
        horizontal.style.setProperty("--guide-position", `${y[1] - stage.top}px`);
        horizontal.classList.add("active");
      }
    });
  }

  function updatePreviewGeometry() {
    const {asset, clip} = previewClipData();
    const geometry = asset ? applyGeometry(previewOutline, asset, clip) : null;
    if (standalonePreviewAssetId && asset) applyStandalonePreviewGeometry(asset);
    visualPlayers.forEach((node, clipId) => {
      const found = findClip(clipId);
      const media = found ? assetMap.get(found.clip.assetId) : null;
      if (found && media) applyGeometry(node, media, found.clip);
    });
    if (previewOutline) previewOutline.hidden = !asset;
    previewGeometry = geometry ? {clip, ...geometry} : null;
    syncFrameSizeControls();
    requestAnimationFrame(updateLayerSnapGuides);
  }

  function updateCanvasEdgeSnapFeedback(node, {flash = false} = {}) {
    if (!node || node.hidden) return;
    const stage = previewStage.getBoundingClientRect();
    const frame = node.getBoundingClientRect();
    const tolerance = 3;
    const classes = [];
    if (Math.abs(frame.left - stage.left) <= tolerance) classes.push("snap-left");
    if (Math.abs(frame.right - stage.right) <= tolerance) classes.push("snap-right");
    if (Math.abs(frame.top - stage.top) <= tolerance) classes.push("snap-top");
    if (Math.abs(frame.bottom - stage.bottom) <= tolerance) classes.push("snap-bottom");
    previewStage.classList.remove("snap-top", "snap-right", "snap-bottom", "snap-left");
    if (classes.length) previewStage.classList.add(...classes);
    if (flash && classes.length) {
      setTimeout(() => previewStage.classList.remove(...classes), 320);
    }
  }

  function nudgeCanvasFrame(action) {
    const found = selectedId ? findClip(selectedId) : null;
    const asset = found ? assetMap.get(found.clip.assetId) : null;
    if (!found || !asset || !canEdit || found.track.locked || standalonePreviewAssetId) return;
    const clip = found.clip;
    const output = outputDimensions();
    const geometry = clipGeometry(asset, clip);
    const limits = {
      minX: 1 - output.width / 2 - geometry.widthPx / 2,
      maxX: output.width / 2 + geometry.widthPx / 2 - 1,
      minY: 1 - output.height / 2 - geometry.heightPx / 2,
      maxY: output.height / 2 + geometry.heightPx / 2 - 1,
    };
    let nextX = Number(clip.positionX || 0);
    let nextY = Number(clip.positionY || 0);
    if (action === "reset") { nextX = 0; nextY = 0; }
    if (action === "left") nextX -= 1;
    if (action === "right") nextX += 1;
    if (action === "up") nextY += 1;
    if (action === "down") nextY -= 1;
    const clampedX = clamp(nextX, limits.minX, limits.maxX);
    const clampedY = clamp(nextY, limits.minY, limits.maxY);
    mutate(() => {
      clip.positionX = clampedX;
      clip.positionY = clampedY;
      if (action === "reset") clip.scale = 1;
    });
    const epsilon = .5;
    const edgeClasses = [];
    if (action === "left" && Math.abs(clampedX - limits.minX) <= epsilon) edgeClasses.push("snap-left");
    if (action === "right" && Math.abs(clampedX - limits.maxX) <= epsilon) edgeClasses.push("snap-right");
    if (action === "up" && Math.abs(clampedY - limits.maxY) <= epsilon) edgeClasses.push("snap-top");
    if (action === "down" && Math.abs(clampedY - limits.minY) <= epsilon) edgeClasses.push("snap-bottom");
    if (edgeClasses.length) {
      previewStage.classList.add(...edgeClasses);
      setTimeout(() => previewStage.classList.remove(...edgeClasses), 240);
    }
    updatePreviewGeometry();
    requestAnimationFrame(() => {
      const node = visualPlayers.get(clip.id) || previewOutline;
      updateCanvasEdgeSnapFeedback(node, {flash: true});
      updateLayerSnapGuides();
    });
    renderInspector();
  }

  function beginPreviewPan(event) {
    if (standalonePreviewAssetId) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    const {clip, track} = previewClipData();
    if (!clip || !canEdit || track?.locked || event.button !== 0) return;
    event.preventDefault();
    if (!selectedIds.has(clip.id)) selectClip(clip.id);
    remember();
    const startX = event.clientX;
    const startY = event.clientY;
    const originalX = Number(clip.positionX || 0);
    const originalY = Number(clip.positionY || 0);
    const output = outputDimensions();
    const pointerId = event.pointerId;
    let changed = false;
    let finished = false;
    const clearCanvasSnapEdges = () => previewStage.classList.remove("snap-top", "snap-right", "snap-bottom", "snap-left");
    const move = next => {
      const dx = next.clientX - startX;
      const dy = next.clientY - startY;
      let nextX = originalX + dx / Math.max(1, previewStage.clientWidth) * output.width;
      let nextY = originalY - dy / Math.max(1, previewStage.clientHeight) * output.height;
      const geometry = clipGeometry(assetMap.get(clip.assetId), clip);
      const minimumX = 1 - output.width / 2 - geometry.widthPx / 2;
      const maximumX = output.width / 2 + geometry.widthPx / 2 - 1;
      const minimumY = 1 - output.height / 2 - geometry.heightPx / 2;
      const maximumY = output.height / 2 + geometry.heightPx / 2 - 1;
      nextX = clamp(nextX, minimumX, maximumX);
      nextY = clamp(nextY, minimumY, maximumY);
      clearCanvasSnapEdges();
      if (snapping && previewGeometry) {
        const edgeTolerance = 8;
        const left = output.width / 2 + nextX - geometry.widthPx / 2;
        const right = output.width / 2 + nextX + geometry.widthPx / 2;
        const top = output.height / 2 - nextY - geometry.heightPx / 2;
        const bottom = output.height / 2 - nextY + geometry.heightPx / 2;
        if (Math.abs(left) <= edgeTolerance) { nextX = geometry.widthPx / 2 - output.width / 2; previewStage.classList.add("snap-left"); }
        if (Math.abs(right - output.width) <= edgeTolerance) { nextX = output.width / 2 - geometry.widthPx / 2; previewStage.classList.add("snap-right"); }
        if (Math.abs(top) <= edgeTolerance) { nextY = output.height / 2 - geometry.heightPx / 2; previewStage.classList.add("snap-top"); }
        if (Math.abs(bottom - output.height) <= edgeTolerance) { nextY = geometry.heightPx / 2 - output.height / 2; previewStage.classList.add("snap-bottom"); }
      }
      clip.positionX = nextX;
      clip.positionY = nextY;
      changed = changed || Math.abs(dx) > 1 || Math.abs(dy) > 1;
      updatePreviewGeometry();
    };
    const finish = () => {
      if (finished) return;
      finished = true;
      clearEditorPointerFinish(finish);
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", finish);
      window.removeEventListener("pointercancel", finish);
      previewStage.onlostpointercapture = null;
      releasePointerCaptureSafely(previewStage, pointerId);
      previewStage.classList.remove("dragging");
      clearCanvasSnapEdges();
      if (!changed) historyUndo.pop();
      else { historyRedo = []; updateDirty(); updateHistoryButtons(); scheduleAutosave(); }
      renderInspector();
    };
    previewStage.classList.add("dragging");
    capturePointerSafely(previewStage, pointerId);
    previewStage.onlostpointercapture = finish;
    registerEditorPointerFinish(finish);
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", finish);
    window.addEventListener("pointercancel", finish);
  }

  function beginPreviewResize(event) {
    const handleNode = event.currentTarget;
    const handle = handleNode.dataset.previewResize || "se";
    const {clip, track} = previewClipData();
    if (!clip || !canEdit || track?.locked || standalonePreviewAssetId) return;
    event.preventDefault(); event.stopPropagation();
    if (!selectedIds.has(clip.id)) selectClip(clip.id);
    remember();
    const startX = event.clientX;
    const startY = event.clientY;
    const original = Number(clip.scale || 1);
    const pointerId = event.pointerId;
    let changed = false;
    let finished = false;
    const move = next => {
      const horizontal = handle.includes("w") ? startX - next.clientX : next.clientX - startX;
      const vertical = handle.includes("n") ? startY - next.clientY : next.clientY - startY;
      const usesHorizontal = /[ew]/.test(handle);
      const usesVertical = /[ns]/.test(handle);
      const delta = usesHorizontal && usesVertical ? (horizontal + vertical) / 2 : usesHorizontal ? horizontal : vertical;
      clip.scale = clamp(original * (1 + delta / Math.max(80, Math.min(previewStage.clientWidth, previewStage.clientHeight))), .05, 8);
      changed = changed || Math.abs(delta) > 1;
      updatePreviewGeometry();
      const range = q("[data-clip-scale-range]");
      const number = q("[data-clip-scale-number]");
      if (range) range.value = String(clip.scale);
      if (number) number.value = String(Math.round(clip.scale * 100));
    };
    const finish = () => {
      if (finished) return;
      finished = true;
      clearEditorPointerFinish(finish);
      window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", finish); window.removeEventListener("pointercancel", finish);
      handleNode.onlostpointercapture = null;
      releasePointerCaptureSafely(handleNode, pointerId);
      previewStage.classList.remove("resizing");
      if (!changed) historyUndo.pop(); else { historyRedo = []; updateDirty(); updateHistoryButtons(); scheduleAutosave(); }
      renderInspector();
    };
    previewStage.classList.add("resizing");
    capturePointerSafely(handleNode, pointerId);
    handleNode.onlostpointercapture = finish;
    registerEditorPointerFinish(finish);
    window.addEventListener("pointermove", move); window.addEventListener("pointerup", finish); window.addEventListener("pointercancel", finish);
  }

  function mediaStatus(asset) {
    if (asset.status === "READY") return `${clock(asset.durationMs || 0)} / Ready`;
    if (asset.status === "FAILED") return "Processing failed";
    return asset.status === "PROCESSING" ? "Creating proxy" : "Queued";
  }

  function mediaSize(bytes) {
    const value = Number(bytes || 0);
    if (!value) return "0 B";
    const units = ["B", "KB", "MB", "GB"];
    const index = Math.min(units.length - 1, Math.floor(Math.log(value) / Math.log(1024)));
    return `${(value / (1024 ** index)).toFixed(index ? 1 : 0)} ${units[index]}`;
  }

  function openMediaProperties(asset) {
    const dialog = q("[data-movie-media-properties]");
    const content = dialog?.querySelector("[data-movie-media-properties-content]");
    if (!dialog || !content) return;
    const rows = [
      ["Name", asset.name],
      ["Type", asset.contentType || asset.kind],
      ["Size", mediaSize(asset.size)],
      ["Resolution", asset.width && asset.height ? `${asset.width} x ${asset.height}` : "Not available yet"],
      ["Duration", asset.durationMs ? clock(asset.durationMs) : "Not available"],
      ["Status", mediaStatus(asset)],
      ["Workspace", asset.workspaceName || ""],
      ["Projects", asset.projectNames?.join(", ") || "Workspace library"],
      ["Added", asset.createdAt ? new Date(asset.createdAt).toLocaleString() : ""],
      ["Added by", asset.createdBy || ""],
      ["Asset ID", asset.id],
    ];
    content.replaceChildren(...rows.map(([label, value]) => {
      const row = document.createElement("div");
      const term = document.createElement("strong");
      const detail = document.createElement("span");
      term.textContent = label;
      detail.textContent = value;
      row.append(term, detail);
      return row;
    }));
    dialog.showModal();
  }

  function moveAssetToFolder(assetId, folderId) {
    moveAssetsToFolder([assetId], folderId);
  }

  function moveAssetsToFolder(assetIds, folderId) {
    const moving = new Set(assetIds);
    mutate(() => {
      timeline.mediaFolders.forEach(folder => {
        folder.assetIds = (folder.assetIds || []).filter(id => !moving.has(id));
      });
      const target = timeline.mediaFolders.find(folder => folder.id === folderId);
      if (target) moving.forEach(id => { if (!target.assetIds.includes(id)) target.assetIds.push(id); });
    });
    renderBin();
  }

  function renderMediaFolders() {
    if (!mediaFoldersNode) return;
    mediaFoldersNode.replaceChildren();
    const makeButton = (label, action, title) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "secondary movie-bin-folder";
      button.textContent = label;
      button.title = title || label;
      button.onclick = action;
      return button;
    };
    const folder = timeline.mediaFolders.find(item => item.id === currentMediaFolderId);
    if (folder) {
      const back = makeButton("\u21B0", () => { currentMediaFolderId = null; selectedMediaIds.clear(); renderBin(); }, "Back to Media");
      back.classList.add("movie-bin-folder-up");
      back.setAttribute("aria-label", "Back to Media");
      back.ondragover = event => { if (![...event.dataTransfer.types].some(type => ["text/asset-id", "text/asset-ids"].includes(type))) return; event.preventDefault(); back.classList.add("drop-target"); };
      back.ondragleave = () => back.classList.remove("drop-target");
      back.ondrop = event => {
        event.preventDefault(); back.classList.remove("drop-target");
        let ids = []; try { ids = JSON.parse(event.dataTransfer.getData("text/asset-ids") || "[]"); } catch (_) {}
        const single = event.dataTransfer.getData("text/asset-id"); if (!ids.length && single) ids = [single];
        if (ids.length) { moveAssetsToFolder(ids, null); currentMediaFolderId = null; renderBin(); }
      };
      mediaFoldersNode.append(back);
      const title = document.createElement("strong"); title.textContent = folder.name; mediaFoldersNode.append(title);
    }
    if (!canEdit) return;
    const create = document.createElement("button");
    create.type = "button"; create.className = "icon-button secondary"; create.textContent = "+"; create.title = "Create folder";
    create.onclick = () => openMediaFolderDialog("create");
    mediaFoldersNode.append(create);
    if (currentMediaFolderId) {
      const rename = document.createElement("button"); rename.type = "button"; rename.className = "icon-button secondary"; rename.textContent = "R"; rename.title = "Rename folder"; rename.onclick = () => openMediaFolderDialog("rename");
      const remove = document.createElement("button"); remove.type = "button"; remove.className = "icon-button secondary"; remove.innerHTML = "&times;"; remove.title = "Delete folder"; remove.onclick = () => openMediaFolderDialog("delete");
      mediaFoldersNode.append(rename, remove);
    }
  }

  function mediaFolderLabel(value) {
    return String(value || "Folder");
  }

  function reorderMediaAssets(movingIds, beforeId) {
    const moving = [...new Set(movingIds)].filter(id => timeline.mediaAssetIds.includes(id));
    if (!moving.length || moving.includes(beforeId)) return;
    mutate(() => {
      const order = timeline.mediaOrder.filter(id => !moving.includes(id));
      const index = Math.max(0, order.indexOf(beforeId));
      order.splice(index, 0, ...moving);
      timeline.mediaOrder = order;
    });
    const sortControl = q("[data-bin-sort]");
    if (sortControl) {
      sortControl.value = "custom";
      sessionStorage.setItem(mediaSortKey, "custom");
    }
    renderBin();
  }

  function openMediaFolderDialog(mode) {
    if (!folderDialog) return;
    const folder = timeline.mediaFolders.find(item => item.id === currentMediaFolderId);
    folderDialog.dataset.mode = mode;
    folderDialog.querySelector("[data-media-folder-title]").textContent = mode === "create" ? "Create media folder" : mode === "rename" ? "Rename media folder" : "Delete media folder";
    const field = folderDialog.querySelector("[data-media-folder-name-field]");
    const input = folderDialog.querySelector("[data-media-folder-name]");
    const message = folderDialog.querySelector("[data-media-folder-message]");
    field.hidden = mode === "delete";
    message.hidden = mode !== "delete";
    message.textContent = mode === "delete" ? `Delete “${folder?.name || "folder"}”? Media files remain in this MC Project.` : "";
    input.value = mode === "rename" ? folder?.name || "" : "";
    folderDialog.showModal();
    if (mode !== "delete") requestAnimationFrame(() => input.focus());
  }

  function renderBin() {
    assetMap.clear();
    libraryAssets.forEach(item => assetMap.set(item.id, item));
    assets.forEach(item => assetMap.set(item.id, item));
    bin.replaceChildren();
    bin.dataset.view = mediaView;
    qa("[data-media-view]").forEach(button => button.classList.toggle("active", button.dataset.mediaView === mediaView));
    const kind = q("[data-bin-kind]")?.value || "ALL";
    const sort = q("[data-bin-sort]")?.value || "latest";
    const used = new Set(timeline.tracks.flatMap(track => track.clips.map(clip => clip.assetId)));
    let montageMedia = [...assetMap.values()].filter(asset => timeline.mediaAssetIds.includes(asset.id) && (kind === "ALL" || asset.kind === kind) && (!q("[data-bin-unused]")?.checked || !used.has(asset.id)));
    const filedIds = new Set(timeline.mediaFolders.flatMap(folder => folder.assetIds || []));
    if (currentMediaFolderId) {
      const folder = timeline.mediaFolders.find(item => item.id === currentMediaFolderId);
      if (!folder) currentMediaFolderId = null;
      else montageMedia = montageMedia.filter(asset => folder.assetIds.includes(asset.id));
    } else montageMedia = montageMedia.filter(asset => !filedIds.has(asset.id));
    if (sort === "custom") {
      const order = new Map(timeline.mediaOrder.map((id, index) => [id, index]));
      montageMedia.sort((a, b) => (order.get(a.id) ?? Number.MAX_SAFE_INTEGER) - (order.get(b.id) ?? Number.MAX_SAFE_INTEGER));
    } else if (sort === "alpha") montageMedia.sort((a, b) => a.name.localeCompare(b.name));
    else if (sort === "duration") montageMedia.sort((a, b) => Number(b.durationMs || 0) - Number(a.durationMs || 0));
    else montageMedia.sort((a, b) => String(b.createdAt || "").localeCompare(String(a.createdAt || "")));
    if (!currentMediaFolderId) timeline.mediaFolders.forEach(folder => {
      const tile = document.createElement("div");
      tile.className = "movie-bin-folder-tile";
      tile.dataset.folderId = folder.id;
      tile.classList.toggle("media-selected", selectedMediaFolderId === folder.id);
      tile.innerHTML = "<div class=\"movie-folder-art\"></div><strong></strong><small></small>";
      tile.querySelector("strong").textContent = mediaFolderLabel(folder.name);
      tile.querySelector("strong").title = folder.name;
      tile.querySelector("small").textContent = String(folder.assetIds?.length || 0);
      tile.title = `Double-click to open ${folder.name}`;
      tile.onclick = event => {
        event.stopPropagation();
        selectedMediaFolderId = folder.id;
        selectedMediaIds.clear();
        renderMediaSelection();
      };
      tile.oncontextmenu = event => {
        event.preventDefault(); event.stopPropagation();
        selectedMediaFolderId = folder.id;
        selectedMediaIds.clear();
        mediaSelectionActive = true;
        mediaContextAsset = null;
        mediaContextFolderId = folder.id;
        renderMediaSelection();
        setMediaContextMode("item");
        mediaContext.style.left = `${Math.min(event.clientX, innerWidth - 190)}px`;
        mediaContext.style.top = `${Math.min(event.clientY, innerHeight - 190)}px`;
        mediaContext.hidden = false;
      };
      tile.ondblclick = () => { currentMediaFolderId = folder.id; selectedMediaIds.clear(); renderBin(); };
      tile.ondragover = event => { if (![...event.dataTransfer.types].some(type => ["text/asset-id", "text/asset-ids"].includes(type))) return; event.preventDefault(); tile.classList.add("drop-target"); };
      tile.ondragleave = () => tile.classList.remove("drop-target");
      tile.ondrop = event => {
        event.preventDefault(); tile.classList.remove("drop-target");
        let ids = []; try { ids = JSON.parse(event.dataTransfer.getData("text/asset-ids") || "[]"); } catch (_) {}
        const single = event.dataTransfer.getData("text/asset-id"); if (!ids.length && single) ids = [single];
        if (ids.length) moveAssetsToFolder(ids, folder.id);
      };
      bin.append(tile);
    });
    montageMedia.forEach(asset => {
      const item = document.createElement("div");
      const visual = document.createElement("div");
      const copy = document.createElement("div");
      const name = document.createElement("strong");
      const status = document.createElement("span");
      item.className = "movie-bin-item";
      item.draggable = canEdit && asset.status === "READY";
      item.dataset.assetId = asset.id;
      item.classList.toggle("media-selected", selectedMediaIds.has(asset.id));
      item.classList.toggle("previewing", standalonePreviewAssetId === asset.id);
      item.setAttribute("aria-selected", standalonePreviewAssetId === asset.id ? "true" : "false");
      item.title = [
        asset.width && asset.height ? `${asset.width} x ${asset.height}` : "Processing metadata",
        mediaSize(asset.size),
        asset.workspaceName || "",
        asset.projectNames?.join(", ") || "Workspace library",
      ].filter(Boolean).join(" / ");
      visual.className = "movie-bin-visual";
      copy.className = "movie-bin-copy";
      name.textContent = asset.name;
      name.title = asset.name;
      status.className = `movie-media-status${asset.status === "FAILED" ? " failed" : ""}`;
      status.textContent = mediaStatus(asset);
      if (asset.thumbnailUrl || asset.waveformUrl) {
        const image = document.createElement("img");
        image.src = asset.thumbnailUrl || asset.waveformUrl;
        image.alt = "";
        image.draggable = false;
        visual.append(image);
      } else visual.textContent = asset.kind;
      const kindBadge = document.createElement("i");
      kindBadge.className = "movie-media-kind-badge";
      kindBadge.dataset.kind = asset.kind || "FILE";
      kindBadge.textContent = asset.kind === "VIDEO" ? "\u25b6" : asset.kind === "AUDIO" ? "\u266a" : asset.kind === "IMAGE" ? "\u25a3" : "T";
      kindBadge.title = asset.kind === "VIDEO" ? "Video" : asset.kind === "AUDIO" ? "Audio" : asset.kind === "IMAGE" ? "Photo" : "Text";
      visual.append(kindBadge);
      if (used.has(asset.id)) {
        const dot = document.createElement("i"); dot.className = "movie-bin-used-dot"; dot.title = "Used on timeline"; visual.append(dot);
      }
      if (asset.status === "QUEUED" || asset.status === "PROCESSING") {
        const processing = document.createElement("i");
        processing.className = "movie-media-processing";
        processing.title = "Media is being processed";
        visual.append(processing);
      }
      copy.append(name, status);
      if (asset.error) {
        const detail = document.createElement("span");
        detail.textContent = asset.error;
        detail.title = asset.error;
        copy.append(detail);
      }
      if (asset.status === "FAILED" && canEdit) {
        const retry = document.createElement("button");
        retry.type = "button";
        retry.className = "secondary movie-retry";
        retry.textContent = "Retry";
        retry.onclick = async () => {
          retry.disabled = true;
          try { await requestJson(asset.retryUrl, {method: "POST", headers: {"X-CSRFToken": csrfToken()}}); }
          catch (error) { toast(error.message, "error"); }
          await refreshMedia();
        };
        copy.append(retry);
      }
      item.append(visual, copy);
      item.addEventListener("dragstart", event => {
        if (!item.draggable) return;
        if (!selectedMediaIds.has(asset.id)) selectedMediaIds = new Set([asset.id]);
        const ids = [...selectedMediaIds];
        event.dataTransfer.setData("application/x-lexamora-media", "1");
        event.dataTransfer.setData("text/asset-id", asset.id);
        event.dataTransfer.setData("text/asset-ids", JSON.stringify(ids));
        event.dataTransfer.effectAllowed = "copyMove";
      });
      item.addEventListener("dragover", event => {
        if (![...event.dataTransfer.types].includes("application/x-lexamora-media")) return;
        event.preventDefault();
        item.classList.add("media-order-target");
        event.dataTransfer.dropEffect = "move";
      });
      item.addEventListener("dragleave", () => item.classList.remove("media-order-target"));
      item.addEventListener("drop", event => {
        item.classList.remove("media-order-target");
        if (![...event.dataTransfer.types].includes("application/x-lexamora-media")) return;
        event.preventDefault(); event.stopPropagation();
        let ids = [];
        try { ids = JSON.parse(event.dataTransfer.getData("text/asset-ids") || "[]"); } catch (_) {}
        if (!ids.length) ids = [event.dataTransfer.getData("text/asset-id")].filter(Boolean);
        reorderMediaAssets(ids, asset.id);
      });
      item.addEventListener("click", event => {
        mediaSelectionActive = true;
        selectedMediaFolderId = null;
        if (event.ctrlKey || event.metaKey || event.shiftKey) {
          if (selectedMediaIds.has(asset.id)) selectedMediaIds.delete(asset.id); else selectedMediaIds.add(asset.id);
        } else selectedMediaIds = new Set([asset.id]);
        renderMediaSelection();
        previewAsset(asset);
      });
      item.addEventListener("contextmenu", event => {
        event.preventDefault();
        event.stopPropagation();
        mediaSelectionActive = true;
        if (!selectedMediaIds.has(asset.id)) selectedMediaIds = new Set([asset.id]);
        renderMediaSelection();
        mediaContextAsset = asset;
        setMediaContextMode("item");
        mediaContext.style.left = `${Math.min(event.clientX, innerWidth - 190)}px`;
        mediaContext.style.top = `${Math.min(event.clientY, innerHeight - 190)}px`;
        mediaContext.hidden = false;
      });
      item.addEventListener("dblclick", () => {
        if (!canEdit || asset.status !== "READY") return;
        const trackKind = asset.kind === "AUDIO" ? "AUDIO" : "VIDEO";
        let track = timeline.tracks.find(value => value.kind === trackKind && !value.locked);
        if (!track) track = addTrack(trackKind);
        addClip(track, asset.id, timelineEnd());
      });
      bin.append(item);
    });
    pendingUploads.forEach(upload => {
      const item = document.createElement("div");
      item.className = "movie-bin-item movie-bin-uploading";
      item.innerHTML = '<div class="movie-bin-visual"></div><div class="movie-bin-copy"><strong></strong><span>Uploading <b>0%</b></span><progress max="100" value="0"></progress></div>';
      item.querySelector("strong").textContent = upload.name;
      item.querySelector("b").textContent = `${upload.progress}%`;
      item.querySelector("progress").value = upload.progress;
      bin.prepend(item);
    });
    if (!montageMedia.length && !pendingUploads.length && (currentMediaFolderId || !timeline.mediaFolders.length)) bin.insertAdjacentHTML("beforeend", '<p class="empty">Open + to add Project or Workspace media</p>');
    renderMediaFolders();
  }

  function renderMediaSelection() {
    qa(".movie-bin-item[data-asset-id]").forEach(item => {
      const selected = selectedMediaIds.has(item.dataset.assetId);
      item.classList.toggle("media-selected", selected);
      item.setAttribute("aria-selected", selected ? "true" : "false");
    });
    qa(".movie-bin-folder-tile").forEach(item => item.classList.toggle("media-selected", item.dataset.folderId === selectedMediaFolderId));
  }

  function selectedMediaAssets() {
    return [...selectedMediaIds].map(id => assetMap.get(id)).filter(Boolean);
  }

  async function copySelectedMedia() {
    const selected = selectedMediaAssets();
    if (!selected.length) return;
    mediaClipboardIds = selected.map(asset => asset.id);
    mediaClipboardMode = "copy";
    const urls = selected.map(asset => asset.originalUrl ? new URL(asset.originalUrl, location.href).href : "").filter(Boolean);
    if (urls.length) await navigator.clipboard.writeText(urls.join("\n"));
    toast(`${selected.length} media item${selected.length === 1 ? "" : "s"} copied`);
  }

  function pasteSelectedMedia() {
    const ids = mediaClipboardIds.filter(id => assetMap.has(id));
    if (!ids.length) return toast("Nothing to paste", "error");
    mutate(() => {
      ids.forEach(id => {
        if (!timeline.mediaAssetIds.includes(id)) timeline.mediaAssetIds.push(id);
        if (!timeline.mediaOrder.includes(id)) timeline.mediaOrder.push(id);
      });
      const folder = timeline.mediaFolders.find(item => item.id === currentMediaFolderId);
      if (folder) ids.forEach(id => { if (!folder.assetIds.includes(id)) folder.assetIds.push(id); });
      else timeline.mediaFolders.forEach(item => { item.assetIds = item.assetIds.filter(id => !ids.includes(id)); });
    });
    selectedMediaIds = new Set(ids);
    if (mediaClipboardMode === "cut") {
      mediaClipboardIds = [];
      mediaClipboardMode = "copy";
    }
    renderBin();
  }

  function removeSelectedMedia() {
    const ids = new Set(selectedMediaAssets().map(asset => asset.id));
    if (!ids.size) return;
    const affectedClips = timeline.tracks.flatMap(track => track.clips.filter(clip => ids.has(clip.assetId)));
    const applyRemoval = () => mutate(() => {
      timeline.mediaAssetIds = timeline.mediaAssetIds.filter(id => !ids.has(id));
      timeline.mediaOrder = timeline.mediaOrder.filter(id => !ids.has(id));
      timeline.mediaFolders.forEach(folder => { folder.assetIds = folder.assetIds.filter(id => !ids.has(id)); });
      timeline.tracks.forEach(track => { track.clips = track.clips.filter(clip => !ids.has(clip.assetId)); });
    });
    const finish = () => {
      selectedMediaIds.clear();
      selectedIds.clear();
      selectedId = null;
      mediaSelectionActive = false;
      renderBin();
      renderTimeline();
      renderInspector();
      syncPlayers(false, true);
    };
    if (!affectedClips.length) {
      applyRemoval();
      finish();
      return;
    }
    const dialog = q("[data-media-remove-dialog]");
    const message = dialog?.querySelector("[data-media-remove-message]");
    if (!dialog || !message) return;
    message.textContent = `${ids.size} selected media item${ids.size === 1 ? " is" : "s are"} used by ${affectedClips.length} timeline clip${affectedClips.length === 1 ? "" : "s"}. Remove the media and those clips?`;
    dialog.showModal();
    dialog.querySelector("[data-media-remove-confirm]").onclick = () => {
      dialog.close();
      applyRemoval();
      finish();
    };
  }

  function showSelectedMediaProperties() {
    const selected = selectedMediaAssets();
    if (!selected.length) {
      const dialog = q("[data-movie-media-properties]");
      const content = dialog?.querySelector("[data-movie-media-properties-content]");
      if (!dialog || !content) return;
      const visible = qa(".movie-bin-item[data-asset-id]")
        .map(item => assetMap.get(item.dataset.assetId))
        .filter(Boolean);
      const folders = currentMediaFolderId ? [] : (timeline.mediaFolders || []);
      const activeFolder = (timeline.mediaFolders || []).find(
        folder => String(folder.id) === String(currentMediaFolderId || ""),
      );
      const totalSize = visible.reduce((sum, asset) => sum + Number(asset.size || 0), 0);
      const rows = [
        ["Location", activeFolder?.name || "Media"],
        ["Visible files", String(visible.length)],
        ["Folders", String(folders.length)],
        ["Visible size", mediaSize(totalSize)],
        ["Sort", q("[data-bin-sort]")?.selectedOptions?.[0]?.textContent || "Newest"],
      ];
      const properties = document.createElement("dl");
      properties.className = "asset-properties";
      rows.forEach(([name, value]) => {
        const term = document.createElement("dt");
        const description = document.createElement("dd");
        term.textContent = name;
        description.textContent = value;
        properties.append(term, description);
      });
      content.replaceChildren(properties);
      dialog.showModal();
      return;
    }
    if (selected.length === 1) return openMediaProperties(selected[0]);
    const dialog = q("[data-movie-media-properties]");
    const content = dialog?.querySelector("[data-movie-media-properties-content]");
    if (!dialog || !content) return;
    const heading = document.createElement("p");
    heading.textContent = `${selected.length} media items selected`;
    const list = document.createElement("ul");
    selected.forEach(asset => {
      const item = document.createElement("li");
      item.textContent = `${asset.name} (${asset.kind}, ${mediaSize(asset.size)})`;
      list.append(item);
    });
    content.replaceChildren(heading, list);
    dialog.showModal();
  }

  async function refreshMedia() {
    try {
      const data = await requestJson(root.dataset.mediaUrl, {headers: {"X-Requested-With": "XMLHttpRequest"}});
      assets = data.items || [];
      libraryAssets = data.libraryItems || data.items || [];
      renderBin();
      renderTimeline();
      if (!playing) syncPlayers(false, true);
      updateDirty();
      renderLibrary();
      clearTimeout(mediaPoll);
      if (libraryAssets.some(item => ["QUEUED", "PROCESSING"].includes(item.status))) mediaPoll = setTimeout(refreshMedia, 2500);
    } catch (error) { previewStatus.textContent = error.message; }
  }

  const renderDate = value => value ? new Date(value).toLocaleString([], {dateStyle: "medium", timeStyle: "short"}) : "";

  async function renderAction(job, action) {
    try {
      const data = await requestJson(job.actionUrl, {method: "POST", headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()}, body: JSON.stringify({action})});
      const index = renderJobs.findIndex(item => item.id === data.id);
      if (index >= 0) renderJobs[index] = data;
      renderRenderJobs();
      scheduleRenderPoll();
    } catch (error) { toast(error.message, "error"); }
  }

  function renderRenderJobs() {
    if (!renderList) return;
    renderList.replaceChildren();
    if (!renderJobs.length) {
      renderList.innerHTML = '<p class="muted">No exports yet</p>';
      return;
    }
    renderJobs.forEach(job => {
      const row = document.createElement("div");
      const copy = document.createElement("div");
      const title = document.createElement("strong");
      const details = document.createElement("span");
      const actions = document.createElement("div");
      row.className = "movie-render-row";
      copy.className = "movie-render-copy";
      actions.className = "movie-render-actions";
      title.textContent = job.title;
      details.textContent = `${job.profileLabel} / ${job.audioProfileLabel} ${job.targetLufs} LUFS / ${job.width}x${job.height} / ${job.statusLabel} / ${renderDate(job.createdAt)}`;
      copy.append(title, details);
      if (["QUEUED", "RUNNING"].includes(job.status)) {
        const progress = document.createElement("div");
        progress.className = "movie-render-progress";
        progress.innerHTML = `<i style="width:${job.progress || 0}%"></i>`;
        copy.append(progress);
      }
      if (job.error) {
        const error = document.createElement("span");
        error.className = "movie-render-error";
        error.textContent = job.error;
        copy.append(error);
      }
      if (job.downloadUrl) {
        const download = document.createElement("a");
        download.className = "button secondary";
        download.href = job.downloadUrl;
        download.textContent = "Download";
        actions.append(download);
      }
      if (job.downloadUrl && canEdit) {
        const attach = document.createElement("button");
        attach.type = "button";
        attach.className = "secondary";
        attach.textContent = job.attachedToProject ? "Remove from project" : "Add to project";
        attach.title = job.attachedToProject ? "Keep the render in the workspace only" : "Attach this render to the current project";
        attach.onclick = () => renderAction(job, job.attachedToProject ? "detach" : "attach");
        actions.append(attach);
      }
      if (job.canCancel && canExport) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "secondary";
        button.textContent = "Cancel";
        button.onclick = () => renderAction(job, "cancel");
        actions.append(button);
      }
      if (job.canRetry && canExport) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "secondary";
        button.textContent = "Retry";
        button.onclick = () => renderAction(job, "retry");
        actions.append(button);
      }
      row.append(copy, actions);
      renderList.append(row);
    });
  }

  async function refreshRenders() {
    if (!root.dataset.renderUrl) return;
    try {
      const completedBefore = new Set(renderJobs.filter(job => job.status === "SUCCEEDED").map(job => job.id));
      const data = await requestJson(`${root.dataset.renderUrl}?timelineId=${encodeURIComponent(timelineId)}`, {headers: {"X-Requested-With": "XMLHttpRequest"}});
      renderJobs = data.items || [];
      renderRenderJobs();
      if (renderJobs.some(job => job.status === "SUCCEEDED" && !completedBefore.has(job.id))) await refreshMedia();
    } catch (error) { toast(error.message, "error"); }
    scheduleRenderPoll();
  }

  function scheduleRenderPoll() {
    clearTimeout(renderPoll);
    if (renderJobs.some(job => ["QUEUED", "RUNNING"].includes(job.status))) renderPoll = setTimeout(refreshRenders, document.hidden ? 5000 : 1800);
  }

  async function saveTimeline(silent = false) {
    if (!canEdit) return true;
    if (savePromise) { saveAgain = true; return savePromise; }
    clearTimeout(autosaveTimer);
    normalizeTimeline();
    stateNode.textContent = "Saving";
    stateNode.classList.remove("error");
    const requestedSignature = signature();
    const scrollState = {left: timelineScroll.scrollLeft, top: timelineScroll.scrollTop};
    savePromise = (async () => { try {
      const data = await requestJson(root.dataset.saveUrl, {
        method: "POST",
        headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
        body: JSON.stringify({timelineId, title: q("[data-movie-title]").value, aspectRatio: q("[data-movie-ratio]").value, resolution: q("[data-movie-resolution]").value, fps: Number(q("[data-movie-fps]").value), timeline}),
      });
      if (data.repaired && data.timeline) {
        timeline = data.timeline;
        normalizeTimeline();
        renderTimeline();
        renderInspector();
        requestAnimationFrame(() => { timelineScroll.scrollLeft = scrollState.left; timelineScroll.scrollTop = scrollState.top; });
      }
      savedSignature = requestedSignature;
      dirty = signature() !== requestedSignature;
      stateNode.textContent = dirty ? "Unsaved changes" : "Saved";
      updateHistoryButtons();
      if (!silent) toast("Timeline saved");
      if (data.repaired) toast("Clip lengths were adjusted to their source files");
      (data.warnings || []).forEach(message => toast(message, "warning"));
      if (historyPanel && !historyPanel.hidden) await loadHistory();
      return true;
    } catch (error) {
      stateNode.textContent = error.message || "Save failed";
      stateNode.classList.add("error");
      toast(stateNode.textContent, "error");
      return false;
    } })();
    const result = await savePromise;
    savePromise = null;
    if (saveAgain) { saveAgain = false; scheduleAutosave(100); }
    return result;
  }

  async function queueRender() {
    const button = q("[data-render-start]");
    button.disabled = true;
    button.textContent = "Saving";
    if (canEdit && !await saveTimeline(true)) {
      button.disabled = false;
      button.textContent = "Save and render MP4";
      return;
    }
    button.textContent = "Queueing";
    try {
      const data = await requestJson(root.dataset.renderUrl, {
        method: "POST",
        headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
        body: JSON.stringify({timelineId, title: q("[data-movie-title]").value, profile: q("[data-render-profile]").value, audioProfile: q("[data-render-audio-profile]").value, targetLufs: Number(q("[data-render-target-lufs]").value)}),
      });
      renderJobs.unshift(data);
      renderRenderJobs();
      renderPanel.hidden = false;
      toast("MP4 render queued");
      scheduleRenderPoll();
    } catch (error) { toast(error.message, "error"); }
    finally { button.disabled = false; button.textContent = "Save and render MP4"; }
  }

  async function queueClipAsset() {
    const found = findClip(selectedId);
    if (!found || selectedIds.size !== 1) return;
    if (canEdit && !await saveTimeline(true)) return;
    try {
      const data = await requestJson(root.dataset.renderUrl, {
        method: "POST",
        headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
        body: JSON.stringify({
          timelineId,
          clipIds: [found.clip.id],
          title: `${found.clip.name || "Edited clip"} media`,
          profile: q("[data-render-profile]")?.value || "DRAFT_720",
          audioProfile: q("[data-render-audio-profile]")?.value || "CLEAN_SPEECH",
          targetLufs: Number(q("[data-render-target-lufs]")?.value || -16),
        }),
      });
      renderJobs.unshift(data);
      renderRenderJobs();
      renderPanel.hidden = false;
      toast("Edited clip queued as reusable media");
      scheduleRenderPoll();
    } catch (error) { toast(error.message, "error"); }
  }

  function timelineCandidates(excluded = new Set()) {
    const values = [0, playheadMs];
    timeline.tracks.forEach(track => track.clips.forEach(clip => {
      if (!excluded.has(clip.id)) values.push(clip.start, clip.start + clip.duration);
    }));
    return values;
  }

  function snapTime(value, excludeId) {
    const precise = Math.max(0, quantize(value));
    snapGuide.hidden = true;
    if (!snapping) return precise;
    const threshold = Math.max(PRECISION_MS, 7 / zoom * 1000);
    let closest = null;
    let distance = threshold + 1;
    timelineCandidates(new Set(excludeId ? [excludeId] : [])).forEach(candidate => {
      const nextDistance = Math.abs(candidate - precise);
      if (nextDistance < distance) { closest = candidate; distance = nextDistance; }
    });
    if (closest === null || distance > threshold) return precise;
    snapGuide.style.left = `${closest / 1000 * zoom}px`;
    snapGuide.hidden = false;
    return quantize(closest);
  }

  function snapGroupDelta(rawDelta, items) {
    const precise = quantize(rawDelta);
    snapGuide.hidden = true;
    const minimumStart = Math.min(...items.map(item => item.original.start));
    const maximumEnd = Math.max(...items.map(item => item.original.start + item.original.duration));
    const bounded = Math.max(-minimumStart, precise);
    if (!snapping) return bounded;
    const excluded = new Set(items.map(item => item.clip.id));
    const threshold = Math.max(PRECISION_MS, 7 / zoom * 1000);
    let best = null;
    timelineCandidates(excluded).forEach(candidate => {
      [minimumStart + bounded, maximumEnd + bounded].forEach(edge => {
        const correction = candidate - edge;
        const distance = Math.abs(correction);
        if (distance <= threshold && (!best || distance < best.distance)) best = {candidate, correction, distance};
      });
    });
    if (!best) return bounded;
    snapGuide.style.left = `${best.candidate / 1000 * zoom}px`;
    snapGuide.hidden = false;
    return Math.max(-minimumStart, quantize(bounded + best.correction));
  }

  function setPlayhead(value, sync = true, autoSelect = true) {
    playheadMs = clamp(quantize(value), 0, Math.max(0, visibleDuration()));
    playheadNode.style.left = `${playheadMs / 1000 * zoom}px`;
    if (playheadLabel) playheadLabel.textContent = clock(playheadMs);
    previewTime.textContent = `${clock(playheadMs)} / ${clock(timelineEnd())}`;
    previewScrub.max = Math.max(PRECISION_MS, visibleDuration());
    previewScrub.value = Math.min(playheadMs, Number(previewScrub.max));
    if (autoSelect) {
      const current = findClip(selectedId);
      const currentIsActive = current && playheadMs >= current.clip.start && playheadMs < current.clip.start + current.clip.duration;
      const top = currentIsActive ? selectedId : activeVisualClips(playheadMs).at(-1)?.clip.id || null;
      if (top !== selectedId && selectedIds.size <= 1) {
        selectedId = top;
        selectedIds = new Set(top ? [top] : []);
        qa(".movie-clip").forEach(node => node.classList.toggle("selected", node.dataset.clipId === top));
        renderInspector();
      }
    }
    const inspectorPlayhead = q("[data-inspector-playhead]");
    if (inspectorPlayhead) inspectorPlayhead.value = (playheadMs / 1000).toFixed(3);
    if (sync && !playing) syncPlayers(false, true);
  }

  function beginPlayheadGesture(event) {
    if (event.button !== 0) return;
    leaveStandalonePreview();
    event.preventDefault(); event.stopPropagation(); stopPlayback();
    const update = next => {
      const bounds = timelineScroll.getBoundingClientRect();
      if (next.clientX > bounds.right - 24) timelineScroll.scrollLeft += 18;
      else if (next.clientX < bounds.left + 24) timelineScroll.scrollLeft = Math.max(0, timelineScroll.scrollLeft - 18);
      setPlayhead((next.clientX - bounds.left + timelineScroll.scrollLeft) / zoom * 1000);
    };
    update(event);
    const finish = () => {
      window.removeEventListener("pointermove", update);
      window.removeEventListener("pointerup", finish);
      playheadNode.classList.remove("dragging");
    };
    playheadNode.classList.add("dragging");
    window.addEventListener("pointermove", update);
    window.addEventListener("pointerup", finish);
  }

  function previewAsset(asset) {
    if (!asset || !["VIDEO", "IMAGE"].includes(asset.kind) || asset.status !== "READY") return;
    stopPlayback();
    visualPlayers.forEach(player => { if (player.pause) player.pause(); player.remove(); });
    visualPlayers.clear();
    audioPlayers.forEach(player => player.pause());
    audioPlayers.clear();
    previewLayers?.replaceChildren();
    selectedId = null;
    selectedIds.clear();
    standalonePreviewAssetId = asset.id;
    renderMediaPreviewSelection();
    renderTimeline();
    renderInspector();
    preview.hidden = asset.kind !== "VIDEO";
    previewImage.hidden = asset.kind !== "IMAGE";
    if (previewOutline) previewOutline.hidden = false;
    previewEmpty.hidden = true;
    const source = asset.proxyUrl || asset.originalUrl;
    if (asset.kind === "IMAGE") {
      preview.pause();
      preview.removeAttribute("src");
      previewImage.dataset.assetId = asset.id;
      previewImage.dataset.clipId = "standalone";
      previewImage.src = source;
    } else {
      previewImage.removeAttribute("src");
      preview.dataset.assetId = asset.id;
      preview.dataset.clipId = "standalone";
      preview.src = source;
      preview.load();
      const start = () => preview.play().catch(() => {});
      if (preview.readyState >= 2) start();
      else preview.addEventListener("loadeddata", start, {once: true});
    }
    updatePreviewGeometry(asset);
    previewStatus.textContent = `Previewing ${asset.name}`;
  }

  function clipVolumeAt(clip, timelinePosition) {
    const local = clamp(timelinePosition - clip.start, 0, clip.duration);
    let value = Number(clip.volume ?? 1);
    const points = [{time: 0, value}, ...(clip.volumeKeyframes || []), {time: clip.duration, value}]
      .sort((a, b) => a.time - b.time);
    for (let index = 1; index < points.length; index += 1) {
      if (local <= points[index].time) {
        const left = points[index - 1]; const right = points[index];
        const span = Math.max(1, right.time - left.time);
        value = left.value + (right.value - left.value) * ((local - left.time) / span);
        break;
      }
    }
    if (clip.fadeIn > 0) value *= clamp(local / clip.fadeIn, 0, 1);
    if (clip.fadeOut > 0) value *= clamp((clip.duration - local) / clip.fadeOut, 0, 1);
    return clamp(value, 0, 2);
  }

  function setPlayer(player, active, shouldPlay, force = false) {
    if (!active) {
      player.pause();
      player.dataset.clipId = "";
      if (player === preview) {
        preview.hidden = true;
        if (previewOutline) previewOutline.hidden = true;
        previewEmpty.hidden = false;
      }
      return;
    }
    const {clip, track} = active;
    const asset = assetMap.get(clip.assetId);
    if (!asset) return;
    const source = asset.proxyUrl || asset.originalUrl;
    const target = Math.max(0, (clip.sourceStart + (playheadMs - clip.start) * Number(clip.speed || 1)) / 1000);
    const changed = player.dataset.assetId !== asset.id || player.dataset.clipId !== clip.id;
    const seek = () => {
      const maximum = Number.isFinite(player.duration) ? Math.max(0, player.duration - 0.01) : target;
      const safeTarget = clamp(target, 0, maximum);
      if (force || changed || Math.abs((player.currentTime || 0) - safeTarget) > (shouldPlay ? 0.65 : 0.04)) player.currentTime = safeTarget;
      player.volume = track?.muted ? 0 : Math.min(1, clipVolumeAt(clip, playheadMs));
      player.playbackRate = clamp(Number(clip.speed || 1), .1, 8);
      if (shouldPlay && player.paused) player.play().catch(error => { if (player === preview) previewStatus.textContent = error.message; });
    };
    player.dataset.assetId = asset.id;
    player.dataset.clipId = clip.id;
    if (player === preview) {
      preview.hidden = false;
      if (previewOutline) previewOutline.hidden = false;
      previewEmpty.hidden = true;
      updatePreviewGeometry(asset);
    }
    if (changed || player.currentSrc !== new URL(source, location.href).href) {
      player.src = source;
      player.load();
      player.addEventListener("loadedmetadata", seek, {once: true});
    } else seek();
  }

  function syncVisualLayers(shouldPlay, force = false) {
    if (!previewLayers || standalonePreviewAssetId) return;
    preview.hidden = true;
    let active = activeVisualClips(playheadMs);
    const selected = findClip(selectedId);
    if (selected && ["VIDEO", "IMAGE"].includes(clipKind(selected.clip)) && !active.some(item => item.clip.id === selected.clip.id)) {
      const trackIndex = timeline.tracks.indexOf(selected.track);
      active.push({...selected, trackIndex, clipIndex: selected.track.clips.indexOf(selected.clip), selectedOutside: true});
    }
    const activeIds = new Set(active.map(item => item.clip.id));
    active.forEach((item, index) => {
      const asset = assetMap.get(item.clip.assetId);
      if (!asset) return;
      let node = visualPlayers.get(item.clip.id);
      const expectedTag = asset.kind === "IMAGE" ? "IMG" : "VIDEO";
      if (node && node.tagName !== expectedTag) { node.remove(); visualPlayers.delete(item.clip.id); node = null; }
      let isNew = false;
      if (!node) {
        node = document.createElement(asset.kind === "IMAGE" ? "img" : "video");
        node.className = "movie-preview-layer";
        node.dataset.clipId = item.clip.id;
        if (node.tagName === "VIDEO") { node.playsInline = true; node.preload = "auto"; node.muted = true; }
        node.addEventListener("pointerdown", event => {
          if (standalonePreviewAssetId || event.button !== 0) return;
          event.stopPropagation();
          selectClip(node.dataset.clipId, false, event.ctrlKey || event.metaKey);
          if (!(event.ctrlKey || event.metaKey)) beginPreviewPan(event);
        });
        visualPlayers.set(item.clip.id, node);
        isNew = true;
      }
      const source = asset.proxyUrl || asset.originalUrl;
      if (node.dataset.assetId !== asset.id) {
        node.dataset.assetId = asset.id;
        node.src = source;
        if (node.tagName === "VIDEO") node.load();
      }
      // Selection must not change the rendered layer order. Track order is authoritative.
      node.style.zIndex = String(index + 1);
      node.classList.toggle("selected-layer", item.clip.id === selectedId);
      applyGeometry(node, asset, item.clip);
      if (isNew) previewLayers.append(node);
      if (node.tagName === "VIDEO") {
        const at = item.selectedOutside ? item.clip.start : playheadMs;
        const target = Math.max(0, (item.clip.sourceStart + (at - item.clip.start) * Number(item.clip.speed || 1)) / 1000);
        const seek = () => {
          const maximum = Number.isFinite(node.duration) ? Math.max(0, node.duration - .01) : target;
          if (force || Math.abs((node.currentTime || 0) - target) > (shouldPlay ? .65 : .04)) node.currentTime = clamp(target, 0, maximum);
          node.playbackRate = clamp(Number(item.clip.speed || 1), .1, 8);
          if (shouldPlay && !item.selectedOutside && node.paused) node.play().catch(error => { previewStatus.textContent = error.message; });
          else if (!shouldPlay || item.selectedOutside) node.pause();
        };
        if (node.readyState < 1) node.addEventListener("loadedmetadata", seek, {once: true}); else seek();
      }
    });
    [...visualPlayers.entries()].forEach(([clipId, node]) => {
      if (!activeIds.has(clipId)) { if (node.pause) node.pause(); node.remove(); visualPlayers.delete(clipId); }
    });
    previewEmpty.hidden = active.length > 0;
    previewOutline.hidden = !selected || !activeIds.has(selected.clip.id);
    updatePreviewGeometry();
  }

  function syncPlayers(shouldPlay, force = false) {
    if (standalonePreviewAssetId) {
      visualPlayers.forEach(player => { if (player.pause) player.pause(); });
      audioPlayers.forEach(player => player.pause());
      if (shouldPlay) preview.play().catch(error => { previewStatus.textContent = error.message; });
      return;
    }
    syncVisualLayers(shouldPlay, force);
    const activeAudio = [];
    timeline.tracks.forEach(track => {
      if (track.muted || track.hidden) return;
      track.clips.forEach(clip => {
        const asset = assetMap.get(clip.assetId);
        const hasAudio = asset?.hasAudio || Boolean(asset?.waveformUrl) || clipKind(clip) === "AUDIO";
        if (hasAudio && playheadMs >= clip.start && playheadMs < clip.start + clip.duration) {
          activeAudio.push({clip, track});
        }
      });
    });
    activeAudio.forEach(active => {
      let player = audioPlayers.get(active.clip.id);
      if (!player) {
        player = new Audio();
        player.preload = "auto";
        audioPlayers.set(active.clip.id, player);
      }
      setPlayer(player, active, shouldPlay, force);
    });
    [...audioPlayers.entries()].forEach(([clipId, player]) => {
      if (!activeAudio.some(item => item.clip.id === clipId)) { player.pause(); audioPlayers.delete(clipId); }
    });
  }

  function stopPlayback(reset = false) {
    playing = false;
    cancelAnimationFrame(playbackFrame);
    preview.pause();
    visualPlayers.forEach(player => { if (player.pause) player.pause(); });
    audioPlayers.forEach(player => player.pause());
    q("[data-preview-play]").innerHTML = "&#9654;";
    previewStatus.textContent = "Ready";
    if (reset) setPlayhead(0);
  }

  function playbackTick(now) {
    if (!playing) return;
    const end = timelineEnd();
    setPlayhead(playbackOrigin + now - playbackStartedAt, false, false);
    if (playheadMs >= end) {
      stopPlayback();
      setPlayhead(end, false);
      return;
    }
    if (now - lastPlaybackSync > 200) {
      syncPlayers(true);
      lastPlaybackSync = now;
    }
    const playheadX = playheadMs / 1000 * zoom;
    if (playheadX > timelineScroll.scrollLeft + timelineScroll.clientWidth - 36) timelineScroll.scrollLeft = Math.max(0, playheadX - timelineScroll.clientWidth * .2);
    playbackFrame = requestAnimationFrame(playbackTick);
  }

  function togglePlayback() {
    if (standalonePreviewAssetId) {
      if (preview.paused) {
        q("[data-preview-play]").innerHTML = "&#9654;";
        previewStatus.textContent = "Playing";
        preview.play().catch(error => {
          q("[data-preview-play]").innerHTML = "&#9654;";
          previewStatus.textContent = error.message;
        });
      } else {
        preview.pause();
        q("[data-preview-play]").innerHTML = "&#9654;";
        previewStatus.textContent = "Ready";
      }
      return;
    }
    if (playing) return stopPlayback();
    if (!timelineEnd()) return toast("Add clips to the timeline first");
    const selected = findClip(selectedId);
    if (selected && ["VIDEO", "IMAGE"].includes(clipKind(selected.clip))) {
      const outside = playheadMs < selected.clip.start || playheadMs >= selected.clip.start + selected.clip.duration;
      if (outside) setPlayhead(selected.clip.start, false, false);
    }
    if (playheadMs >= timelineEnd()) setPlayhead(0, false);
    playing = true;
    playbackOrigin = playheadMs;
    playbackStartedAt = performance.now();
    lastPlaybackSync = playbackStartedAt;
    q("[data-preview-play]").innerHTML = "&#9654;";
    previewStatus.textContent = "Playing";
    syncPlayers(true, true);
    playbackFrame = requestAnimationFrame(playbackTick);
  }

  function numberInput(value, step, minimum) {
    const input = document.createElement("input");
    input.type = "number";
    input.value = value;
    input.step = step;
    input.min = minimum;
    return input;
  }
  const field = (text, input) => {
    const label = document.createElement("label");
    const caption = document.createElement("span");
    label.className = "movie-inspector-field";
    label.title = text;
    caption.className = "movie-inspector-label";
    caption.textContent = text;
    input.title = text;
    input.setAttribute("aria-label", text);
    label.append(caption, input);
    return label;
  };
  const inspectorGroup = (title, ...children) => {
    const group = document.createElement("section");
    const heading = document.createElement("strong");
    group.className = "movie-inspector-group";
    group.dataset.group = title.toLowerCase().replaceAll(" ", "-");
    group.title = ({
      Clip: "Clip name and rendered size",
      Move: "Move the clip in time or to another track",
      Transform: "Trim the source without moving the clip",
      Frame: "Position the picture inside the output canvas",
      Playhead: "Current timeline cursor position",
      Playback: "Playback speed and frame interpolation",
    })[title] || title;
    heading.textContent = title;
    group.append(heading, ...children);
    return group;
  };
  const inspectorButton = (text, title, action) => {
    const button = document.createElement("button");
    button.type = "button"; button.className = "icon-button secondary"; button.textContent = text;
    button.title = title; button.setAttribute("aria-label", title); button.onclick = action;
    return button;
  };
  const bindHold = (button, action) => {
    let delay = 0; let interval = 0; let repeated = false;
    const stop = () => { clearTimeout(delay); clearInterval(interval); button.classList.remove("movie-continuous-active"); };
    button.onpointerdown = event => {
      if (button.disabled || event.button !== 0) return;
      event.preventDefault(); repeated = false; action();
      delay = setTimeout(() => {
        repeated = true; button.classList.add("movie-continuous-active"); interval = setInterval(action, 70);
      }, 320);
      window.addEventListener("pointerup", stop, {once: true});
      window.addEventListener("pointercancel", stop, {once: true});
    };
    button.onclick = event => { event.preventDefault(); if (repeated) repeated = false; };
  };

  function renderInspector() {
    inspector.replaceChildren();
    inspectorActions.replaceChildren();
    qa("[data-inspector-tab]").forEach(button => button.classList.toggle("active", button.dataset.inspectorTab === inspectorTab));
    if (selectedIds.size > 1) {
      const summary = document.createElement("strong");
      summary.className = "movie-selection-summary";
      summary.textContent = `${selectedIds.size} clips selected`;
      inspector.append(summary);
      return;
    }
    const found = findClip(selectedId);
    const clip = found?.clip;
    const track = found?.track;
    const asset = clip ? assetMap.get(clip.assetId) : null;
    if (found) {
      const sourceSummary = document.createElement("span");
      sourceSummary.className = "movie-source-summary";
      sourceSummary.textContent = `Source: ${asset?.name || clip.sourceAssetId || "Unknown"}`;
      sourceSummary.title = `${asset?.name || "Source media"} / ${clip.sourceAssetId || clip.assetId}`;
      inspectorActions.append(sourceSummary);
    }
    const name = document.createElement("input");
    const timeStep = (frameMs() / 1000).toFixed(4);
    const start = numberInput(((clip?.start || 0) / 1000).toFixed(3), timeStep, "0");
    const end = numberInput((((clip?.start || 0) + (clip?.duration || 0)) / 1000).toFixed(3), timeStep, "0");
    const marker = numberInput((playheadMs / 1000).toFixed(3), timeStep, "0");
    const sourceIn = numberInput(((clip?.sourceStart || 0) / 1000).toFixed(3), timeStep, "0");
    const sourceOut = numberInput((((clip?.sourceStart || 0) + (clip?.duration || 0) * Number(clip?.speed || 1)) / 1000).toFixed(3), timeStep, "0");
    const positionX = numberInput(Math.round(Number(clip?.positionX || 0)), "1", "-7680");
    const positionY = numberInput(Math.round(Number(clip?.positionY || 0)), "1", "-7680");
    positionX.max = "7680"; positionY.max = "7680";
    const clipScale = document.createElement("input");
    clipScale.type = "range"; clipScale.min = ".05"; clipScale.max = "8"; clipScale.step = ".01";
    clipScale.dataset.clipScaleRange = "";
    clipScale.value = Number(clip?.scale || 1).toFixed(2);
    const clipScaleNumber = numberInput(Math.round(Number(clip?.scale || 1) * 100), "1", "5"); clipScaleNumber.max = "800"; clipScaleNumber.dataset.clipScaleNumber = "";
    const speed = numberInput(Number(clip?.speed || 1).toFixed(2), ".05", ".1"); speed.max = "8"; speed.className = "movie-clip-speed";
    const speedMethod = document.createElement("select");
    [["FRAME_SAMPLE", "Frame sampling"], ["FRAME_BLEND", "Frame blending"], ["OPTICAL_FLOW", "Optical flow"]].forEach(([value, label]) => {
      const option = document.createElement("option"); option.value = value; option.textContent = label; speedMethod.append(option);
    });
    speedMethod.value = clip?.speedMethod || "FRAME_SAMPLE";
    const fadeIn = numberInput(((clip?.fadeIn || 0) / 1000).toFixed(1), ".1", "0"); fadeIn.max = "2";
    const fadeOut = numberInput(((clip?.fadeOut || 0) / 1000).toFixed(1), ".1", "0"); fadeOut.max = "2";
    const volume = document.createElement("input");
    name.value = clip?.name || asset?.name || "";
    marker.dataset.inspectorPlayhead = "";
    volume.type = "range"; volume.min = "0"; volume.max = "2"; volume.step = ".05"; volume.value = clip?.volume ?? 0;
    [name, start, end, sourceIn, sourceOut, positionX, positionY, clipScale, clipScaleNumber, speed, speedMethod, fadeIn, fadeOut, volume].forEach(control => control.disabled = !found || !canEdit || track?.locked);
    marker.disabled = !timelineEnd();
    if (!found) {
      inspector.append(inspectorGroup("Selection", field("Name", name), field("Start", start), field("End", end), field("Playhead", marker)));
      return;
    }
    if (["EFFECTS", "COLOR"].includes(inspectorTab)) {
      const controls = inspectorTab === "EFFECTS" ? [
        ["Opacity", "opacity", 0, 1, .05], ["Blur", "blur", 0, 40, .5], ["Sharpen", "sharpen", 0, 5, .1],
      ] : [
        ["Brightness", "brightness", -1, 1, .05], ["Contrast", "contrast", 0, 3, .05],
        ["Saturation", "saturation", 0, 3, .05], ["Gamma", "gamma", .1, 3, .05],
      ];
      const fields = controls.map(([label, key, minimum, maximum, step]) => {
        const input = document.createElement("input"); input.type = "range"; input.min = minimum; input.max = maximum; input.step = step; input.value = clip[key];
        const numeric = numberInput(Number(clip[key]).toFixed(step < .1 ? 2 : 1), String(step), String(minimum)); numeric.max = String(maximum);
        const reset = inspectorButton("&#8634;", `Reset ${label}`, () => {
          const defaults = {opacity: 1, blur: 0, sharpen: 0, brightness: 0, contrast: 1, saturation: 1, gamma: 1};
          mutate(() => { clip[key] = defaults[key]; syncPlayers(false, true); }); renderInspector();
        });
        const wrap = document.createElement("span"); wrap.className = "movie-value-control"; wrap.append(input, numeric, reset);
        input.disabled = numeric.disabled = reset.disabled = !canEdit || track.locked;
        const update = value => mutate(() => { clip[key] = clamp(Number(value), minimum, maximum); input.value = clip[key]; numeric.value = clip[key]; syncPlayers(false, true); });
        input.onchange = () => update(input.value); numeric.onchange = () => update(numeric.value);
        return field(label, wrap);
      });
      inspector.append(inspectorGroup(inspectorTab === "EFFECTS" ? "Visual effects" : "Color correction", ...fields));
      return;
    }
    name.onchange = event => mutate(() => { clip.name = event.target.value; renderTimeline(); });
    start.onchange = event => mutate(() => {
      clip.start = Math.max(0, quantizeFrame(Number(event.target.value) * 1000));
      renderTimeline(); syncPlayers(false, true);
    });
    end.onchange = event => mutate(() => {
      const requested = quantizeFrame(Number(event.target.value) * 1000);
      clip.start = Math.max(0, requested - clip.duration);
      renderTimeline(); syncPlayers(false, true);
    });
    sourceIn.onchange = event => mutate(() => {
      const speedValue = Number(clip.speed || 1);
      const oldOut = clip.sourceStart + clip.duration * speedValue;
      clip.sourceStart = clamp(quantizeFrame(Number(event.target.value) * 1000), 0, Math.max(0, oldOut - frameMs() * speedValue));
      clip.duration = Math.max(frameMs(), (oldOut - clip.sourceStart) / speedValue);
      renderTimeline(); syncPlayers(false, true);
    });
    sourceOut.onchange = event => mutate(() => {
      const speedValue = Number(clip.speed || 1);
      const requested = clamp(quantizeFrame(Number(event.target.value) * 1000), clip.sourceStart + frameMs() * speedValue, assetDuration(clip));
      clip.duration = Math.max(frameMs(), (requested - clip.sourceStart) / speedValue);
      renderTimeline(); syncPlayers(false, true);
    });
    marker.onchange = event => { stopPlayback(); setPlayhead(quantizeFrame(Number(event.target.value) * 1000)); };
    const updatePosition = () => mutate(() => {
      clip.positionX = clamp(Number(positionX.value), -7680, 7680);
      clip.positionY = clamp(Number(positionY.value), -7680, 7680);
      updatePreviewGeometry();
    });
    positionX.oninput = updatePosition; positionY.oninput = updatePosition;
    let scaleRemembered = false;
    const updateScaleValue = () => { clipScaleNumber.value = String(Math.round(Number(clipScale.value) * 100)); };
    clipScale.onpointerdown = () => { if (!scaleRemembered) { remember(); scaleRemembered = true; } };
    clipScale.oninput = () => {
      clip.scale = clamp(Number(clipScale.value), .05, 8);
      clipScaleNumber.value = String(Math.round(clip.scale * 100)); updateScaleValue(); updatePreviewGeometry();
    };
    clipScale.onchange = () => {
      clip.scale = clamp(Number(clipScale.value), .05, 8);
      historyRedo = []; updateDirty(); updateHistoryButtons(); scaleRemembered = false; scheduleAutosave();
      updateScaleValue(); updatePreviewGeometry();
    };
    clipScaleNumber.onchange = () => mutate(() => {
      clip.scale = clamp(Number(clipScaleNumber.value) / 100, .05, 8);
      clipScale.value = String(clip.scale); updateScaleValue(); updatePreviewGeometry();
    });
    updateScaleValue();
    volume.onchange = event => mutate(() => { clip.volume = Number(event.target.value); syncPlayers(false, true); });
    speed.onchange = () => mutate(() => {
      const previous = Number(clip.speed || 1); const next = clamp(Number(speed.value), .1, 8);
      const previousDuration = clip.duration;
      clip.duration = Math.max(200, quantize(clip.duration * previous / next)); clip.speed = next;
      clip.volumeKeyframes = (clip.volumeKeyframes || []).map(point => ({...point, time: clamp(quantize(point.time * clip.duration / previousDuration), 0, clip.duration)}));
      renderTimeline(); syncPlayers(false, true);
    });
    speedMethod.onchange = () => mutate(() => { clip.speedMethod = speedMethod.value; });
    fadeIn.onchange = () => mutate(() => { clip.fadeIn = clamp(Math.round(Number(fadeIn.value) * 1000), 0, 2000); });
    fadeOut.onchange = () => mutate(() => { clip.fadeOut = clamp(Math.round(Number(fadeOut.value) * 1000), 0, 2000); });
    if (inspectorTab === "AUDIO") {
      const cleanup = document.createElement("select");
      [["NONE", "Original"], ["VOICE", "Voice clarity"], ["DENOISE", "Noise reduction"]].forEach(([value, label]) => { const option = document.createElement("option"); option.value = value; option.textContent = label; cleanup.append(option); });
      cleanup.value = clip.audioCleanup || "NONE"; cleanup.disabled = !canEdit || track.locked;
      cleanup.onchange = () => mutate(() => { clip.audioCleanup = cleanup.value; });
      const automation = document.createElement("div"); automation.className = "movie-volume-automation";
      const pointArea = document.createElement("div"); pointArea.className = "movie-volume-points"; pointArea.title = "Volume automation key points";
      (clip.volumeKeyframes || []).forEach((point, index) => {
        const pointButton = document.createElement("button"); pointButton.type = "button"; pointButton.className = "movie-volume-point";
        pointButton.style.left = `${point.time / Math.max(1, clip.duration) * 100}%`; pointButton.style.bottom = `${point.value / 2 * 100}%`;
        pointButton.title = `${clock(point.time)} / ${point.value.toFixed(2)}`;
        pointButton.onclick = () => setPlayhead(clip.start + point.time, true, false);
        pointButton.ondblclick = () => mutate(() => { clip.volumeKeyframes.splice(index, 1); renderInspector(); });
        pointArea.append(pointButton);
      });
      const automationActions = document.createElement("div"); automationActions.className = "movie-audio-actions";
      const addPoint = inspectorButton("+", "Add volume point at playhead", () => {
        const local = clamp(playheadMs - clip.start, 0, clip.duration);
        mutate(() => { clip.volumeKeyframes.push({time: quantize(local), value: Number(volume.value)}); clip.volumeKeyframes.sort((a, b) => a.time - b.time); });
        renderInspector();
      });
      const removePoint = inspectorButton("-", "Remove nearest volume point", () => {
        if (!clip.volumeKeyframes.length) return;
        const local = playheadMs - clip.start;
        let nearest = 0;
        clip.volumeKeyframes.forEach((point, index) => { if (Math.abs(point.time - local) < Math.abs(clip.volumeKeyframes[nearest].time - local)) nearest = index; });
        mutate(() => clip.volumeKeyframes.splice(nearest, 1)); renderInspector();
      });
      const jCut = inspectorButton("J", "Apply fade in", () => { mutate(() => { clip.fadeIn = Math.max(100, Number(fadeIn.value || .5) * 1000); }); renderInspector(); });
      const lCut = inspectorButton("L", "Apply fade out", () => { mutate(() => { clip.fadeOut = Math.max(100, Number(fadeOut.value || .5) * 1000); }); renderInspector(); });
      automationActions.append(addPoint, removePoint, jCut, lCut);
      automation.append(pointArea, automationActions);
      const source = document.createElement("div"); source.className = "movie-source-readout"; source.textContent = `Source: ${asset?.name || clip.sourceAssetId}`;
      inspector.append(
        inspectorGroup("Sound", field("Cleanup", cleanup), field("Volume", volume), field("Fade in (s)", fadeIn), field("Fade out (s)", fadeOut)),
        inspectorGroup("Timing", field("Start", start), field("End", end), field("Playhead", marker)),
        inspectorGroup("Automation", automation), source,
      );
      return;
    }
    const scaleWrap = document.createElement("label");
    const scaleCaption = document.createElement("span");
    const scaleControls = document.createElement("span");
    const scaleDown = inspectorButton("-", "Reduce clip scale by 1%", () => {
      if (!found || track.locked) return;
      mutate(() => { clip.scale = clamp(Number(clip.scale || 1) - .01, .05, 8); updatePreviewGeometry(); });
      clipScale.value = String(clip.scale); updateScaleValue();
      renderInspector();
    });
    const scaleUp = inspectorButton("+", "Increase clip scale by 1%", () => {
      if (!found || track.locked) return;
      mutate(() => { clip.scale = clamp(Number(clip.scale || 1) + .01, .05, 8); updatePreviewGeometry(); });
      clipScale.value = String(clip.scale); updateScaleValue();
      renderInspector();
    });
    scaleDown.disabled = scaleUp.disabled = !canEdit || track.locked;
    scaleWrap.className = "movie-inspector-field"; scaleCaption.className = "movie-inspector-label"; scaleCaption.textContent = "Scale";
    clipScaleNumber.title = "Clip scale in percent";
    scaleControls.className = "movie-scale-stepper"; scaleControls.append(scaleDown, clipScale, clipScaleNumber, scaleUp); scaleWrap.append(scaleCaption, scaleControls);
    const pad = document.createElement("div");
    pad.className = "movie-position-pad"; pad.title = "Move rendered video inside the output frame";
    const nudgePosition = (dx, dy) => {
      mutate(() => { clip.positionX = clamp(Number(clip.positionX || 0) + dx, -7680, 7680); clip.positionY = clamp(Number(clip.positionY || 0) + dy, -7680, 7680); updatePreviewGeometry(); });
      renderInspector();
    };
    [["&#8593;", 0, 1, "Move video up one pixel"], ["&#8592;", -1, 0, "Move video left one pixel"], ["&#9679;", 0, 0, "Center video"], ["&#8594;", 1, 0, "Move video right one pixel"], ["&#8595;", 0, -1, "Move video down one pixel"]].forEach(([symbol, dx, dy, title]) => {
      const button = document.createElement("button"); button.type = "button"; button.className = "icon-button secondary"; button.innerHTML = symbol; button.title = title;
      button.disabled = !canEdit || track.locked;
      if (dx) button.dataset.positionX = dx; if (dy) button.dataset.positionY = dy;
      if (!dx && !dy) button.classList.add("movie-position-home");
      const moveAction = () => {
        if (dx || dy) nudgePosition(dx, dy);
        else {
          mutate(() => { clip.positionX = 0; clip.positionY = 0; updatePreviewGeometry(); });
          renderInspector();
        }
      };
      bindHold(button, moveAction);
      pad.append(button);
    });
    const coordinates = document.createElement("div"); coordinates.className = "movie-position-coordinates";
    coordinates.append(field("X (px)", positionX), field("Y (px)", positionY));
    const timelinePad = document.createElement("div"); timelinePad.className = "movie-position-pad movie-timeline-position-pad"; timelinePad.title = "Move the clip on the timeline";
    const alignToPlayhead = () => {
      const selected = [...selectedIds].map(findClip).filter(item => item && !item.track.locked);
      if (!selected.length) return;
      const first = Math.min(...selected.map(item => item.clip.start));
      mutate(() => selected.forEach(item => { item.clip.start = Math.max(0, quantize(item.clip.start + playheadMs - first)); }));
      renderTimeline(); renderInspector();
    };
    [
      ["&#8593;", () => moveSelectedToAdjacentTrack(-1), "Move clip to the track above", "up"],
      ["&#8592;", () => nudgeSelected(-1, false), "Move clip left one frame", "left"],
      ["&#9679;", alignToPlayhead, "Align selection start to playhead", "home"],
      ["&#8594;", () => nudgeSelected(1, false), "Move clip right one frame", "right"],
      ["&#8595;", () => moveSelectedToAdjacentTrack(1), "Move clip to the track below", "down"],
    ].forEach(([symbol, action, title, direction]) => {
      const button = document.createElement("button"); button.type = "button"; button.className = `icon-button secondary movie-pad-${direction}`;
      button.innerHTML = symbol; button.title = title; button.disabled = !canEdit || track.locked;
      bindHold(button, action); timelinePad.append(button);
    });
    inspector.append(
      inspectorGroup("Clip", field("Name", name), scaleWrap),
      inspectorGroup("Move", field("Start", start), field("End", end), timelinePad),
      inspectorGroup("Transform", field("Source in", sourceIn), field("Source out", sourceOut)),
      inspectorGroup("Frame", coordinates, pad),
      inspectorGroup("Playhead", field("Position", marker)),
      inspectorGroup("Playback", field("Speed", speed), field("Interpolation", speedMethod)),
    );
  }

  function selectClip(id, movePlayhead = false, additive = false) {
    leaveStandalonePreview();
    mediaSelectionActive = false;
    if (movePlayhead) {
      const found = findClip(id);
      if (found) setPlayhead(found.clip.start, true, false);
    }
    if (additive) {
      if (selectedIds.has(id)) selectedIds.delete(id);
      else selectedIds.add(id);
      selectedId = selectedIds.has(id) ? id : [...selectedIds].at(-1) || null;
    } else {
      selectedIds = new Set(id ? [id] : []);
      selectedId = id;
    }
    qa(".movie-clip").forEach(node => node.classList.toggle("selected", selectedIds.has(node.dataset.clipId)));
    renderInspector();
    if (!playing) syncPlayers(false, true);
  }

  function deleteSelected() {
    const removable = new Set([...selectedIds].filter(id => !findClip(id)?.track.locked));
    if (!removable.size) return;
    mutate(() => {
      timeline.tracks.forEach(track => { track.clips = track.clips.filter(item => !removable.has(item.id)); });
      selectedId = null; selectedIds.clear();
    });
    renderTimeline(); renderInspector(); syncPlayers(false, true);
  }

  function copySelected() {
    const items = [...selectedIds].map(id => {
      const found = findClip(id);
      return found ? {clip: structuredClone(found.clip), trackId: found.track.id, trackIndex: timeline.tracks.indexOf(found.track)} : null;
    }).filter(Boolean);
    if (!items.length) return toast("Select one or more clips first");
    const origin = Math.min(...items.map(item => item.clip.start));
    const anchorIndex = items.find(item => item.clip.id === selectedId)?.trackIndex ?? items[0].trackIndex;
    clipClipboard = items.map(item => ({...item, offset: item.clip.start - origin, trackOffset: item.trackIndex - anchorIndex}));
    toast(`${clipClipboard.length} clip${clipClipboard.length === 1 ? "" : "s"} copied`);
  }

  function pasteSelected() {
    if (!clipClipboard.length) return toast("Copy clips first");
    const pasted = [];
    mutate(() => {
      const indicated = timeline.tracks.find(track => track.id === pasteTargetTrackId && !track.locked);
      const indicatedIndex = indicated ? timeline.tracks.indexOf(indicated) : -1;
      clipClipboard.forEach(item => {
        const offsetTarget = indicatedIndex >= 0 ? timeline.tracks[indicatedIndex + Number(item.trackOffset || 0)] : null;
        const originalTrack = timeline.tracks.find(track => track.id === item.trackId && !track.locked);
        const target = offsetTarget && !offsetTarget.locked ? offsetTarget : indicated || originalTrack || timeline.tracks.find(track => !track.locked);
        if (!target) return;
        const clip = {...structuredClone(item.clip), id: uid(), start: quantize(playheadMs + item.offset)};
        target.clips.push(clip); pasted.push(clip.id);
      });
      selectedIds = new Set(pasted); selectedId = pasted.at(-1) || null;
    });
    renderTimeline(); renderInspector();
  }

  function joinSelected() {
    const items = [...selectedIds].map(findClip).filter(Boolean).sort((a, b) => a.clip.start - b.clip.start);
    if (items.length !== 2) return toast("Select exactly two clips to join");
    if (items.some(item => item.track.locked)) return;
    const [first, second] = items;
    mutate(() => {
      if (second.track !== first.track) {
        second.track.clips = second.track.clips.filter(item => item.id !== second.clip.id);
        first.track.clips.push(second.clip);
      }
      second.clip.start = quantize(first.clip.start + first.clip.duration);
      const groupId = first.clip.groupId || second.clip.groupId || uid();
      first.clip.groupId = groupId; second.clip.groupId = groupId;
    });
    renderTimeline(); renderInspector(); toast("Selected clips joined");
  }

  function splitSelected() {
    const found = findClip(selectedId);
    if (!found || found.track.locked) return;
    const {clip, track} = found;
    const offset = quantize(playheadMs - clip.start);
    if (offset < 200 || clip.duration - offset < 200) return toast("Put the playhead at least 0.2 s from either clip edge");
    mutate(() => {
      const right = {...clip, id: uid(), name: `${clip.name} B`, start: clip.start + offset, sourceStart: clip.sourceStart + offset * Number(clip.speed || 1), duration: clip.duration - offset};
      clip.duration = offset;
      track.clips.push(right);
      selectedId = right.id;
      selectedIds = new Set([right.id]);
    });
    renderTimeline(); renderInspector();
  }

  function nudgeSelected(direction, movePlayhead = true) {
    const found = [...selectedIds].map(findClip).filter(item => item && !item.track.locked);
    if (!found.length) return;
    const delta = direction * frameMs();
    mutate(() => { found.forEach(item => { item.clip.start = Math.max(0, quantize(item.clip.start + delta)); }); });
    renderTimeline(); renderInspector();
    if (movePlayhead) setPlayhead(Math.min(...found.map(item => item.clip.start)), false);
  }

  function moveSelectedToAdjacentTrack(direction) {
    const found = [...selectedIds].map(findClip).filter(Boolean);
    if (!found.length || found.some(item => item.track.locked)) return;
    const moves = found.map(item => {
      const index = timeline.tracks.indexOf(item.track);
      return {item, target: timeline.tracks[index + direction]};
    });
    if (moves.some(move => !move.target || move.target.locked)) return toast("No free track in that direction");
    mutate(() => {
      const ids = new Set(found.map(item => item.clip.id));
      timeline.tracks.forEach(track => { track.clips = track.clips.filter(item => !ids.has(item.id)); });
      moves.forEach(move => move.target.clips.push(move.item.clip));
    });
    renderTimeline(); renderInspector();
  }

  function addTrack(kind) {
    let result;
    mutate(() => {
      const count = timeline.tracks.filter(item => item.kind === kind).length + 1;
      result = {id: uid(), name: `${kind === "VIDEO" ? "Video" : "Audio"} ${count}`, kind, muted: false, locked: false, height: defaultTrackHeight, clips: []};
      timeline.tracks.push(result);
    });
    renderTimeline();
    return result;
  }

  function moveTrack(track, direction) {
    const swapWith = timeline.tracks[timeline.tracks.indexOf(track) + direction];
    if (!swapWith) return;
    mutate(() => {
      const first = timeline.tracks.indexOf(track);
      const second = timeline.tracks.indexOf(swapWith);
      [timeline.tracks[first], timeline.tracks[second]] = [timeline.tracks[second], timeline.tracks[first]];
    });
    renderTimeline();
  }

  function selectTrack(trackId, {render = true} = {}) {
    selectedTrackId = timeline.tracks.some(track => track.id === trackId) ? trackId : null;
    const actions = q("[data-selected-track-actions]");
    if (actions) actions.hidden = !selectedTrackId;
    if (render) renderTimeline();
  }

  function moveSelectedTrack(direction) {
    const track = timeline.tracks.find(item => item.id === selectedTrackId);
    if (track) moveTrack(track, direction);
  }

  function reorderTrack(sourceId, targetId, after = false) {
    const sourceIndex = timeline.tracks.findIndex(track => track.id === sourceId);
    const targetIndex = timeline.tracks.findIndex(track => track.id === targetId);
    if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex) return;
    mutate(() => {
      const [track] = timeline.tracks.splice(sourceIndex, 1);
      let insertion = timeline.tracks.findIndex(item => item.id === targetId);
      if (after) insertion += 1;
      timeline.tracks.splice(clamp(insertion, 0, timeline.tracks.length), 0, track);
    });
    selectedTrackId = sourceId;
    renderTimeline();
  }

  function openTrackContext(event, track) {
    if (!trackContext) return;
    event.preventDefault();
    event.stopPropagation();
    trackContextId = track.id;
    selectTrack(track.id, {render: false});
    q("[data-track-context-title]", trackContext).textContent = track.name;
    const labels = {
      mute: track.muted ? "Unmute" : "Mute",
      visibility: track.hidden ? "Show" : "Hide",
      lock: track.locked ? "Unlock" : "Lock",
      display: track.displayMode === "WAVEFORM" ? "Show clips and frames" : "Show waveform",
    };
    Object.entries(labels).forEach(([action, label]) => {
      const button = q(`[data-track-context-action="${action}"]`, trackContext);
      if (button) button.textContent = label;
    });
    trackContext.hidden = false;
    const width = Math.max(190, trackContext.offsetWidth);
    const height = Math.max(180, trackContext.offsetHeight);
    trackContext.style.left = `${clamp(event.clientX, 4, innerWidth - width - 4)}px`;
    trackContext.style.top = `${clamp(event.clientY, 4, innerHeight - height - 4)}px`;
  }

  function performTrackAction(action, trackId = trackContextId) {
    const track = timeline.tracks.find(item => item.id === trackId);
    if (!track) return;
    if (action === "up") return moveTrack(track, -1);
    if (action === "down") return moveTrack(track, 1);
    if (action === "delete") {
      if (track.clips.length) return toast("Remove clips before deleting this track");
      mutate(() => { timeline.tracks = timeline.tracks.filter(item => item.id !== track.id); });
      selectedTrackId = null;
      renderTimeline();
      return;
    }
    mutate(() => {
      if (action === "mute") track.muted = !track.muted;
      if (action === "visibility") track.hidden = !track.hidden;
      if (action === "lock") track.locked = !track.locked;
      if (action === "display") track.displayMode = track.displayMode === "WAVEFORM" ? "CLIPS" : "WAVEFORM";
      if (action === "height-down") track.height = clamp(Math.round(Number(track.height || defaultTrackHeight) / 21 - 1), 1, 10) * 21;
      if (action === "height-up") track.height = clamp(Math.round(Number(track.height || defaultTrackHeight) / 21 + 1), 1, 10) * 21;
    });
    renderTimeline();
    renderInspector();
    syncPlayers(false, true);
  }

  function addClip(track, assetId, start) {
    const asset = assetMap.get(assetId);
    if (!asset || asset.status !== "READY" || track.locked) return;
    let clip;
    mutate(() => {
      clip = {id: uid(), assetId, sourceAssetId: assetId, name: asset.name, start: snapTime(start, null), sourceStart: 0, duration: Math.max(200, asset.durationMs || 8000), volume: 1, scale: 1, positionX: 0, positionY: 0, positionUnit: "PIXELS", speed: 1, speedMethod: "FRAME_SAMPLE", fadeIn: 0, fadeOut: 0, audioCleanup: "NONE", opacity: 1, blur: 0, sharpen: 0, brightness: 0, contrast: 1, saturation: 1, gamma: 1, volumeKeyframes: []};
      track.clips.push(clip);
      selectedId = clip.id;
      selectedIds = new Set([clip.id]);
    });
    snapGuide.hidden = true;
    renderTimeline();
    selectClip(clip.id, true);
  }

  function addClipGroup(track, assetIds, start) {
    if (!track || track.locked) return;
    const uniqueIds = [...new Set((assetIds || []).filter(Boolean))];
    const ready = uniqueIds.map(id => assetMap.get(id)).filter(asset => asset?.status === "READY");
    const skipped = uniqueIds.length - ready.length;
    if (!ready.length) {
      if (skipped) toast(`${skipped} media item${skipped === 1 ? " is" : "s are"} not ready and was skipped`, "warning");
      return;
    }
    let cursor = snapTime(start, null);
    const added = [];
    mutate(() => {
      ready.forEach(asset => {
        const clip = {id: uid(), assetId: asset.id, sourceAssetId: asset.id, name: asset.name, start: cursor, sourceStart: 0, duration: Math.max(200, asset.durationMs || 8000), volume: 1, scale: 1, positionX: 0, positionY: 0, positionUnit: "PIXELS", speed: 1, speedMethod: "FRAME_SAMPLE", fadeIn: 0, fadeOut: 0, audioCleanup: "NONE", opacity: 1, blur: 0, sharpen: 0, brightness: 0, contrast: 1, saturation: 1, gamma: 1, volumeKeyframes: []};
        track.clips.push(clip);
        added.push(clip);
        cursor = quantize(cursor + clip.duration);
      });
      selectedIds = new Set(added.map(clip => clip.id));
      selectedId = added.at(-1)?.id || null;
    });
    snapGuide.hidden = true;
    renderTimeline();
    if (selectedId) selectClip(selectedId, true);
    if (skipped) toast(`${skipped} media item${skipped === 1 ? " is" : "s are"} not ready and was skipped`, "warning");
  }

  function makeTrackHead(track) {
    const head = document.createElement("div");
    const nameWrap = document.createElement("div");
    const title = document.createElement("strong");
    const kind = document.createElement("small");
    const code = document.createElement("b");
    const controls = document.createElement("div");
    const up = document.createElement("button");
    const down = document.createElement("button");
    const mute = document.createElement("button");
    const visibility = document.createElement("button");
    const lock = document.createElement("button");
    const display = document.createElement("button");
    const loudnessGuide = document.createElement("input");
    const height = document.createElement("span");
    const heightDown = document.createElement("button");
    const heightValue = document.createElement("output");
    const heightUp = document.createElement("button");
    const heightResizer = document.createElement("i");
    const remove = document.createElement("button");
    head.className = `movie-track-head${track.locked ? " locked" : ""}${track.hidden ? " hidden-track" : ""}${Number(track.height) <= 21 ? " height-1" : ""}`;
    head.dataset.trackId = track.id;
    head.draggable = canEdit;
    nameWrap.className = "movie-track-name";
    code.className = "movie-track-code";
    const sameKind = timeline.tracks.filter(item => item.kind === track.kind);
    code.textContent = `${track.kind === "AUDIO" ? "A" : "V"}${sameKind.indexOf(track) + 1}`;
    controls.className = "movie-track-controls";
    head.style.height = `${Number(track.height || defaultTrackHeight)}px`;
    title.textContent = track.name;
    kind.textContent = `${track.kind} / ${track.clips.length} clip${track.clips.length === 1 ? "" : "s"}`;
    nameWrap.append(code, title, kind);
    [up, down, mute, visibility, lock, display, remove].forEach(button => { button.type = "button"; button.className = "icon-button secondary movie-track-control"; });
    up.innerHTML = "&#8593;"; up.title = "Move track up"; up.onclick = () => moveTrack(track, -1);
    down.innerHTML = "&#8595;"; down.title = "Move track down"; down.onclick = () => moveTrack(track, 1);
    mute.innerHTML = track.muted ? "&#128263;" : "&#128266;"; mute.classList.toggle("off", track.muted); mute.title = track.muted ? "Unmute track" : "Mute track";
    visibility.innerHTML = "&#128065;"; visibility.classList.toggle("off", track.hidden); visibility.title = track.hidden ? "Show track" : "Hide track";
    lock.innerHTML = track.locked ? "&#128274;" : "&#128275;"; lock.classList.toggle("off", track.locked); lock.title = track.locked ? "Unlock track" : "Lock track";
    mute.classList.add("movie-track-mute"); visibility.classList.add("movie-track-visible"); lock.classList.add("movie-track-lock");
    display.textContent = track.displayMode === "WAVEFORM" ? "\u224b" : "\u2261";
    display.classList.add("movie-track-display");
    display.title = track.displayMode === "WAVEFORM" ? "Show clips and frames" : "Show waveform and loudness guide";
    const levelForHeight = value => clamp(Math.round(Number(value || defaultTrackHeight) / 21), 1, 10);
    const applyHeightLevel = (level, {commit = true} = {}) => {
      const nextLevel = clamp(Math.round(level), 1, 10);
      const nextHeight = nextLevel * 21;
      if (commit) remember();
      track.height = nextHeight;
      heightValue.value = String(nextLevel);
      heightValue.textContent = String(nextLevel);
      localStorage.setItem("studio-movie-track-height", String(nextHeight));
      if (commit) {
        historyRedo = [];
        updateDirty();
        updateHistoryButtons();
        scheduleAutosave();
        renderTimeline();
      } else {
        head.style.height = `${nextHeight}px`;
        const lane = q(`.movie-track-lane[data-track-id="${track.id}"]`);
        if (lane) lane.style.height = `${nextHeight}px`;
      }
    };
    height.className = "movie-track-height-stepper";
    height.title = "Track height level from 1 to 10";
    heightDown.type = "button"; heightDown.className = "icon-button secondary"; heightDown.textContent = "\u2212"; heightDown.title = "Decrease track height";
    heightUp.type = "button"; heightUp.className = "icon-button secondary"; heightUp.textContent = "+"; heightUp.title = "Increase track height";
    heightValue.value = String(levelForHeight(track.height)); heightValue.textContent = heightValue.value;
    heightDown.onclick = () => applyHeightLevel(levelForHeight(track.height) - 1);
    heightUp.onclick = () => applyHeightLevel(levelForHeight(track.height) + 1);
    height.append(heightDown, heightValue, heightUp);
    heightResizer.className = "movie-track-boundary-resizer";
    heightResizer.title = "Drag the track boundary to change its height";
    heightResizer.onpointerdown = event => {
      if (event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      remember();
      const startY = event.clientY;
      const startHeight = track.height;
      heightResizer.classList.add("dragging");
      capturePointerSafely(heightResizer, event.pointerId);
      heightResizer.onpointermove = move => applyHeightLevel((startHeight + (move.clientY - startY) / 2) / 21, {commit: false});
      const finish = () => {
        heightResizer.onpointermove = null;
        heightResizer.onpointerup = null;
        heightResizer.onpointercancel = null;
        heightResizer.classList.remove("dragging");
        historyRedo = [];
        updateDirty();
        updateHistoryButtons();
        scheduleAutosave();
        renderTimeline();
      };
      heightResizer.onpointerup = finish;
      heightResizer.onpointercancel = finish;
    };
    remove.innerHTML = "&#215;"; remove.title = "Delete empty track";
    mute.onclick = () => mutate(() => { track.muted = !track.muted; renderTimeline(); syncPlayers(false, true); });
    visibility.onclick = () => mutate(() => { track.hidden = !track.hidden; renderTimeline(); syncPlayers(false, true); });
    lock.onclick = () => mutate(() => { track.locked = !track.locked; renderTimeline(); renderInspector(); });
    display.onclick = () => mutate(() => { track.displayMode = track.displayMode === "WAVEFORM" ? "CLIPS" : "WAVEFORM"; renderTimeline(); });
    loudnessGuide.type = "range"; loudnessGuide.min = "0"; loudnessGuide.max = "2"; loudnessGuide.step = ".05";
    loudnessGuide.value = String(track.loudnessGuide ?? 1); loudnessGuide.className = "movie-track-guide";
    loudnessGuide.title = "Target volume guide"; loudnessGuide.setAttribute("aria-label", "Target volume guide");
    loudnessGuide.hidden = track.displayMode !== "WAVEFORM";
    loudnessGuide.onchange = () => mutate(() => { track.loudnessGuide = Number(loudnessGuide.value); renderTimeline(); });
    remove.onclick = () => {
      if (track.clips.length) return toast("Remove clips before deleting this track");
      mutate(() => { timeline.tracks = timeline.tracks.filter(item => item.id !== track.id); });
      renderTimeline();
    };
    controls.append(mute, visibility, lock, display, loudnessGuide, height, remove);
    head.append(nameWrap, controls, heightResizer);
    nameWrap.addEventListener("contextmenu", event => openTrackContext(event, track));
    head.addEventListener("contextmenu", event => openTrackContext(event, track));
    head.addEventListener("dragstart", event => {
      if (!canEdit || event.target.closest("button,input")) return event.preventDefault();
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/x-lexamora-track", track.id);
      requestAnimationFrame(() => head.classList.add("dragging"));
    });
    head.addEventListener("dragend", () => {
      head.classList.remove("dragging");
      qa(".movie-track-head").forEach(item => item.classList.remove("movie-track-drag-over"));
    });
    head.addEventListener("dragover", event => {
      if (!event.dataTransfer.types.includes("text/x-lexamora-track")) return;
      event.preventDefault();
      event.dataTransfer.dropEffect = "move";
      qa(".movie-track-head").forEach(item => item.classList.remove("movie-track-drag-over"));
      head.classList.add("movie-track-drag-over");
    });
    head.addEventListener("drop", event => {
      const sourceId = event.dataTransfer.getData("text/x-lexamora-track");
      if (!sourceId) return;
      event.preventDefault();
      head.classList.remove("movie-track-drag-over");
      const rect = head.getBoundingClientRect();
      reorderTrack(sourceId, track.id, event.clientY > rect.top + rect.height / 2);
    });
    return head;
  }

  function closestTrack(clientY) {
    const candidates = qa(".movie-track-lane").map(lane => {
      const track = timeline.tracks.find(item => item.id === lane.dataset.trackId);
      const rect = lane.getBoundingClientRect();
      return {lane, track, distance: clientY < rect.top ? rect.top - clientY : clientY > rect.bottom ? clientY - rect.bottom : 0};
    }).filter(item => item.track && !item.track.locked);
    candidates.sort((a, b) => a.distance - b.distance);
    return candidates[0] || null;
  }

  function beginClipGesture(event, clip, track, mode, node) {
    if (!canEdit || track.locked || event.button !== 0) return;
    event.preventDefault(); event.stopPropagation();
    const additive = event.ctrlKey || event.metaKey || event.shiftKey;
    if (!selectedIds.has(clip.id)) {
      selectClip(clip.id, false, additive);
      if (!additive && clip.groupId) {
        selectedIds = new Set(timeline.tracks.flatMap(item => item.clips).filter(item => item.groupId === clip.groupId).map(item => item.id));
        selectedId = clip.id;
        qa(".movie-clip").forEach(item => item.classList.toggle("selected", selectedIds.has(item.dataset.clipId)));
      }
    }
    pasteTargetTrackId = track.id;
    const boundsAtPress = node.getBoundingClientRect();
    const clickedAt = clip.start + clamp(event.clientX - boundsAtPress.left, 0, boundsAtPress.width) / zoom * 1000;
    const playheadInsideClip = playheadMs >= clip.start && playheadMs <= clip.start + clip.duration;
    if (!playheadInsideClip) setPlayhead(clickedAt, true, false);
    remember();
    const originX = event.clientX;
    const originScroll = timelineScroll.scrollLeft;
    const anchorTrackIndex = timeline.tracks.indexOf(track);
    const movingIds = mode === "move" ? new Set(selectedIds) : new Set([clip.id]);
    const items = [...movingIds].map(id => {
      const found = findClip(id);
      const clipNode = q(`.movie-clip[data-clip-id="${CSS.escape(id)}"]`);
      if (!found || found.track.locked || !clipNode) return null;
      return {
        clip: found.clip,
        track: found.track,
        node: clipNode,
        trackIndex: timeline.tracks.indexOf(found.track),
        laneTop: clipNode.closest(".movie-track-lane")?.getBoundingClientRect().top || 0,
        original: {start: found.clip.start, sourceStart: found.clip.sourceStart, duration: found.clip.duration},
        targetTrack: found.track,
      };
    }).filter(Boolean);
    const anchor = items.find(item => item.clip.id === clip.id);
    if (!anchor) { historyUndo.pop(); return; }
    let changed = false;
    items.forEach(item => item.node.classList.add("dragging"));
    capturePointerSafely(node, event.pointerId);
    const move = next => {
      const bounds = timelineScroll.getBoundingClientRect();
      if (next.clientX > bounds.right - 36) timelineScroll.scrollLeft += 22;
      else if (next.clientX < bounds.left + 36) timelineScroll.scrollLeft = Math.max(0, timelineScroll.scrollLeft - 22);
      const delta = (next.clientX - originX + timelineScroll.scrollLeft - originScroll) / zoom * 1000;
      if (mode === "move") {
        const snappedDelta = snapGroupDelta(delta, items);
        const candidate = closestTrack(next.clientY);
        let trackDelta = 0;
        if (candidate) {
          trackDelta = timeline.tracks.indexOf(candidate.track) - anchorTrackIndex;
          const valid = items.every(item => {
            const target = timeline.tracks[item.trackIndex + trackDelta];
            return target && !target.locked;
          });
          if (!valid) trackDelta = 0;
        }
        items.forEach(item => {
          item.clip.start = Math.max(0, quantize(item.original.start + snappedDelta));
          item.targetTrack = timeline.tracks[item.trackIndex + trackDelta] || item.track;
          const targetLane = q(`.movie-track-lane[data-track-id="${CSS.escape(item.targetTrack.id)}"]`);
          const targetTop = targetLane?.getBoundingClientRect().top ?? item.laneTop;
          item.node.style.left = `${item.clip.start / 1000 * zoom}px`;
          item.node.style.transform = `translateY(${targetTop - item.laneTop}px)`;
        });
      } else if (mode === "left") {
        let shift = snapTime(anchor.original.start + delta, clip.id) - anchor.original.start;
        const speed = Number(clip.speed || 1);
        shift = clamp(quantize(shift), -anchor.original.sourceStart / speed, anchor.original.duration - 200);
        clip.start = anchor.original.start + shift; clip.sourceStart = anchor.original.sourceStart + shift * speed; clip.duration = anchor.original.duration - shift;
      } else {
        const nextEnd = snapTime(anchor.original.start + anchor.original.duration + delta, clip.id);
        clip.duration = clamp(quantize(nextEnd - anchor.original.start), 200, (assetDuration(clip) - anchor.original.sourceStart) / Number(clip.speed || 1));
      }
      changed = changed || Math.abs(next.clientX - originX) > 2 || Math.abs(next.clientY - event.clientY) > 2;
      if (mode !== "move") {
        node.style.left = `${clip.start / 1000 * zoom}px`;
        node.style.width = `${Math.max(8, clip.duration / 1000 * zoom)}px`;
      }
    };
    const cleanup = () => {
      window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", finish); window.removeEventListener("pointercancel", cancel);
      items.forEach(item => { item.node.classList.remove("dragging"); item.node.style.transform = ""; });
      qa(".movie-track-lane").forEach(lane => lane.classList.remove("drop-target")); snapGuide.hidden = true;
    };
    const finish = () => {
      cleanup();
      const placedTrackIds = new Set();
      if (!changed) historyUndo.pop();
      else {
        items.forEach(item => { item.node.dataset.suppressClick = "true"; });
        if (mode === "move") {
          const movedIds = new Set(items.map(item => item.clip.id));
          timeline.tracks.forEach(item => { item.clips = item.clips.filter(existing => !movedIds.has(existing.id)); });
          items.forEach(item => { item.targetTrack.clips.push(item.clip); placedTrackIds.add(item.targetTrack.id); });
        }
        historyRedo = []; updateDirty(); updateHistoryButtons(); scheduleAutosave();
      }
      renderTimeline(); renderInspector();
      placedTrackIds.forEach(id => {
        const lane = q(`.movie-track-lane[data-track-id="${CSS.escape(id)}"]`);
        lane?.classList.add("placed");
        setTimeout(() => lane?.classList.remove("placed"), 320);
      });
    };
    const cancel = () => {
      items.forEach(item => Object.assign(item.clip, item.original));
      changed = false;
      finish();
    };
    window.addEventListener("pointermove", move); window.addEventListener("pointerup", finish); window.addEventListener("pointercancel", cancel);
  }

  function beginMarquee(event, lane) {
    if (event.button !== 0) return;
    event.preventDefault();
    const startX = event.clientX;
    const startY = event.clientY;
    let active = false;
    const move = next => {
      if (!active && Math.hypot(next.clientX - startX, next.clientY - startY) < 4) return;
      active = true;
      const canvasRect = timelineCanvas.getBoundingClientRect();
      const left = Math.min(startX, next.clientX);
      const top = Math.min(startY, next.clientY);
      const right = Math.max(startX, next.clientX);
      const bottom = Math.max(startY, next.clientY);
      marquee.hidden = false;
      marquee.style.left = `${left - canvasRect.left}px`;
      marquee.style.top = `${top - canvasRect.top}px`;
      marquee.style.width = `${right - left}px`;
      marquee.style.height = `${bottom - top}px`;
      selectedIds = new Set(qa(".movie-clip").filter(item => {
        const rect = item.getBoundingClientRect();
        return rect.right >= left && rect.left <= right && rect.bottom >= top && rect.top <= bottom;
      }).map(item => item.dataset.clipId));
      selectedId = [...selectedIds].at(-1) || null;
      qa(".movie-clip").forEach(item => item.classList.toggle("selected", selectedIds.has(item.dataset.clipId)));
    };
    const finish = next => {
      window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", finish);
      marquee.hidden = true;
      if (!active) {
        selectClip(null);
        setPlayhead((next.clientX - lane.getBoundingClientRect().left) / zoom * 1000);
      } else renderInspector();
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", finish);
  }

  function beginTimelinePan(event, lane) {
    if (event.button !== 0) return;
    event.preventDefault();
    const startX = event.clientX;
    const startScroll = timelineScroll.scrollLeft;
    let moved = false;
    const move = next => {
      const delta = next.clientX - startX;
      moved = moved || Math.abs(delta) > 3;
      timelineScroll.scrollLeft = Math.max(0, startScroll - delta);
    };
    const finish = next => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", finish);
      timelineScroll.classList.remove("panning");
      if (!moved) {
        selectClip(null);
        setPlayhead((next.clientX - lane.getBoundingClientRect().left) / zoom * 1000);
      }
    };
    timelineScroll.classList.add("panning");
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", finish);
  }

  function beginLaneGesture(event, lane) {
    if (event.button !== 0) return;
    pasteTargetTrackId = lane.dataset.trackId;
    event.preventDefault();
    const startX = event.clientX;
    const startY = event.clientY;
    const startScroll = timelineScroll.scrollLeft;
    let mode = event.ctrlKey || event.metaKey ? "marquee" : "pan";
    const move = next => {
      const dx = next.clientX - startX;
      const dy = next.clientY - startY;
      if (mode === "pan") {
        timelineScroll.classList.add("panning");
        timelineScroll.scrollLeft = Math.max(0, startScroll - dx);
        return;
      }
      if (mode !== "marquee") return;
      const canvasRect = timelineCanvas.getBoundingClientRect();
      const left = Math.min(startX, next.clientX);
      const top = Math.min(startY, next.clientY);
      const right = Math.max(startX, next.clientX);
      const bottom = Math.max(startY, next.clientY);
      marquee.hidden = false;
      marquee.style.left = `${left - canvasRect.left}px`;
      marquee.style.top = `${top - canvasRect.top}px`;
      marquee.style.width = `${right - left}px`;
      marquee.style.height = `${bottom - top}px`;
      selectedIds = new Set(qa(".movie-clip").filter(item => {
        const rect = item.getBoundingClientRect();
        return rect.right >= left && rect.left <= right && rect.bottom >= top && rect.top <= bottom;
      }).map(item => item.dataset.clipId));
      selectedId = [...selectedIds].at(-1) || null;
      qa(".movie-clip").forEach(item => item.classList.toggle("selected", selectedIds.has(item.dataset.clipId)));
    };
    const finish = next => {
      window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", finish);
      timelineScroll.classList.remove("panning"); marquee.hidden = true;
      if (mode === "pan" && Math.abs(next.clientX - startX) < 4) {
        selectClip(null);
        setPlayhead((next.clientX - lane.getBoundingClientRect().left) / zoom * 1000);
      } else if (mode === "marquee") renderInspector();
    };
    window.addEventListener("pointermove", move); window.addEventListener("pointerup", finish);
  }

  function beginCanvasGesture(event) {
    if (event.defaultPrevented || event.button !== 0 || event.target.closest(".movie-clip,.movie-ruler,.movie-playhead,button,input,select")) return;
    const lane = event.target.closest(".movie-track-lane") || tracksNode;
    beginLaneGesture(event, lane);
  }

  function showClipContext(event, clip) {
    event.preventDefault(); event.stopPropagation();
    const asset = assetMap.get(clip.sourceAssetId || clip.assetId);
    const menu = q("[data-clip-context]");
    if (!menu) return;
    menu.querySelector("[data-clip-context-current]").textContent = clip.name || "Untitled clip";
    menu.querySelector("[data-clip-context-source]").textContent = asset?.name || clip.sourceAssetId || "Unknown source";
    const download = menu.querySelector("[data-clip-source-download]");
    if (download) download.href = asset?.downloadUrl || asset?.originalUrl || "#";
    const render = menu.querySelector("[data-clip-render-project]");
    if (render) render.onclick = () => { menu.hidden = true; queueClipAsset(); };
    const copy = menu.querySelector("[data-context-copy]");
    const paste = menu.querySelector("[data-context-paste]");
    const group = menu.querySelector("[data-context-group]");
    const ungroup = menu.querySelector("[data-context-ungroup]");
    if (copy) copy.onclick = () => { copySelected(); menu.hidden = true; };
    if (paste) paste.onclick = () => { pasteSelected(); menu.hidden = true; };
    if (group) group.onclick = () => {
      if (selectedIds.size < 2) return toast("Select at least two clips");
      mutate(() => { const groupId = uid(); selectedIds.forEach(id => { const item = findClip(id); if (item) item.clip.groupId = groupId; }); });
      renderTimeline(); menu.hidden = true;
    };
    if (ungroup) ungroup.onclick = () => {
      mutate(() => selectedIds.forEach(id => { const item = findClip(id); if (item) delete item.clip.groupId; }));
      renderTimeline(); menu.hidden = true;
    };
    menu.hidden = false;
    const width = 300;
    menu.style.left = `${clamp(event.clientX, 8, innerWidth - width - 8)}px`;
    menu.style.top = `${clamp(event.clientY, 8, innerHeight - 130)}px`;
  }

  function makeClipNode(clip, track) {
    const node = document.createElement("div");
    const strip = document.createElement("i");
    const body = document.createElement("div");
    const title = document.createElement("strong");
    const left = document.createElement("i");
    const right = document.createElement("i");
    const asset = assetMap.get(clip.assetId);
    const mediaKind = asset?.kind || track.kind;
    node.className = `movie-clip ${mediaKind.toLowerCase()}${selectedIds.has(clip.id) ? " selected" : ""}${clip.groupId ? " joined" : ""}`;
    node.dataset.clipId = clip.id;
    node.style.left = `${clip.start / 1000 * zoom}px`;
    node.style.width = `${Math.max(8, clip.duration / 1000 * zoom)}px`;
    node.style.height = `${Math.max(13, track.height - 4)}px`;
    node.classList.add(`track-level-${clamp(Math.round(Number(track.height || defaultTrackHeight) / 21), 1, 10)}`);
    node.classList.toggle("compact-height", Number(track.height) <= 63);
    strip.className = "movie-clip-strip";
    if (mediaKind === "VIDEO" && (asset?.filmstripUrl || asset?.thumbnailUrl)) {
      const interval = asset.filmstripIntervalMs || 2000;
      const total = asset.filmstripFrameCount || 1;
      const frameSize = Math.max(16, Number(track.height || defaultTrackHeight) - 4);
      const visible = Math.max(1, Math.ceil(clip.duration / interval));
      const first = Math.floor(clip.sourceStart / interval);
      node.style.setProperty("--movie-frame-height", `${frameSize}px`);
      for (let offset = 0; offset < visible; offset += 1) {
        const frame = document.createElement("b");
        const index = Math.min(total - 1, first + offset);
        frame.className = "movie-clip-frame";
        frame.style.backgroundImage = `url("${asset.filmstripUrl || asset.thumbnailUrl}")`;
        if (asset.filmstripUrl) {
          frame.style.backgroundSize = `${total * frameSize}px ${frameSize}px`;
          frame.style.backgroundPosition = `-${index * frameSize}px 0`;
        }
        strip.append(frame);
      }
      if (!asset.filmstripUrl) node.classList.add("fallback-strip");
      node.append(strip);
    }
    if (asset?.waveformUrl) {
      const wave = document.createElement("img"); wave.className = `movie-clip-wave${mediaKind === "VIDEO" ? " video-wave" : ""}`; wave.src = asset.waveformUrl; wave.alt = ""; node.append(wave);
    }
    (clip.volumeKeyframes || []).forEach((point, index) => {
      const key = document.createElement("button"); key.type = "button"; key.className = "movie-clip-keyframe";
      key.style.left = `${clamp(point.time / Math.max(1, clip.duration), 0, 1) * 100}%`;
      key.title = `Audio keyframe ${index + 1}`;
      key.onclick = event => { event.stopPropagation(); selectClip(clip.id, false); setPlayhead(clip.start + point.time, true, false); };
      node.append(key);
    });
    body.className = "movie-clip-body"; title.textContent = clip.name; body.append(title);
    left.className = "movie-trim left"; right.className = "movie-trim right";
    node.append(left, body, right);
    node.onclick = event => {
      event.stopPropagation();
      if (node.dataset.suppressClick === "true") { node.dataset.suppressClick = ""; return; }
      if (!selectedIds.has(clip.id)) selectClip(clip.id, false, event.ctrlKey || event.metaKey || event.shiftKey);
    };
    node.ondblclick = event => { event.stopPropagation(); selectClip(clip.id, false); togglePlayback(); };
    node.oncontextmenu = event => {
      pasteTargetTrackId = track.id;
      if (!selectedIds.has(clip.id)) selectClip(clip.id, false);
      showClipContext(event, clip);
    };
    node.onpointerdown = event => {
      if (event.target === left || event.target === right) return;
      beginClipGesture(event, clip, track, "move", node);
    };
    left.onpointerdown = event => beginClipGesture(event, clip, track, "left", node);
    right.onpointerdown = event => beginClipGesture(event, clip, track, "right", node);
    return node;
  }

  function renderRuler(durationMs) {
    ruler.replaceChildren();
    const major = zoom >= 80 ? 1000 : zoom >= 30 ? 5000 : 10000;
    const minor = major / 5;
    for (let at = 0; at <= durationMs; at += minor) {
      const tick = document.createElement("i"); tick.className = `movie-ruler-tick${at % major === 0 ? " major" : ""}`; tick.style.left = `${at / 1000 * zoom}px`; ruler.append(tick);
      if (at % major === 0) { const label = document.createElement("span"); label.className = "movie-ruler-label"; label.style.left = tick.style.left; label.textContent = clock(at).slice(0, 5); ruler.append(label); }
    }
  }

  function renderTimeline() {
    const scrollState = {left: timelineScroll.scrollLeft, top: timelineScroll.scrollTop};
    normalizeTimeline();
    const duration = visibleDuration();
    timelineCanvas.style.width = `${Math.max(timelineScroll.clientWidth || 600, duration / 1000 * zoom)}px`;
    const minor = Math.max(4, (zoom >= 50 ? 200 : zoom >= 20 ? 1000 : 2000) / 1000 * zoom);
    tracksNode.style.setProperty("--grid-step", `${minor}px`); tracksNode.style.setProperty("--grid-step-minus", `${Math.max(1, minor - 1)}px`);
    headsNode.replaceChildren(); tracksNode.replaceChildren(); renderRuler(duration);
    timeline.tracks.forEach(track => {
      headsNode.append(makeTrackHead(track));
      const lane = document.createElement("div"); lane.className = `movie-track-lane${track.locked ? " locked" : ""}${track.hidden ? " hidden-track" : ""}${track.displayMode === "WAVEFORM" ? " waveform-mode" : ""}`; lane.dataset.trackId = track.id;
      lane.style.height = `${Number(track.height || defaultTrackHeight)}px`;
      lane.onpointerenter = () => { pasteTargetTrackId = track.id; };
      lane.onpointermove = () => { pasteTargetTrackId = track.id; };
      lane.onpointerdown = event => {
        if (event.target !== lane) return;
        beginLaneGesture(event, lane);
      };
      lane.ondragover = event => { if (!track.locked && canEdit) event.preventDefault(); };
      lane.ondrop = event => {
        event.preventDefault();
        let assetIds = [];
        try { assetIds = JSON.parse(event.dataTransfer.getData("text/asset-ids") || "[]"); } catch (_) {}
        if (!assetIds.length) assetIds = [event.dataTransfer.getData("text/asset-id")].filter(Boolean);
        addClipGroup(track, assetIds, (event.clientX - lane.getBoundingClientRect().left) / zoom * 1000);
        requestAnimationFrame(() => {
          const placedLane = q(`.movie-track-lane[data-track-id="${CSS.escape(track.id)}"]`);
          placedLane?.classList.add("placed");
          setTimeout(() => placedLane?.classList.remove("placed"), 320);
        });
      };
      track.clips.sort((a, b) => a.start - b.start).forEach(clip => lane.append(makeClipNode(clip, track)));
      if (track.displayMode === "WAVEFORM") {
        const guide = document.createElement("i"); guide.className = "movie-loudness-guide";
        guide.style.top = `${clamp(1 - track.loudnessGuide / 2, 0, 1) * 100}%`;
        guide.title = `Target volume ${track.loudnessGuide.toFixed(2)}`;
        lane.append(guide);
      }
      if (!track.clips.length) lane.innerHTML = '<span class="movie-empty">Drop a clip anywhere across this track</span>';
      tracksNode.append(lane);
    });
    setPlayhead(playheadMs, false, false);
    q("[data-zoom-label]").textContent = `${zoom} px/s`;
    zoomInput.value = zoom;
    requestAnimationFrame(() => { timelineScroll.scrollLeft = scrollState.left; timelineScroll.scrollTop = scrollState.top; });
  }

  function changeZoom(next, anchorClientX = null) {
    const previous = zoom;
    const bounds = timelineScroll.getBoundingClientRect();
    const anchor = anchorClientX == null ? timelineScroll.clientWidth / 2 : clamp(anchorClientX - bounds.left, 0, timelineScroll.clientWidth);
    const anchorMs = (timelineScroll.scrollLeft + anchor) / previous * 1000;
    zoom = clamp(Math.round(next), 1, 160);
    localStorage.setItem("studio-movie-zoom", String(zoom));
    renderTimeline();
    requestAnimationFrame(() => { timelineScroll.scrollLeft = Math.max(0, anchorMs / 1000 * zoom - anchor); });
  }

  function removeTargetTrack() {
    const selectedTrack = findClip(selectedId)?.track;
    const track = selectedTrack || timeline.tracks.find(item => item.id === pasteTargetTrackId);
    if (!track) return toast("Point to a track or select one of its clips first");
    if (track.clips.length) return toast("Remove clips before deleting this track");
    mutate(() => { timeline.tracks = timeline.tracks.filter(item => item.id !== track.id); });
    pasteTargetTrackId = null;
    renderTimeline();
  }

  function renderLibrary() {
    if (!libraryGrid) return;
    const kind = q("[data-media-kind]")?.value || "ALL";
    const sort = q("[data-media-sort]")?.value || "latest";
    let items = libraryAssets.filter(asset => !timeline.mediaAssetIds.includes(asset.id) && (libraryScope === "workspace" || asset.attached) && (kind === "ALL" || asset.kind === kind));
    if (sort === "alpha") items.sort((a, b) => a.name.localeCompare(b.name));
    q("[data-media-library-count]").textContent = items.length;
    libraryGrid.replaceChildren();
    items.forEach(asset => {
      const button = document.createElement("button");
      const visual = document.createElement("div");
      const copy = document.createElement("div");
      button.type = "button"; button.className = "movie-library-card";
      visual.className = "movie-library-card-visual"; copy.className = "movie-library-card-copy";
      if (asset.thumbnailUrl || asset.waveformUrl) { const image = document.createElement("img"); image.src = asset.thumbnailUrl || asset.waveformUrl; image.alt = ""; visual.append(image); }
      else visual.textContent = asset.kind;
      copy.innerHTML = `<strong>${asset.name}</strong><small>${asset.kind} / ${mediaStatus(asset)}</small><small>${asset.attached ? "Attached to project" : "Workspace media"}</small>`;
      button.append(visual, copy);
      button.onclick = () => {
        mutate(() => { if (!timeline.mediaAssetIds.includes(asset.id)) timeline.mediaAssetIds.push(asset.id); });
        renderLibrary(); renderBin();
        bin.querySelector(`[data-asset-id="${asset.id}"]`)?.scrollIntoView({block: "nearest"});
      };
      libraryGrid.append(button);
    });
    if (!items.length) libraryGrid.innerHTML = '<p class="muted">No matching media</p>';
  }

  async function loadHistory() {
    historyList.innerHTML = '<p class="muted">Loading history</p>';
    try {
      const data = await requestJson(`${root.dataset.historyUrl}?timelineId=${encodeURIComponent(timelineId)}`);
      historyList.replaceChildren();
      if (!data.items.length) { historyList.innerHTML = '<p class="muted">No previous saves yet</p>'; return; }
      data.items.forEach(item => {
        const row = document.createElement("div"); const copy = document.createElement("div");
        row.className = "movie-history-row"; copy.className = "movie-history-copy";
        copy.innerHTML = `<strong>${item.title}</strong><span>${item.reason} / ${new Date(item.createdAt).toLocaleString()} / ${item.createdBy}</span>`; row.append(copy);
        if (data.canRestore) {
          const restore = document.createElement("button"); restore.type = "button"; restore.className = "secondary"; restore.textContent = "Restore";
          restore.onclick = async () => {
            restore.disabled = true;
            try {
              const data = await requestJson(item.restoreUrl, {method: "POST", headers: {"X-CSRFToken": csrfToken()}});
              timeline = data.timeline; selectedId = null; selectedIds.clear();
              q("[data-movie-title]").value = data.title; q("[data-movie-ratio]").value = data.aspectRatio; q("[data-movie-resolution]").value = data.resolution; q("[data-movie-fps]").value = data.fps;
              savedSignature = signature(); historyUndo = []; historyRedo = []; applyCanvas(); renderTimeline(); renderInspector(); updateDirty(); updateHistoryButtons(); toast("Timeline restored"); await loadHistory();
            } catch (error) { restore.disabled = false; toast(error.message, "error"); }
          };
          row.append(restore);
        }
        historyList.append(row);
      });
    } catch (error) { historyList.textContent = error.message; }
  }

  async function montageAction(action, extra = {}) {
    const data = await requestJson(root.dataset.editsUrl, {
      method: "POST",
      headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
      body: JSON.stringify({action, timelineId, ...extra}),
    });
    return data;
  }

  const editSelect = q("[data-movie-edit-select]");
  editSelect?.addEventListener("change", async () => {
    const item = movieEdits.find(edit => edit.id === editSelect.value);
    if (!item) return;
    if (dirty && !await saveTimeline(true)) { editSelect.value = timelineId; return; }
    try {
      if (!item.attached) {
        const attached = await requestJson(root.dataset.editsUrl, {
          method: "POST", headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
          body: JSON.stringify({action: "attach", timelineId: item.id}),
        });
        item.attached = attached.attached;
      }
      window.open(item.openUrl, "_blank", "noopener");
    } catch (error) { editSelect.value = timelineId; toast(error.message, "error"); }
  });
  q("[data-edit-create]")?.addEventListener("click", async () => {
    try { const item = await montageAction("create", {title: "Untitled edit"}); window.open(item.openUrl, "_blank", "noopener"); }
    catch (error) { toast(error.message, "error"); }
  });
  q("[data-edit-copy]")?.addEventListener("click", async () => {
    try { const item = await montageAction("copy", {title: `${q("[data-movie-title]").value} copy`}); window.open(item.openUrl, "_blank", "noopener"); }
    catch (error) { toast(error.message, "error"); }
  });
  const importDialog = q("[data-import-dialog]");
  let pendingImport = null;
  q("[data-edit-import]")?.addEventListener("click", () => q("[data-edit-import-file]")?.click());
  q("[data-edit-import-file]")?.addEventListener("change", async event => {
    const file = event.target.files?.[0]; if (!file) return;
    try {
      if (/\.zip$/i.test(file.name) || file.type === "application/zip") {
        pendingImport = {file, parsed: null, timeline: null};
        q("[data-import-file-name]").textContent = `${file.name} / media bundle`;
        q("[data-import-mode]").value = "project";
        q("[data-import-mode]").disabled = true;
        q("[data-import-track-field]").hidden = true;
        importDialog.showModal();
        event.target.value = "";
        return;
      }
      const parsed = JSON.parse(await file.text());
      const importedTimeline = parsed.timeline || parsed;
      if (!Array.isArray(importedTimeline.tracks)) throw new Error("The MC Project file has no tracks");
      pendingImport = {file, parsed, timeline: importedTimeline};
      q("[data-import-file-name]").textContent = file.name;
      const trackSelect = q("[data-import-track]"); trackSelect.replaceChildren();
      importedTimeline.tracks.forEach((track, index) => { const option = document.createElement("option"); option.value = index; option.textContent = `${track.name || track.kind || "Track"} (${track.clips?.length || 0} clips)`; trackSelect.append(option); });
      q("[data-import-mode]").value = "project"; q("[data-import-track-field]").hidden = true;
      q("[data-import-mode]").disabled = false;
      importDialog.showModal();
    } catch (error) { toast(error.message || "The MC Project file is invalid", "error"); }
    event.target.value = "";
  });
  q("[data-import-mode]")?.addEventListener("change", event => { q("[data-import-track-field]").hidden = event.target.value !== "track"; });
  const closeImport = () => { pendingImport = null; q("[data-import-mode]").disabled = false; importDialog?.close(); };
  q("[data-import-close]")?.addEventListener("click", closeImport); q("[data-import-cancel]")?.addEventListener("click", closeImport);
  q("[data-import-apply]")?.addEventListener("click", async () => {
    if (!pendingImport) return;
    if (q("[data-import-mode]").value === "track" && pendingImport.timeline) {
      const source = pendingImport.timeline.tracks[Number(q("[data-import-track]").value)];
      if (!source) return;
      mutate(() => {
        const track = structuredClone(source); track.id = uid(); track.name = `${track.name || track.kind || "Track"} imported`;
        track.clips = (track.clips || []).map(clip => ({...clip, id: uid(), groupId: clip.groupId ? uid() : undefined}));
        timeline.tracks.push(track);
      });
      closeImport(); renderTimeline(); renderInspector(); toast("Track imported");
      return;
    }
    const form = new FormData(); form.append("file", pendingImport.file);
    try {
      const item = await requestJson(root.dataset.editsUrl, {method: "POST", headers: {"X-CSRFToken": csrfToken()}, body: form});
      window.open(item.openUrl, "_blank", "noopener");
    } catch (error) { toast(error.message, "error"); }
  });
  q("[data-edit-detach]")?.addEventListener("click", async () => {
    try { await montageAction("detach"); location.assign(root.dataset.saveUrl); }
    catch (error) { toast(error.message, "error"); }
  });
  const editSettingsDialog = q("[data-edit-settings-dialog]");
  const editSettingsForm = q("[data-edit-settings-form]");
  const editWorkspacePattern = q("[data-edit-workspace-pattern]");
  const closeEditSettings = () => {
    editSettingsDialog?.close();
  };
  q("[data-edit-settings-open]")?.addEventListener("click", () => {
    if (editWorkspacePattern) editWorkspacePattern.value = workspacePattern;
    editSettingsDialog?.showModal();
  });
  [q("[data-edit-settings-close]"), q("[data-edit-settings-cancel]")].forEach(button => button?.addEventListener("click", closeEditSettings));
  editSettingsDialog?.addEventListener("click", event => {
    if (event.target === editSettingsDialog) closeEditSettings();
  });
  editSettingsForm?.addEventListener("submit", async event => {
    event.preventDefault();
    applyWorkspacePattern(editWorkspacePattern?.value || "grid");
    localStorage.setItem(workspacePatternKey, workspacePattern);
    closeEditSettings();
    toast("MC Project settings saved", "success");
  });
  let archiveArmedUntil = 0;
  q("[data-edit-archive]")?.addEventListener("click", async event => {
    if (Date.now() > archiveArmedUntil) {
      archiveArmedUntil = Date.now() + 5000; event.currentTarget.classList.add("warning");
      toast("Click Archive again within five seconds to confirm"); return;
    }
    try { await montageAction("archive"); location.assign(root.dataset.saveUrl); }
    catch (error) { toast(error.message, "error"); }
  });
  const editDeleteDialog = q("[data-edit-delete-dialog]");
  const closeEditDelete = () => editDeleteDialog?.close();
  q("[data-edit-delete]")?.addEventListener("click", () => editDeleteDialog?.showModal());
  [q("[data-edit-delete-close]"), q("[data-edit-delete-cancel]")].forEach(button => button?.addEventListener("click", closeEditDelete));
  editDeleteDialog?.addEventListener("click", event => {
    if (event.target === editDeleteDialog) closeEditDelete();
  });
  q("[data-edit-delete-confirm]")?.addEventListener("click", async () => {
    try {
      await montageAction("delete");
      location.assign(root.dataset.saveUrl);
    } catch (error) {
      toast(error.message, "error");
    }
  });

  q("[data-media-library-open]")?.addEventListener("click", () => { libraryScope = "project"; qa("[data-media-scope]").forEach(button => button.classList.toggle("active", button.dataset.mediaScope === libraryScope)); renderLibrary(); libraryDialog.showModal(); });
  q("[data-media-library-close]")?.addEventListener("click", () => libraryDialog.close());
  libraryDialog?.addEventListener("click", event => { if (event.target === libraryDialog) libraryDialog.close(); });
  qa("[data-media-scope]").forEach(button => button.onclick = () => { libraryScope = button.dataset.mediaScope; qa("[data-media-scope]").forEach(item => item.classList.toggle("active", item === button)); renderLibrary(); });
  q("[data-media-kind]")?.addEventListener("change", renderLibrary);
  q("[data-media-sort]")?.addEventListener("change", renderLibrary);
  const uploadMediaFiles = (filesToUpload, targetFolderId = currentMediaFolderId) => {
    const input = q("[data-media-files]");
    const files = [...filesToUpload].slice(0, 10);
    if (!files.length) return;
    const batchId = uid();
    pendingUploads = files.map(file => ({id: `${batchId}-${file.name}`, name: file.name, progress: 0}));
    renderBin();
    const form = new FormData(); files.forEach(file => form.append("files", file));
    const progress = q("[data-media-progress]"); const bar = progress.querySelector("i"); progress.hidden = false; bar.style.width = "0";
    const xhr = new XMLHttpRequest(); xhr.open("POST", root.dataset.mediaUrl); xhr.setRequestHeader("X-CSRFToken", csrfToken());
    xhr.upload.onprogress = update => { if (update.lengthComputable) { const percent = Math.round(update.loaded / update.total * 100); bar.style.width = `${percent}%`; pendingUploads.forEach(item => item.progress = percent); renderBin(); } };
    xhr.onload = async () => { pendingUploads = []; progress.hidden = true; input.value = ""; let data = {}; try { data = JSON.parse(xhr.responseText); } catch (_) {} if (xhr.status >= 400) toast(data.error || "Upload failed", "error"); else { mutate(() => { const uploadedIds = []; (data.items || []).forEach(item => { uploadedIds.push(item.id); if (!timeline.mediaAssetIds.includes(item.id)) timeline.mediaAssetIds.push(item.id); if (!timeline.mediaOrder.includes(item.id)) timeline.mediaOrder.push(item.id); }); const folder = timeline.mediaFolders.find(item => item.id === targetFolderId); if (folder) uploadedIds.forEach(id => { if (!folder.assetIds.includes(id)) folder.assetIds.push(id); }); }); if (data.errors?.length) toast(data.errors.join(" / "), "error"); } await refreshMedia(); };
    xhr.onerror = () => { pendingUploads = []; renderBin(); progress.hidden = true; toast("Upload failed", "error"); };
    xhr.send(form);
  };
  q("[data-media-upload-form]")?.addEventListener("submit", event => {
    event.preventDefault();
    uploadMediaFiles(q("[data-media-files]").files);
  });
  const clipboardFileName = (blob, index) => {
    const subtype = String(blob.type || "").split("/")[1] || "bin";
    const extension = subtype === "jpeg" ? "jpg" : subtype.replace(/[^a-z0-9]+/gi, "") || "bin";
    const stamp = new Date().toISOString().replace(/[:.]/g, "-");
    return `clipboard-${stamp}-${index + 1}.${extension}`;
  };
  const pasteSystemMedia = async (targetFolderId = currentMediaFolderId) => {
    if (!navigator.clipboard?.read) return toast("Use Ctrl+V to paste a copied media file", "error");
    try {
      const clipboardItems = await navigator.clipboard.read();
      const files = [];
      for (const item of clipboardItems) {
        const type = item.types.find(value => /^(image|video|audio)\//.test(value));
        if (!type) continue;
        const blob = await item.getType(type);
        files.push(new File([blob], clipboardFileName(blob, files.length), {type, lastModified: Date.now()}));
      }
      if (!files.length) return toast("Clipboard does not contain a supported media file", "error");
      uploadMediaFiles(files, targetFolderId);
    } catch (error) {
      toast(error.message || "Clipboard access was denied", "error");
    }
  };
  let mediaDragDepth = 0;
  [q(".movie-bin")].filter(Boolean).forEach(target => {
    target.addEventListener("dragenter", event => { if ([...event.dataTransfer.types].includes("application/x-lexamora-media") || ![...event.dataTransfer.types].includes("Files")) return; event.preventDefault(); mediaDragDepth += 1; target.classList.add("file-drop-active"); });
    target.addEventListener("dragover", event => { if ([...event.dataTransfer.types].includes("application/x-lexamora-media") || ![...event.dataTransfer.types].includes("Files")) return; event.preventDefault(); event.dataTransfer.dropEffect = "copy"; });
    target.addEventListener("dragleave", () => { mediaDragDepth = Math.max(0, mediaDragDepth - 1); if (!mediaDragDepth) target.classList.remove("file-drop-active"); });
    target.addEventListener("drop", event => { if ([...event.dataTransfer.types].includes("application/x-lexamora-media") || !event.dataTransfer.files.length) return; event.preventDefault(); event.stopPropagation(); mediaDragDepth = 0; target.classList.remove("file-drop-active"); uploadMediaFiles(event.dataTransfer.files); });
  });
  q("[data-movie-media-properties-close]")?.addEventListener("click", () => q("[data-movie-media-properties]")?.close());
  qa("[data-media-folder-cancel]").forEach(button => button.addEventListener("click", () => folderDialog?.close()));
  folderDialog?.querySelector("[data-media-folder-name]")?.addEventListener("keydown", event => {
    if (event.key !== "Enter") return;
    event.preventDefault();
    folderDialog.querySelector("[data-media-folder-save]")?.click();
  });
  q("[data-media-folder-save]")?.addEventListener("click", () => {
    const mode = folderDialog.dataset.mode;
    const input = folderDialog.querySelector("[data-media-folder-name]");
    const name = input.value.trim();
    if (mode !== "delete" && !name) return input.focus();
    mutate(() => {
      if (mode === "create") {
        const folder = {id: uid(), name, assetIds: []}; timeline.mediaFolders.push(folder); currentMediaFolderId = folder.id;
      } else if (mode === "rename") {
        const folder = timeline.mediaFolders.find(item => item.id === currentMediaFolderId); if (folder) folder.name = name;
      } else {
        timeline.mediaFolders = timeline.mediaFolders.filter(item => item.id !== currentMediaFolderId); currentMediaFolderId = null;
      }
    });
    folderDialog.close(); renderBin();
  });
  const hideMediaContext = () => { if (mediaContext) mediaContext.hidden = true; mediaContextAsset = null; mediaContextFolderId = null; };
  q("[data-media-context-select-all]")?.addEventListener("click", () => {
    selectedMediaIds = new Set(qa(".movie-bin-item[data-asset-id]").map(item => item.dataset.assetId));
    mediaSelectionActive = true;
    hideMediaContext();
    renderMediaSelection();
  });
  q("[data-media-context-copy]")?.addEventListener("click", async () => {
    const folder = timeline.mediaFolders.find(item => item.id === mediaContextFolderId);
    hideMediaContext();
    if (folder) { await navigator.clipboard.writeText(folder.name); toast("Folder name copied"); return; }
    await copySelectedMedia();
  });
  q("[data-media-context-cut]")?.addEventListener("click", () => {
    const selected = selectedMediaAssets();
    if (!selected.length) return;
    mediaClipboardIds = selected.map(asset => asset.id);
    mediaClipboardMode = "cut";
    hideMediaContext();
    toast(`${selected.length} media item${selected.length === 1 ? "" : "s"} ready to move`);
  });
  q("[data-media-context-paste]")?.addEventListener("click", () => {
    const folderId = mediaContextFolderId || currentMediaFolderId;
    hideMediaContext();
    if (folderId && mediaClipboardIds.length) { moveAssetsToFolder(mediaClipboardIds, folderId); return; }
    if (mediaClipboardIds.some(id => assetMap.has(id))) pasteSelectedMedia();
    else pasteSystemMedia(folderId);
  });
  q("[data-media-context-root]")?.addEventListener("click", () => {
    const ids = selectedMediaAssets().map(asset => asset.id);
    hideMediaContext();
    if (!ids.length) return;
    moveAssetsToFolder(ids, null);
    currentMediaFolderId = null;
    renderBin();
  });
  q("[data-media-context-download]")?.addEventListener("click", event => {
    event.preventDefault();
    const selected = selectedMediaAssets().filter(asset => asset.downloadUrl);
    hideMediaContext();
    selected.forEach((asset, index) => setTimeout(() => {
      const link = document.createElement("a");
      link.href = asset.downloadUrl;
      link.download = asset.name || "media";
      document.body.append(link);
      link.click();
      link.remove();
    }, index * 120));
  });
  q("[data-media-context-delete]")?.addEventListener("click", () => {
    const folderId = mediaContextFolderId;
    hideMediaContext();
    if (folderId) {
      mutate(() => { timeline.mediaFolders = timeline.mediaFolders.filter(item => item.id !== folderId); });
      selectedMediaFolderId = null; renderBin(); return;
    }
    removeSelectedMedia();
  });
  q("[data-media-context-properties]")?.addEventListener("click", () => {
    const folder = timeline.mediaFolders.find(item => item.id === mediaContextFolderId);
    hideMediaContext();
    if (folder) {
      const dialog = q("[data-movie-media-properties]");
      const content = dialog?.querySelector("[data-movie-media-properties-content]");
      if (dialog && content) {
        content.innerHTML = `<div><strong>Name</strong><span></span></div><div><strong>Type</strong><span>Media folder</span></div><div><strong>Items</strong><span>${folder.assetIds?.length || 0}</span></div>`;
        content.querySelector("span").textContent = folder.name;
        dialog.showModal();
      }
      return;
    }
    showSelectedMediaProperties();
  });
  qa("[data-media-remove-cancel]").forEach(button => button.addEventListener("click", () => q("[data-media-remove-dialog]")?.close()));
  bin?.addEventListener("contextmenu", event => {
    if (event.target.closest(".movie-bin-item,.movie-bin-folder-tile,button,input,select,label")) return;
    event.preventDefault();
    mediaSelectionActive = true;
    mediaContextAsset = null;
    setMediaContextMode("free");
    mediaContext.style.left = `${Math.min(event.clientX, innerWidth - 200)}px`;
    mediaContext.style.top = `${Math.min(event.clientY, innerHeight - 230)}px`;
    mediaContext.hidden = false;
  });
  bin?.addEventListener("pointerdown", event => {
    if (event.button !== 0 || event.target.closest(".movie-bin-item,.movie-bin-folder-tile,button,input,select,label")) return;
    if (!event.shiftKey) return;
    event.preventDefault();
    mediaSelectionActive = true;
    const startX = event.clientX;
    const startY = event.clientY;
    const preserved = event.ctrlKey || event.metaKey ? new Set(selectedMediaIds) : new Set();
    const selection = document.createElement("i");
    selection.className = "movie-bin-marquee";
    selection.style.left = `${startX}px`;
    selection.style.top = `${startY}px`;
    document.body.append(selection);
    const move = next => {
      const left = Math.min(startX, next.clientX);
      const top = Math.min(startY, next.clientY);
      const right = Math.max(startX, next.clientX);
      const bottom = Math.max(startY, next.clientY);
      selection.style.left = `${left}px`;
      selection.style.top = `${top}px`;
      selection.style.width = `${right - left}px`;
      selection.style.height = `${bottom - top}px`;
      selectedMediaIds = new Set(preserved);
      qa(".movie-bin-item[data-asset-id]").forEach(item => {
        const rect = item.getBoundingClientRect();
        if (rect.right >= left && rect.left <= right && rect.bottom >= top && rect.top <= bottom) selectedMediaIds.add(item.dataset.assetId);
      });
      renderMediaSelection();
    };
    const finish = () => {
      selection.remove();
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", finish);
      window.removeEventListener("pointercancel", finish);
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", finish);
    window.addEventListener("pointercancel", finish);
  });
  document.addEventListener("paste", event => {
    if (!mediaSelectionActive || event.target.closest("input,textarea,[contenteditable=true]")) return;
    const files = [...(event.clipboardData?.files || [])].filter(file => /^(image|video|audio)\//.test(file.type));
    if (!files.length) return;
    event.preventDefault();
    uploadMediaFiles(files, currentMediaFolderId);
  });
  document.addEventListener("pointerdown", event => { if (mediaContext && !mediaContext.hidden && !mediaContext.contains(event.target)) hideMediaContext(); });

  qa("[data-add-track]").forEach(button => button.onclick = () => addTrack(button.dataset.addTrack));
  q("[data-delete-track]")?.addEventListener("click", removeTargetTrack);
  q("[data-auto-cut]")?.addEventListener("click", () => {
    let track = timeline.tracks.find(item => item.kind === "VIDEO" && !item.locked);
    if (!track) track = addTrack("VIDEO");
    const target = track;
    mutate(() => {
      let cursor = 0;
      target.clips = assets.filter(asset => asset.status === "READY" && asset.kind === "VIDEO").map(asset => { const duration = Math.max(200, asset.durationMs || 8000); const clip = {id: uid(), assetId: asset.id, sourceAssetId: asset.id, name: asset.name, start: cursor, sourceStart: 0, duration, volume: 1, speed: 1, speedMethod: "FRAME_SAMPLE", fadeIn: 0, fadeOut: 0, volumeKeyframes: []}; cursor += duration; return clip; });
    });
    renderTimeline();
  });
  qa("[data-keyframe-step]").forEach(button => button.onclick = () => {
    const direction = Number(button.dataset.keyframeStep);
    const points = timeline.tracks.flatMap(track => track.clips.flatMap(clip =>
      (clip.volumeKeyframes || []).map(point => clip.start + Number(point.time || 0))
    )).sort((a, b) => a - b);
    const target = direction < 0
      ? points.filter(point => point < playheadMs - 1).at(-1)
      : points.find(point => point > playheadMs + 1);
    if (target == null) return toast(direction < 0 ? "No previous keyframe" : "No next keyframe");
    stopPlayback(); setPlayhead(target);
  });
  q("[data-preview-play]").onclick = togglePlayback;
  q("[data-preview-stop]").onclick = () => { if (standalonePreviewAssetId) { preview.pause(); preview.currentTime = 0; previewStatus.textContent = "Ready"; q("[data-preview-play]").innerHTML = "&#9654;"; return; } stopPlayback(true); };
  qa("[data-preview-step]").forEach(button => bindHold(button, () => { leaveStandalonePreview(); stopPlayback(); setPlayhead(playheadMs + Number(button.dataset.previewStep) * frameMs()); }));
  previewScrub.oninput = event => { leaveStandalonePreview(); stopPlayback(); setPlayhead(Number(event.target.value)); };
  preview.addEventListener("waiting", () => { previewStatus.textContent = "Buffering"; });
  preview.addEventListener("playing", () => { previewStatus.textContent = "Playing"; });
  preview.addEventListener("ended", () => {
    if (!standalonePreviewAssetId) return;
    q("[data-preview-play]").innerHTML = "&#9654;";
    previewStatus.textContent = "Ready";
  });
  preview.addEventListener("play", () => { if (!playing && !standalonePreviewAssetId) togglePlayback(); });
  preview.addEventListener("loadedmetadata", () => updatePreviewGeometry());
  preview.addEventListener("error", () => { previewStatus.textContent = preview.error?.message || "Preview failed"; });
  previewStage.addEventListener("pointerdown", beginPreviewPan);
  qa("[data-preview-resize]").forEach(handle => handle.addEventListener("pointerdown", beginPreviewResize));
  qa("[data-canvas-frame]").forEach(button => bindHold(button, () => nudgeCanvasFrame(button.dataset.canvasFrame), 210, 75));
  const forwardVerticalWheelToWorkspace = event => {
    if (event.ctrlKey || !event.deltaY || Math.abs(event.deltaX) > Math.abs(event.deltaY)) return false;
    event.preventDefault();
    if (editorViewport) {
      editorViewport.scrollTop += event.deltaY;
    }
    return true;
  };
  previewStage.addEventListener("wheel", forwardVerticalWheelToWorkspace, {passive: false});
  playheadNode.addEventListener("pointerdown", beginPlayheadGesture);
  previewZoomInput?.addEventListener("input", event => {
    previewZoomManual = true;
    previewZoom = clamp(Number(event.target.value), .05, 2);
    localStorage.setItem(previewZoomStorageKey, String(previewZoom));
    applyPreviewZoom();
    updatePreviewGeometry();
  });
  const setPreviewZoomPercent = value => {
    previewZoomManual = true;
    previewZoom = clamp((Number(value) || 100) / 100, .05, 2);
    localStorage.setItem(previewZoomStorageKey, String(previewZoom));
    applyPreviewZoom();
    updatePreviewGeometry();
  };
  previewZoomValue?.addEventListener("change", event => setPreviewZoomPercent(event.target.value));
  q("[data-preview-zoom-down]")?.addEventListener("click", () => setPreviewZoomPercent(Math.round(previewZoom * 100) - 5));
  q("[data-preview-zoom-up]")?.addEventListener("click", () => setPreviewZoomPercent(Math.round(previewZoom * 100) + 5));
  frameSizeInput?.addEventListener("input", event => setSelectedFrameScale(event.target.value));
  q("[data-frame-size-down]")?.addEventListener("click", () => setSelectedFrameScale((Number(frameScaleValue?.value || 100) - 1) / 100));
  q("[data-frame-size-up]")?.addEventListener("click", () => setSelectedFrameScale((Number(frameScaleValue?.value || 100) + 1) / 100));
  qa("[data-inspector-tab]").forEach(button => button.addEventListener("click", () => { inspectorTab = button.dataset.inspectorTab; renderInspector(); }));
  q("[data-timeline-split]").onclick = splitSelected;
  q("[data-clip-copy]")?.addEventListener("click", copySelected);
  q("[data-clip-paste]")?.addEventListener("click", pasteSelected);
  q("[data-clip-join]")?.addEventListener("click", joinSelected);
  q("[data-clip-render-toolbar]")?.addEventListener("click", queueClipAsset);
  q("[data-delete-selected]")?.addEventListener("click", deleteSelected);
  q("[data-selected-up]")?.addEventListener("click", () => moveSelectedToAdjacentTrack(-1));
  q("[data-selected-down]")?.addEventListener("click", () => moveSelectedToAdjacentTrack(1));
  q("[data-selected-track-up]")?.addEventListener("click", () => moveSelectedTrack(-1));
  q("[data-selected-track-down]")?.addEventListener("click", () => moveSelectedTrack(1));
  qa("[data-clip-nudge]").forEach(button => bindHold(button, () => nudgeSelected(Number(button.dataset.clipNudge))));
  q("[data-timeline-snap]").onclick = event => { snapping = !snapping; event.currentTarget.classList.toggle("active", snapping); toast(snapping ? "Snapping enabled" : "Snapping disabled"); };
  ruler.onpointerdown = event => {
    leaveStandalonePreview();
    stopPlayback();
    const update = next => setPlayhead((next.clientX - ruler.getBoundingClientRect().left) / zoom * 1000);
    update(event); capturePointerSafely(ruler, event.pointerId); ruler.onpointermove = update; ruler.onpointerup = () => { ruler.onpointermove = null; ruler.onpointerup = null; };
  };
  timelineScroll.addEventListener("scroll", () => { headsNode.style.transform = `translateY(-${timelineScroll.scrollTop}px)`; }, {passive: true});
  timelineScroll.addEventListener("wheel", event => {
    if (event.ctrlKey && Math.abs(event.deltaY) >= Math.abs(event.deltaX)) {
      event.preventDefault();
      changeZoom(zoom + (event.deltaY < 0 ? 4 : -4), event.clientX);
      return;
    }
    forwardVerticalWheelToWorkspace(event);
  }, {passive: false});
  zoomInput.oninput = event => changeZoom(Number(event.target.value));
  q("[data-zoom-out]").onclick = () => changeZoom(zoom - 4);
  q("[data-zoom-in]").onclick = () => changeZoom(zoom + 4);
  qa("[data-editor-undo]").forEach(button => { button.onclick = undo; });
  qa("[data-editor-redo]").forEach(button => { button.onclick = redo; });
  timelineCanvas.addEventListener("pointerdown", beginCanvasGesture);
  const saveAsDialog = q("[data-save-as-dialog]");
  const downloadMontageJson = () => {
    const item = movieEdits.find(edit => edit.id === timelineId);
    if (item?.exportUrl && !dirty) { location.assign(item.exportUrl); return; }
    const payload = {format: "lexamora-montage-project", version: 1, title: q("[data-movie-title]").value, aspectRatio: q("[data-movie-ratio]").value, resolution: q("[data-movie-resolution]").value, fps: Number(q("[data-movie-fps]").value), timeline};
    const blob = new Blob([JSON.stringify(payload, null, 2)], {type: "application/json"}); const link = document.createElement("a"); link.href = URL.createObjectURL(blob); link.download = `${q("[data-movie-title]").value || "draft"}-montage.json`; link.click(); URL.revokeObjectURL(link.href);
  };
  q("[data-export-timeline]").onclick = () => saveAsDialog?.showModal();
  q("[data-save-as-close]")?.addEventListener("click", () => saveAsDialog.close());
  q("[data-save-as-json]")?.addEventListener("click", () => { saveAsDialog.close(); downloadMontageJson(); });
  q("[data-save-as-bundle]")?.addEventListener("click", async () => {
    const item = movieEdits.find(edit => edit.id === timelineId);
    if (!item?.exportUrl) return;
    if (dirty && !await saveTimeline(true)) return;
    saveAsDialog.close(); location.assign(`${item.exportUrl}?bundle=media`);
  });
  q("[data-movie-history-toggle]").onclick = async () => { historyPanel.hidden = !historyPanel.hidden; if (!historyPanel.hidden) await loadHistory(); };
  q("[data-movie-history-close]").onclick = () => { historyPanel.hidden = true; };
  q("[data-render-toggle]")?.addEventListener("click", () => { renderPanel.hidden = !renderPanel.hidden; if (!renderPanel.hidden) refreshRenders(); });
  q("[data-render-close]")?.addEventListener("click", () => { renderPanel.hidden = true; });
  q("[data-render-start]")?.addEventListener("click", queueRender);
  q("[data-save-movie]")?.addEventListener("click", () => saveTimeline());
  qa("[data-movie-title],[data-movie-ratio],[data-movie-resolution],[data-movie-fps]").forEach(control => {
    const capture = () => settingSnapshots.set(control, currentState());
    control.addEventListener("focus", capture);
    control.addEventListener("pointerdown", capture);
    control.addEventListener("change", () => {
      const previous = settingSnapshots.get(control);
      if (previous && signature(previous) !== signature()) {
        historyUndo.push(previous);
        if (historyUndo.length > 100) historyUndo.shift();
        historyRedo = [];
      }
      if (control.matches("[data-movie-ratio]")) {
        applyCanvas();
      }
      if (control.matches("[data-movie-resolution]")) {
        applyPreviewZoom();
        requestAnimationFrame(() => {
          updatePreviewGeometry();
          syncPlayers(false, true);
        });
      }
      if (control.matches("[data-movie-fps]")) renderTimeline();
      updateDirty(); updateHistoryButtons(); scheduleAutosave();
    });
  });

  document.addEventListener("keydown", event => {
    const editing = event.target.matches("input,textarea,select") || event.target.isContentEditable;
    const shortcut = event.ctrlKey || event.metaKey;
    if (shortcut && (event.code === "KeyZ" || event.key.toLowerCase() === "z")) { event.preventDefault(); event.shiftKey ? redo() : undo(); return; }
    if (shortcut && (event.code === "KeyY" || event.key.toLowerCase() === "y")) { event.preventDefault(); redo(); return; }
    if (shortcut && (event.code === "KeyC" || event.key.toLowerCase() === "c") && !editing) {
      event.preventDefault();
      if (mediaSelectionActive && selectedMediaIds.size) copySelectedMedia();
      else copySelected();
      return;
    }
    if (shortcut && (event.code === "KeyV" || event.key.toLowerCase() === "v") && !editing) {
      if (mediaSelectionActive) {
        if (mediaClipboardIds.some(id => assetMap.has(id))) { event.preventDefault(); pasteSelectedMedia(); }
        return;
      }
      event.preventDefault();
      pasteSelected();
      return;
    }
    if (editing) return;
    if (event.code === "Space") { event.preventDefault(); togglePlayback(); }
    else if (event.key.toLowerCase() === "s" && !event.ctrlKey && !event.metaKey) { event.preventDefault(); splitSelected(); }
    else if ((event.key === "Delete" || event.key === "Backspace") && selectedId) { event.preventDefault(); deleteSelected(); }
    else if (event.key === "ArrowLeft" || event.key === "ArrowRight") { event.preventDefault(); const direction = event.key === "ArrowLeft" ? -1 : 1; if (selectedId && !event.ctrlKey && !event.metaKey) nudgeSelected(direction); else setPlayhead(playheadMs + direction * (event.shiftKey ? 1000 : frameMs())); }
    else if ((event.key === "ArrowUp" || event.key === "ArrowDown") && selectedId) { event.preventDefault(); moveSelectedToAdjacentTrack(event.key === "ArrowUp" ? -1 : 1); }
    else if ((event.ctrlKey || event.metaKey) && ["+", "=", "-"].includes(event.key)) { event.preventDefault(); changeZoom(zoom + (event.key === "-" ? -4 : 4)); }
  });
  addEventListener("beforeunload", event => { if (dirty) { event.preventDefault(); event.returnValue = ""; } });
  document.addEventListener("click", event => {
    const clipContext = q("[data-clip-context]");
    if (clipContext && !event.target.closest("[data-clip-context]")) clipContext.hidden = true;
    if (trackContext && !event.target.closest("[data-track-context]")) trackContext.hidden = true;
    if (
      selectedTrackId &&
      !event.target.closest(".movie-track-head") &&
      !event.target.closest("[data-selected-track-actions]") &&
      !event.target.closest("[data-track-context]")
    ) {
      selectTrack(null, {render: false});
    }
    qa("details[open]").forEach(details => {
      if (!event.target.closest("summary") || !details.contains(event.target)) details.removeAttribute("open");
    });
  });
  trackContext?.addEventListener("click", event => {
    const button = event.target.closest("[data-track-context-action]");
    if (!button) return;
    event.stopPropagation();
    performTrackAction(button.dataset.trackContextAction);
    trackContext.hidden = true;
  });
  document.addEventListener("pointerdown", event => {
    if (event.target.closest("[data-preview-play]")) return;
    if (playing) stopPlayback();
    if (standalonePreviewAssetId && !preview.paused) {
      preview.pause();
      previewStatus.textContent = "Ready";
      q("[data-preview-play]").innerHTML = "&#9654;";
    }
  }, {capture: true});
  let resizeTimer;
  addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      scheduleEditorViewportFit();
      applyPreviewZoom();
      updatePreviewGeometry();
      renderTimeline();
    }, 120);
  });
  window.visualViewport?.addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      scheduleEditorViewportFit();
      applyPreviewZoom();
      updatePreviewGeometry();
      renderTimeline();
    }, 120);
  });

  const mediaViewCycle = q("[data-media-view-cycle]");
  const mediaViews = ["large", "small", "list"];
  const mediaViewCommands = {large: "gridLarge", small: "gridSmall", list: "list"};
  const syncMediaViewCycle = () => {
    if (!mediaViewCycle) return;
    mediaViewCycle.dataset.view = mediaView;
    mediaViewCycle.dataset.businessCommand = mediaViewCommands[mediaView];
    mediaViewCycle.title = `${mediaView[0].toUpperCase()}${mediaView.slice(1)} view`;
    if (window.setLexamoraIcon) window.setLexamoraIcon(mediaViewCycle, mediaViewCommands[mediaView]);
    else document.dispatchEvent(new CustomEvent("studio:icons-refresh", {detail: {root: mediaViewCycle}}));
  };
  if (mediaViewCycle) {
    mediaViewCycle.onclick = () => {
      mediaView = mediaViews[(mediaViews.indexOf(mediaView) + 1) % mediaViews.length];
      localStorage.setItem("studio-movie-media-view", mediaView);
      syncMediaViewCycle();
      renderBin();
    };
    syncMediaViewCycle();
  }
  const binSortControl = q("[data-bin-sort]");
  if (binSortControl) {
    const savedMediaSort = sessionStorage.getItem(mediaSortKey);
    if (["latest", "alpha", "duration", "custom"].includes(savedMediaSort)) binSortControl.value = savedMediaSort;
    binSortControl.addEventListener("change", () => {
      sessionStorage.setItem(mediaSortKey, binSortControl.value);
      renderBin();
    });
  }
  qa("[data-bin-kind],[data-bin-unused]").forEach(control => control.addEventListener("change", renderBin));
  q("[data-media-position]")?.setAttribute("hidden", "hidden");

  [...document.querySelectorAll("[data-panel-toggle]")].filter(button => button.closest("[data-panel-key]")).forEach(button => {
    const panel = button.closest(".movie-collapsible");
    const key = `studio-movie-panel-${panel?.dataset.panelKey || "panel"}`;
    const apply = collapsed => {
      if (!panel) return;
      if (collapsed && panel.classList.contains("movie-panel-floating")) {
        const rect = panel.getBoundingClientRect();
        panel.dataset.expandedHeight = String(rect.height);
      }
      panel.classList.toggle("collapsed", collapsed);
      button.dataset.businessCommand = collapsed ? "add" : "remove";
      button.title = `${collapsed ? "Expand" : "Collapse"} ${panel.dataset.panelKey}`;
      button.setAttribute("aria-label", button.title);
      if (panel.classList.contains("movie-panel-floating")) {
        if (collapsed) {
          panel.style.height = "auto";
        } else if (panel.dataset.expandedHeight) {
          panel.style.height = `${Math.max(86, Number(panel.dataset.expandedHeight))}px`;
          const rect = panel.getBoundingClientRect();
          setPanelFloatingState(panel.dataset.panelKey, panel, rect);
          persistFloatingPanels();
        }
      }
      window.refreshLexamoraIcons?.(button);
      refreshFloatingGeometry();
    };
    apply(sessionStorage.getItem(key) === "collapsed");
    button.onclick = () => { const collapsed = !panel.classList.contains("collapsed"); apply(collapsed); sessionStorage.setItem(key, collapsed ? "collapsed" : "expanded"); };
  });
  const trackWidth = q("[data-track-width]");
  const timelineShell = q("[data-timeline-shell]");
  if (trackWidth && timelineShell) {
    trackWidth.value = localStorage.getItem("studio-movie-track-width") || "190";
    const applyTrackWidth = () => {
      timelineShell.style.setProperty("--track-sidebar-width", `${trackWidth.value}px`);
      timelineShell.classList.toggle("compact-tracks", Number(trackWidth.value) < 92);
    };
    trackWidth.oninput = () => { applyTrackWidth(); localStorage.setItem("studio-movie-track-width", trackWidth.value); };
    q("[data-track-width-restore]")?.addEventListener("click", () => {
      trackWidth.value = "190";
      applyTrackWidth();
      localStorage.setItem("studio-movie-track-width", trackWidth.value);
    });
    const trackSidebarResizer = q("[data-track-sidebar-resizer]");
    if (trackSidebarResizer) trackSidebarResizer.onpointerdown = event => {
      if (event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      const startX = event.clientX;
      const startWidth = Number(trackWidth.value);
      const pointerId = event.pointerId;
      trackSidebarResizer.classList.add("dragging");
      capturePointerSafely(trackSidebarResizer, pointerId);
      trackSidebarResizer.onpointermove = move => {
        trackWidth.value = String(clamp(startWidth + move.clientX - startX, 44, 320));
        applyTrackWidth();
      };
      const finish = () => {
        trackSidebarResizer.onpointermove = null;
        trackSidebarResizer.onpointerup = null;
        trackSidebarResizer.onpointercancel = null;
        trackSidebarResizer.onlostpointercapture = null;
        releasePointerCaptureSafely(trackSidebarResizer, pointerId);
        trackSidebarResizer.classList.remove("dragging");
        localStorage.setItem("studio-movie-track-width", trackWidth.value);
      };
      trackSidebarResizer.onpointerup = finish;
      trackSidebarResizer.onpointercancel = finish;
      trackSidebarResizer.onlostpointercapture = finish;
    };
    applyTrackWidth();
  }
  const updateLayoutGeometry = geometry => {
    editorLayouts[editorLayoutMode] = clampLayoutGeometry(editorLayoutMode, {
      ...editorLayouts[editorLayoutMode],
      ...geometry,
    });
    applyEditorLayout(cloneLayoutState(), {persist: false, refresh: false});
    applyPreviewZoom();
    updatePreviewGeometry();
  };
  const snapLayoutValue = (value, targets, handle, threshold = 10) => {
    const candidate = targets
      .map(target => Number(target))
      .filter(Number.isFinite)
      .find(target => Math.abs(target - value) <= threshold);
    handle?.classList.toggle("snapped", candidate != null);
    return candidate == null ? value : candidate;
  };
  const finishLayoutResize = handle => {
    handle?.classList.remove("dragging", "snapped");
    persistLayout(editorLayoutMode);
    applyPreviewZoom();
    updatePreviewGeometry();
    renderTimeline();
    updateHistoryButtons();
  };
  const bindLayoutDivider = (handle, side) => {
    if (!handle || !editorLayout) return;
    handle.onpointerdown = event => {
      if (layoutLocked || event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      remember();
      const startX = event.clientX;
      const start = {...editorLayouts[editorLayoutMode]};
      const pointerId = event.pointerId;
      handle.classList.add("dragging");
      capturePointerSafely(handle, pointerId);
      handle.onpointermove = move => {
        const deltaX = move.clientX - startX;
        if (side === "media") {
          const width = snapLayoutValue(start.mediaWidth + deltaX, [layoutDefaults.columns.mediaWidth], handle);
          updateLayoutGeometry({mediaWidth: width});
        } else if (editorLayoutMode === "columns") {
          const width = snapLayoutValue(start.inspectorWidth - deltaX, [layoutDefaults.columns.inspectorWidth], handle);
          updateLayoutGeometry({inspectorWidth: width});
        }
      };
      let finished = false;
      const finish = () => {
        if (finished) return;
        finished = true;
        clearEditorPointerFinish(finish);
        handle.onpointermove = null;
        handle.onpointerup = null;
        handle.onpointercancel = null;
        handle.onlostpointercapture = null;
        releasePointerCaptureSafely(handle, pointerId);
        finishLayoutResize(handle);
      };
      handle.onpointerup = finish;
      handle.onpointercancel = finish;
      handle.onlostpointercapture = finish;
      registerEditorPointerFinish(finish);
    };
  };
  bindLayoutDivider(q("[data-stage-resizer]"), "media");
  bindLayoutDivider(q("[data-inspector-resizer]"), "inspector");
  qa("[data-panel-width-resizer]").forEach(handle => {
    handle.onpointerdown = event => {
      if (layoutLocked || event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      remember();
      const startX = event.clientX;
      const start = {...editorLayouts[editorLayoutMode]};
      const panel = handle.dataset.panelWidthResizer;
      const pointerId = event.pointerId;
      handle.classList.add("dragging");
      capturePointerSafely(handle, pointerId);
      handle.onpointermove = move => {
        const deltaX = move.clientX - startX;
        if (panel === "media") {
          const width = snapLayoutValue(start.mediaWidth - deltaX, [layoutDefaults.columns.mediaWidth], handle);
          updateLayoutGeometry({mediaWidth: width});
        }
        if (panel === "inspector" && editorLayoutMode === "columns") {
          const width = snapLayoutValue(start.inspectorWidth + deltaX, [layoutDefaults.columns.inspectorWidth], handle);
          updateLayoutGeometry({inspectorWidth: width});
        }
      };
      let finished = false;
      const finish = () => {
        if (finished) return;
        finished = true;
        clearEditorPointerFinish(finish);
        handle.onpointermove = null;
        handle.onpointerup = null;
        handle.onpointercancel = null;
        handle.onlostpointercapture = null;
        releasePointerCaptureSafely(handle, pointerId);
        finishLayoutResize(handle);
      };
      handle.onpointerup = finish;
      handle.onpointercancel = finish;
      handle.onlostpointercapture = finish;
      registerEditorPointerFinish(finish);
    };
  });
  qa("[data-timeline-edge-resizer]").forEach(handle => {
    handle.onpointerdown = event => {
      if (layoutLocked || event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      remember();
      const startX = event.clientX;
      const start = {...editorLayouts[editorLayoutMode]};
      const edge = handle.dataset.timelineEdgeResizer;
      const pointerId = event.pointerId;
      handle.classList.add("dragging");
      capturePointerSafely(handle, pointerId);
      handle.onpointermove = move => {
        const deltaX = move.clientX - startX;
        const value = snapLayoutValue(
          edge === "left" ? start.timelineInsetLeft + deltaX : start.timelineInsetRight - deltaX,
          [0],
          handle,
        );
        updateLayoutGeometry(edge === "left" ? {timelineInsetLeft: value} : {timelineInsetRight: value});
      };
      let finished = false;
      const finish = () => {
        if (finished) return;
        finished = true;
        clearEditorPointerFinish(finish);
        handle.onpointermove = null;
        handle.onpointerup = null;
        handle.onpointercancel = null;
        handle.onlostpointercapture = null;
        releasePointerCaptureSafely(handle, pointerId);
        finishLayoutResize(handle);
      };
      handle.onpointerup = finish;
      handle.onpointercancel = finish;
      handle.onlostpointercapture = finish;
      registerEditorPointerFinish(finish);
    };
  });
  const timelineHeightResizer = q("[data-timeline-height-resizer]");
  if (timelineHeightResizer) {
    timelineHeightResizer.onpointerdown = event => {
      if (layoutLocked || event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      remember();
      const startY = event.clientY;
      const start = {...editorLayouts[editorLayoutMode]};
      const pointerId = event.pointerId;
      timelineHeightResizer.classList.add("dragging");
      capturePointerSafely(timelineHeightResizer, pointerId);
      timelineHeightResizer.onpointermove = move => {
        const deltaY = move.clientY - startY;
        const targets = [
          layoutDefaults.columns.mediaHeight,
          layoutDefaults.columns.canvasHeight,
          layoutDefaults.columns.inspectorHeight,
        ];
        const upper = {
          mediaHeight: snapLayoutValue(start.mediaHeight + deltaY, targets, timelineHeightResizer),
          canvasHeight: snapLayoutValue(start.canvasHeight + deltaY, targets, timelineHeightResizer),
        };
        if (editorLayoutMode === "columns") upper.inspectorHeight = start.inspectorHeight + deltaY;
        updateLayoutGeometry(upper);
      };
      let finished = false;
      const finish = () => {
        if (finished) return;
        finished = true;
        clearEditorPointerFinish(finish);
        timelineHeightResizer.onpointermove = null;
        timelineHeightResizer.onpointerup = null;
        timelineHeightResizer.onpointercancel = null;
        timelineHeightResizer.onlostpointercapture = null;
        releasePointerCaptureSafely(timelineHeightResizer, pointerId);
        finishLayoutResize(timelineHeightResizer);
      };
      timelineHeightResizer.onpointerup = finish;
      timelineHeightResizer.onpointercancel = finish;
      timelineHeightResizer.onlostpointercapture = finish;
      registerEditorPointerFinish(finish);
    };
  }
  const timelineBottomResizer = q("[data-timeline-bottom-resizer]");
  if (timelineBottomResizer) {
    timelineBottomResizer.onpointerdown = event => {
      if (event.button !== 0) return;
      if (layoutLocked) {
        event.preventDefault();
        flashLayoutLock();
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      remember();
       const startY = event.clientY;
       const timelineShell = q("[data-timeline-shell]");
       const startHeight = Number(editorLayouts[editorLayoutMode].timelineHeight || pageRect(timelineShell).height);
      const pointerId = event.pointerId;
      let liveHeight = startHeight;
      timelineBottomResizer.classList.add("dragging");
      document.body.classList.add("movie-panel-interacting");
      capturePointerSafely(timelineBottomResizer, pointerId);
      const moveTimelineBottom = move => {
        if (move.pointerId !== pointerId) return;
         const height = snapLayoutValue(
           startHeight + move.clientY - startY,
           [layoutDefaults.columns.timelineHeight],
           timelineBottomResizer,
         );
         liveHeight = height;
         editorLayouts[editorLayoutMode] = {
           ...editorLayouts[editorLayoutMode],
           timelineHeight: clampLayoutGeometry(editorLayoutMode, {
             ...editorLayouts[editorLayoutMode],
             timelineHeight: height,
           }).timelineHeight,
         };
         timelineShell?.style.setProperty("height", `${editorLayouts[editorLayoutMode].timelineHeight}px`, "important");
      };
      window.addEventListener("pointermove", moveTimelineBottom, true);
      let finished = false;
      const finish = finishEvent => {
        if (finishEvent?.pointerId != null && finishEvent.pointerId !== pointerId) return;
        if (finished) return;
        finished = true;
        clearEditorPointerFinish(finish);
        window.removeEventListener("pointermove", moveTimelineBottom, true);
        window.removeEventListener("pointerup", finish, true);
        window.removeEventListener("pointercancel", finish, true);
        timelineBottomResizer.onlostpointercapture = null;
        releasePointerCaptureSafely(timelineBottomResizer, pointerId);
        timelineBottomResizer.classList.remove("dragging");
        document.body.classList.remove("movie-panel-interacting");
         const geometry = clampLayoutGeometry(editorLayoutMode, {
           ...editorLayouts[editorLayoutMode],
           timelineHeight: liveHeight,
         });
         editorLayouts[editorLayoutMode] = geometry;
         timelineShell?.style.setProperty("height", `${geometry.timelineHeight}px`, "important");
         persistLayout(editorLayoutMode);
        applyPreviewZoom();
        updatePreviewGeometry();
        renderTimeline();
        historyRedo = [];
        updateHistoryButtons();
      };
      window.addEventListener("pointerup", finish, true);
      window.addEventListener("pointercancel", finish, true);
      timelineBottomResizer.onlostpointercapture = finish;
      registerEditorPointerFinish(finish);
    };
  }
  qa("[data-panel-height-resizer]").forEach(handle => {
    handle.onpointerdown = event => {
      if (layoutLocked || event.button !== 0) return;
      event.preventDefault();
      event.stopPropagation();
      remember();
      const key = handle.dataset.panelHeightResizer;
      const startY = event.clientY;
      const start = {...editorLayouts[editorLayoutMode]};
      const property = key === "media" ? "mediaHeight" : key === "canvas" ? "canvasHeight" : "inspectorHeight";
      const pointerId = event.pointerId;
      handle.classList.add("dragging");
      capturePointerSafely(handle, pointerId);
       // Each upper panel owns its lower edge and grows down independently.
       handle.onpointermove = move => {
         const targets = [
           layoutDefaults.columns.mediaHeight,
           layoutDefaults.columns.canvasHeight,
           layoutDefaults.columns.inspectorHeight,
           ...["mediaHeight", "canvasHeight", "inspectorHeight"].map(name => start[name]),
         ];
         const height = snapLayoutValue(start[property] + (move.clientY - startY), targets, handle);
         updateLayoutGeometry({[property]: height});
       };
      let finished = false;
      const finish = () => {
        if (finished) return;
        finished = true;
        clearEditorPointerFinish(finish);
        handle.onpointermove = null;
        handle.onpointerup = null;
        handle.onpointercancel = null;
        handle.onlostpointercapture = null;
        releasePointerCaptureSafely(handle, pointerId);
        finishLayoutResize(handle);
      };
      handle.onpointerup = finish;
      handle.onpointercancel = finish;
      handle.onlostpointercapture = finish;
      registerEditorPointerFinish(finish);
    };
  });
  qa("[data-stage-resizer],[data-inspector-resizer],[data-timeline-height-resizer],[data-timeline-bottom-resizer]").forEach(handle => {
    handle.addEventListener("dblclick", event => {
      if (layoutLocked) {
        flashLayoutLock();
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      widePanelGaps = !widePanelGaps;
      localStorage.setItem(panelGapKey, widePanelGaps ? "wide" : "narrow");
      applyPanelGapMode();
      toast(widePanelGaps ? "Wide panel gaps" : "Compact panel gaps");
    });
  });

  qa("[data-layout-mode]").forEach(button => {
    button.addEventListener("click", () => {
      const mode = button.dataset.layoutMode;
      if (!canEdit || mode === editorLayoutMode || !editorLayouts[mode]) return;
      remember();
      persistLayout(editorLayoutMode);
      editorLayoutMode = mode;
      applyEditorLayout(cloneLayoutState());
      updateHistoryButtons();
    });
  });
  q("[data-layout-lock]")?.addEventListener("click", () => {
    layoutLocked = !layoutLocked;
    localStorage.setItem(layoutLockKey, String(layoutLocked));
    applyLayoutLockState();
    toast(layoutLocked ? "Layout locked" : "Layout unlocked");
  });
  q("[data-layout-reset]")?.addEventListener("click", () => {
    if (!canEdit) return;
    remember();
    const resetLayout = structuredClone(tileLayoutDefaults || createDefaultTileLayout());
    editorLayouts.columns = {...layoutDefaults.columns, ...resetLayout};
    editorLayouts.stacked = {...layoutDefaults.stacked, ...resetLayout};
    editorLayoutMode = "columns";
    previewZoomManual = false;
    localStorage.removeItem(previewZoomStorageKey);
    applyEditorLayout(cloneLayoutState());
    scheduleTileWorkspaceCenter({force: true});
    updateHistoryButtons();
    toast("Layout reset");
  });
  const loadLayoutButton = q("[data-layout-load]");
  const refreshSavedLayoutButton = () => {
    if (!loadLayoutButton) return;
    const available = Boolean(localStorage.getItem(savedLayoutKey));
    loadLayoutButton.disabled = !available;
    loadLayoutButton.title = available ? "Load saved layout" : "No saved layout";
  };
  q("[data-layout-save]")?.addEventListener("click", () => {
    localStorage.setItem(savedLayoutKey, JSON.stringify(cloneLayoutState()));
    refreshSavedLayoutButton();
    toast("Layout saved");
  });
  loadLayoutButton?.addEventListener("click", () => {
    try {
      const saved = JSON.parse(localStorage.getItem(savedLayoutKey) || "null");
      if (!saved?.layouts) return;
      remember();
      applyEditorLayout(saved);
      scheduleTileWorkspaceCenter({force: true});
      updateHistoryButtons();
      toast("Saved layout loaded");
    } catch (_) {
      toast("Saved layout is unavailable");
    }
  });
  refreshSavedLayoutButton();
  q("[data-preview-dock]")?.addEventListener("click", () => {
    previewDockMode = previewDockMode === "bottom" ? "center" : "bottom";
    localStorage.setItem(previewDockKey, previewDockMode);
    applyPreviewDock();
    applyPreviewZoom();
    updatePreviewGeometry();
  });

  q("[data-movie-ratio]").value = root.dataset.aspectRatio || "16:9";
  q("[data-movie-resolution]").value = root.dataset.resolution || "1920x1080";
  q("[data-movie-fps]").value = root.dataset.fps || "25";
  applyPanelGapMode();
  bindPanelDragging();
  applyLayoutLockState();
  applyEditorLayout(cloneLayoutState(), {persist: false, refresh: false});
  scheduleEditorViewportFit();
  scheduleTileWorkspaceCenter();
  editorViewport?.classList.add("movie-scrollbars-idle");
  tileCameraScrollbar?.classList.add("movie-scrollbars-idle");
  applyPreviewDock();
  normalizeTimeline(); applyCanvas(); renderBin(); renderTimeline(); renderInspector(); renderRenderJobs(); renderLibrary(); scheduleRenderPoll(); setPlayhead(0, true, true);
  savedSignature = signature(); updateDirty(); updateHistoryButtons(); refreshMedia();
  requestAnimationFrame(() => requestAnimationFrame(() => {
    window.scrollTo({left: 0, top: 0, behavior: "auto"});
    if (editorViewport) editorViewport.scrollTop = 0;
    fitEditorViewportToWindow();
    applyPreviewZoom();
    updatePreviewGeometry();
    renderTimeline();
  }));
  if ("ResizeObserver" in window && editorViewport) {
    let observedViewportWidth = editorViewport.clientWidth;
    new ResizeObserver(() => {
      const nextWidth = editorViewport.clientWidth;
      if (Math.abs(nextWidth - observedViewportWidth) < 1) return;
      if (!Number.isFinite(pendingTileCameraLogicalCenterX) && tileViewportGeometry) {
        const origin = tileWorkspaceViewportOrigin();
        pendingTileCameraLogicalCenterX = (
          tileCameraX
          + observedViewportWidth / 2
          - origin.x
          - tileViewportGeometry.offsetX
        );
      }
      observedViewportWidth = nextWidth;
      scheduleEditorViewportFit();
    }).observe(editorViewport);
  }
  if ("ResizeObserver" in window && previewBody) {
    new ResizeObserver(() => {
      applyPreviewZoom();
      updatePreviewGeometry();
    }).observe(previewBody);
  }
})();

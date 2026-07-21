(() => {
  const root = document.querySelector("[data-movie-editor]");
  if (!root) return;

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
  let clipClipboard = [];
  let selectedMediaIds = new Set();
  let selectedMediaFolderId = null;
  let mediaClipboardIds = [];
  let mediaSelectionActive = false;
  let pasteTargetTrackId = null;
  let playheadMs = 0;
  let dirty = false;
  let snapping = true;
  let zoom = Math.max(6, Math.min(160, Number(localStorage.getItem("studio-movie-zoom")) || 24));
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
  const defaultTrackHeight = Math.max(56, Math.min(200, Number(localStorage.getItem("studio-movie-track-height")) || 84));
  let mediaView = ["list", "small", "large"].includes(localStorage.getItem("studio-movie-media-view")) ? localStorage.getItem("studio-movie-media-view") : "list";
  let previewZoom = Math.max(.25, Math.min(2, Number(localStorage.getItem("studio-movie-preview-zoom")) || 1));
  let inspectorTab = "VIDEO";
  let previewGeometry = null;
  let autosaveTimer = null;
  let savePromise = null;
  let saveAgain = false;
  const settingSnapshots = new WeakMap();
  const PRECISION_MS = 10;

  const q = selector => root.querySelector(selector);
  const qa = selector => [...root.querySelectorAll(selector)];
  const bin = q("[data-movie-bin]");
  const tracksNode = q("[data-movie-tracks]");
  const headsNode = q("[data-track-heads]");
  const preview = q("[data-movie-preview]");
  const previewStage = q("[data-preview-stage]");
  const previewLayers = q("[data-preview-layers]");
  const previewOutline = q("[data-preview-outline]");
  const previewEmpty = q("[data-preview-empty]");
  const previewStatus = q("[data-preview-status]");
  const previewScrub = q("[data-preview-scrub]");
  const inspector = q("[data-clip-inspector]");
  const inspectorActions = q("[data-inspector-actions]");
  const previewZoomInput = q("[data-preview-zoom]");
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
  const assetMap = new Map();
  const audioPlayers = new Map();
  const visualPlayers = new Map();

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
  });
  const signature = state => {
    const value = state || currentState();
    return JSON.stringify({ ...value, selectedId: null });
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
      track.height = clamp(Number(track.height || defaultTrackHeight), 56, 200);
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
    const ratio = output.width / output.height;
    const availableWidth = Math.max(180, body.clientWidth - 36);
    const availableHeight = Math.max(240, Math.min(640, window.innerHeight * .68));
    const naturalWidth = Math.min(availableWidth, availableHeight * ratio);
    const naturalHeight = naturalWidth / ratio;
    previewStage.style.width = `${Math.max(90, naturalWidth * previewZoom)}px`;
    previewStage.style.height = `${Math.max(90, naturalHeight * previewZoom)}px`;
    previewStage.style.aspectRatio = `${output.width} / ${output.height}`;
    if (previewZoomInput) previewZoomInput.value = previewZoom;
    const zoomValue = q("[data-preview-zoom-value]");
    if (zoomValue) zoomValue.textContent = `${Math.round(previewZoom * 100)}%`;
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
    const sourceWidth = Number(asset?.width || preview.videoWidth || 0);
    const sourceHeight = Number(asset?.height || preview.videoHeight || 0);
    const exactCanvas = sourceWidth === output.width && sourceHeight === output.height;
    const guard = exactCanvas ? 1 : 0;
    preview.style.setProperty("width", `calc(100% + ${guard * 2}px)`, "important");
    preview.style.setProperty("height", `calc(100% + ${guard * 2}px)`, "important");
    preview.style.setProperty("left", `${-guard}px`, "important");
    preview.style.setProperty("top", `${-guard}px`, "important");
    preview.style.setProperty("transform", "none", "important");
    preview.style.setProperty("object-fit", exactCanvas ? "fill" : "contain", "important");
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
    renderMediaPreviewSelection();
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
    let changed = false;
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
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", finish);
      window.removeEventListener("pointercancel", finish);
      previewStage.classList.remove("dragging");
      clearCanvasSnapEdges();
      if (!changed) historyUndo.pop();
      else { historyRedo = []; updateDirty(); updateHistoryButtons(); scheduleAutosave(); }
      renderInspector();
    };
    previewStage.classList.add("dragging");
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", finish);
    window.addEventListener("pointercancel", finish);
  }

  function beginPreviewResize(event) {
    const handle = event.currentTarget.dataset.previewResize || "se";
    const {clip, track} = previewClipData();
    if (!clip || !canEdit || track?.locked || standalonePreviewAssetId) return;
    event.preventDefault(); event.stopPropagation();
    if (!selectedIds.has(clip.id)) selectClip(clip.id);
    remember();
    const startX = event.clientX;
    const startY = event.clientY;
    const original = Number(clip.scale || 1);
    let changed = false;
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
      window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", finish); window.removeEventListener("pointercancel", finish);
      if (!changed) historyUndo.pop(); else { historyRedo = []; updateDirty(); updateHistoryButtons(); scheduleAutosave(); }
      renderInspector();
    };
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
      mediaFoldersNode.append(makeButton("\u2190 Back", () => { currentMediaFolderId = null; selectedMediaIds.clear(); renderBin(); }, "Back to Media"));
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
    const text = String(value || "Folder");
    return text.length > 7 ? `${text.slice(0, 7)}...` : text;
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
    message.textContent = mode === "delete" ? `Delete “${folder?.name || "folder"}”? Media files remain in this edit project.` : "";
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
    });
    selectedMediaIds = new Set(ids);
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
    if (!selected.length) return;
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
    playheadLabel.textContent = clock(playheadMs);
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
    if (!asset || asset.kind !== "VIDEO" || asset.status !== "READY") return;
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
    preview.hidden = false;
    if (previewOutline) previewOutline.hidden = false;
    previewEmpty.hidden = true;
    preview.dataset.assetId = asset.id;
    preview.dataset.clipId = "standalone";
    preview.src = asset.proxyUrl || asset.originalUrl;
    preview.load();
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
        q("[data-preview-play]").innerHTML = "&#10074;&#10074;";
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
    q("[data-preview-play]").innerHTML = "&#10074;&#10074;";
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
    const actionSet = disabled => {
      const actions = [
        inspectorButton("S", "Split at playhead", splitSelected),
        inspectorButton("C", "Copy selected clips", copySelected),
        inspectorButton("M", "Render selected clip as reusable media", queueClipAsset),
        inspectorButton("x", "Delete selection", deleteSelected),
      ];
      actions.forEach(button => { button.disabled = disabled; });
      actions[2].disabled = disabled || selectedIds.size !== 1 || !canExport;
      inspectorActions.append(...actions);
    };
    if (selectedIds.size > 1) {
      const summary = document.createElement("strong");
      summary.className = "movie-selection-summary";
      summary.textContent = `${selectedIds.size} clips selected`;
      actionSet(false);
      inspector.append(summary);
      return;
    }
    const found = findClip(selectedId);
    actionSet(!found);
    const clip = found?.clip;
    const track = found?.track;
    const asset = clip ? assetMap.get(clip.assetId) : null;
    if (found) {
      const sourceSummary = document.createElement("span");
      sourceSummary.className = "movie-source-summary";
      sourceSummary.textContent = `Source: ${asset?.name || clip.sourceAssetId || "Unknown"}`;
      sourceSummary.title = `${asset?.name || "Source media"} / ${clip.sourceAssetId || clip.assetId}`;
      inspectorActions.prepend(sourceSummary);
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
    const height = document.createElement("input");
    const heightValue = document.createElement("output");
    const remove = document.createElement("button");
    head.className = `movie-track-head${track.locked ? " locked" : ""}${track.hidden ? " hidden-track" : ""}`;
    nameWrap.className = "movie-track-name";
    code.className = "movie-track-code";
    const sameKind = timeline.tracks.filter(item => item.kind === track.kind);
    code.textContent = `${track.kind === "AUDIO" ? "A" : "V"}${sameKind.indexOf(track) + 1}`;
    controls.className = "movie-track-controls";
    head.style.height = `${track.height}px`;
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
    display.textContent = track.displayMode === "WAVEFORM" ? "W" : "C";
    display.title = track.displayMode === "WAVEFORM" ? "Show clips and frames" : "Show waveform and loudness guide";
    height.type = "range"; height.min = "56"; height.max = "200"; height.step = "4"; height.value = String(track.height);
    height.className = "movie-track-size"; height.title = "Track height"; height.setAttribute("aria-label", "Track height");
    heightValue.className = "movie-track-height-value"; heightValue.textContent = `${Math.round(track.height)} px`; heightValue.title = "Individual track height";
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
    let heightRemembered = false;
    height.onpointerdown = () => { if (!heightRemembered) { remember(); heightRemembered = true; } };
    height.oninput = () => {
      const next = clamp(Number(height.value), 56, 200);
      track.height = next;
      heightValue.textContent = `${Math.round(next)} px`;
      head.style.height = `${next}px`;
      const lane = q(`.movie-track-lane[data-track-id="${CSS.escape(track.id)}"]`);
      if (lane) {
        lane.style.height = `${next}px`;
        lane.querySelectorAll(".movie-clip").forEach(node => node.style.height = `${Math.max(48, next - 8)}px`);
      }
    };
    height.onchange = () => {
      track.height = clamp(Number(height.value), 56, 200);
      historyRedo = []; updateDirty(); updateHistoryButtons(); heightRemembered = false; scheduleAutosave();
      localStorage.setItem("studio-movie-track-height", String(track.height));
      renderTimeline();
    };
    remove.onclick = () => {
      if (track.clips.length) return toast("Remove clips before deleting this track");
      mutate(() => { timeline.tracks = timeline.tracks.filter(item => item.id !== track.id); });
      renderTimeline();
    };
    controls.append(up, down, mute, visibility, lock, display, loudnessGuide, height, heightValue, remove);
    head.append(nameWrap, controls);
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
    node.setPointerCapture(event.pointerId);
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
    node.style.height = `${Math.max(48, track.height - 8)}px`;
    strip.className = "movie-clip-strip";
    if (mediaKind === "VIDEO" && (asset?.filmstripUrl || asset?.thumbnailUrl)) {
      const interval = asset.filmstripIntervalMs || 2000;
      const total = asset.filmstripFrameCount || 1;
      const frameSize = Math.max(32, track.height - (asset?.waveformUrl ? 30 : 8));
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
      lane.style.height = `${track.height}px`;
      lane.onpointerenter = () => { pasteTargetTrackId = track.id; };
      lane.onpointermove = () => { pasteTargetTrackId = track.id; };
      lane.onpointerdown = event => {
        if (event.target !== lane) return;
        beginLaneGesture(event, lane);
      };
      lane.ondragover = event => { if (!track.locked && canEdit) event.preventDefault(); };
      lane.ondrop = event => {
        event.preventDefault();
        addClip(track, event.dataTransfer.getData("text/asset-id"), (event.clientX - lane.getBoundingClientRect().left) / zoom * 1000);
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
    zoom = clamp(Math.round(next / 2) * 2, 6, 160);
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
      location.assign(item.openUrl);
    } catch (error) { editSelect.value = timelineId; toast(error.message, "error"); }
  });
  q("[data-edit-create]")?.addEventListener("click", async () => {
    try { const item = await montageAction("create", {title: "Untitled edit"}); location.assign(item.openUrl); }
    catch (error) { toast(error.message, "error"); }
  });
  q("[data-edit-copy]")?.addEventListener("click", async () => {
    try { const item = await montageAction("copy", {title: `${q("[data-movie-title]").value} copy`}); location.assign(item.openUrl); }
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
      if (!Array.isArray(importedTimeline.tracks)) throw new Error("The edit project file has no tracks");
      pendingImport = {file, parsed, timeline: importedTimeline};
      q("[data-import-file-name]").textContent = file.name;
      const trackSelect = q("[data-import-track]"); trackSelect.replaceChildren();
      importedTimeline.tracks.forEach((track, index) => { const option = document.createElement("option"); option.value = index; option.textContent = `${track.name || track.kind || "Track"} (${track.clips?.length || 0} clips)`; trackSelect.append(option); });
      q("[data-import-mode]").value = "project"; q("[data-import-track-field]").hidden = true;
      q("[data-import-mode]").disabled = false;
      importDialog.showModal();
    } catch (error) { toast(error.message || "The edit project file is invalid", "error"); }
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
      location.assign(item.openUrl);
    } catch (error) { toast(error.message, "error"); }
  });
  q("[data-edit-detach]")?.addEventListener("click", async () => {
    try { await montageAction("detach"); location.assign(root.dataset.saveUrl); }
    catch (error) { toast(error.message, "error"); }
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
  let deleteArmedUntil = 0;
  q("[data-edit-delete]")?.addEventListener("click", async event => {
    if (Date.now() > deleteArmedUntil) {
      deleteArmedUntil = Date.now() + 5000;
      event.currentTarget.classList.add("warning");
      toast("Click Delete again within five seconds to confirm");
      return;
    }
    try { await montageAction("delete"); location.assign(root.dataset.saveUrl); }
    catch (error) { toast(error.message, "error"); }
  });

  q("[data-media-library-open]")?.addEventListener("click", () => { libraryScope = "project"; qa("[data-media-scope]").forEach(button => button.classList.toggle("active", button.dataset.mediaScope === libraryScope)); renderLibrary(); libraryDialog.showModal(); });
  q("[data-media-library-close]")?.addEventListener("click", () => libraryDialog.close());
  libraryDialog?.addEventListener("click", event => { if (event.target === libraryDialog) libraryDialog.close(); });
  qa("[data-media-scope]").forEach(button => button.onclick = () => { libraryScope = button.dataset.mediaScope; qa("[data-media-scope]").forEach(item => item.classList.toggle("active", item === button)); renderLibrary(); });
  q("[data-media-kind]")?.addEventListener("change", renderLibrary);
  q("[data-media-sort]")?.addEventListener("change", renderLibrary);
  const uploadMediaFiles = filesToUpload => {
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
    xhr.onload = async () => { pendingUploads = []; progress.hidden = true; input.value = ""; let data = {}; try { data = JSON.parse(xhr.responseText); } catch (_) {} if (xhr.status >= 400) toast(data.error || "Upload failed", "error"); else { mutate(() => (data.items || []).forEach(item => { if (!timeline.mediaAssetIds.includes(item.id)) timeline.mediaAssetIds.push(item.id); if (!timeline.mediaOrder.includes(item.id)) timeline.mediaOrder.push(item.id); })); if (data.errors?.length) toast(data.errors.join(" / "), "error"); } await refreshMedia(); };
    xhr.onerror = () => { pendingUploads = []; renderBin(); progress.hidden = true; toast("Upload failed", "error"); };
    xhr.send(form);
  };
  q("[data-media-upload-form]")?.addEventListener("submit", event => {
    event.preventDefault();
    uploadMediaFiles(q("[data-media-files]").files);
  });
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
  q("[data-media-context-paste]")?.addEventListener("click", () => {
    const folderId = mediaContextFolderId;
    hideMediaContext();
    if (folderId && mediaClipboardIds.length) { moveAssetsToFolder(mediaClipboardIds, folderId); return; }
    pasteSelectedMedia();
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
    mediaContext.style.left = `${Math.min(event.clientX, innerWidth - 200)}px`;
    mediaContext.style.top = `${Math.min(event.clientY, innerHeight - 230)}px`;
    mediaContext.hidden = false;
  });
  bin?.addEventListener("pointerdown", event => {
    if (event.button !== 0 || event.target.closest(".movie-bin-item,.movie-bin-folder-tile,button,input,select,label")) return;
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
  qa("[data-canvas-frame]").forEach(button => button.addEventListener("click", event => {
    event.preventDefault(); event.stopPropagation();
    const found = selectedId ? findClip(selectedId) : null;
    if (!found || !canEdit || found.track.locked || standalonePreviewAssetId) return;
    const action = button.dataset.canvasFrame;
    mutate(() => {
      if (action === "reset") { found.clip.positionX = 0; found.clip.positionY = 0; found.clip.scale = 1; }
      else if (action === "left") found.clip.positionX = Number(found.clip.positionX || 0) - 1;
      else if (action === "right") found.clip.positionX = Number(found.clip.positionX || 0) + 1;
      else if (action === "up") found.clip.positionY = Number(found.clip.positionY || 0) + 1;
      else if (action === "down") found.clip.positionY = Number(found.clip.positionY || 0) - 1;
    });
    updatePreviewGeometry(); renderInspector();
  }));
  previewStage.addEventListener("wheel", () => {}, {passive: true});
  playheadNode.addEventListener("pointerdown", beginPlayheadGesture);
  previewZoomInput?.addEventListener("input", event => {
    previewZoom = clamp(Number(event.target.value), .25, 2);
    localStorage.setItem("studio-movie-preview-zoom", String(previewZoom));
    applyPreviewZoom();
    updatePreviewGeometry();
  });
  qa("[data-inspector-tab]").forEach(button => button.addEventListener("click", () => { inspectorTab = button.dataset.inspectorTab; renderInspector(); }));
  q("[data-timeline-split]").onclick = splitSelected;
  q("[data-clip-copy]")?.addEventListener("click", copySelected);
  q("[data-clip-paste]")?.addEventListener("click", pasteSelected);
  q("[data-clip-join]")?.addEventListener("click", joinSelected);
  q("[data-delete-selected]")?.addEventListener("click", deleteSelected);
  q("[data-selected-up]")?.addEventListener("click", () => moveSelectedToAdjacentTrack(-1));
  q("[data-selected-down]")?.addEventListener("click", () => moveSelectedToAdjacentTrack(1));
  qa("[data-clip-nudge]").forEach(button => bindHold(button, () => nudgeSelected(Number(button.dataset.clipNudge))));
  q("[data-timeline-snap]").onclick = event => { snapping = !snapping; event.currentTarget.classList.toggle("active", snapping); toast(snapping ? "Snapping enabled" : "Snapping disabled"); };
  ruler.onpointerdown = event => {
    leaveStandalonePreview();
    stopPlayback();
    const update = next => setPlayhead((next.clientX - ruler.getBoundingClientRect().left) / zoom * 1000);
    update(event); ruler.setPointerCapture(event.pointerId); ruler.onpointermove = update; ruler.onpointerup = () => { ruler.onpointermove = null; ruler.onpointerup = null; };
  };
  timelineScroll.addEventListener("scroll", () => { headsNode.style.transform = `translateY(-${timelineScroll.scrollTop}px)`; }, {passive: true});
  timelineScroll.addEventListener("wheel", event => {
    if (event.ctrlKey && Math.abs(event.deltaY) >= Math.abs(event.deltaX)) {
      event.preventDefault();
      changeZoom(zoom + (event.deltaY < 0 ? 4 : -4), event.clientX);
    }
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
      event.preventDefault();
      if (mediaSelectionActive) pasteSelectedMedia();
      else pasteSelected();
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
    qa("details[open]").forEach(details => {
      if (!event.target.closest("summary") || !details.contains(event.target)) details.removeAttribute("open");
    });
  });
  root.addEventListener("pointerdown", event => {
    if (playing && !event.target.closest("[data-preview-play],[data-preview-step],[data-preview-stop]")) stopPlayback();
  }, {capture: true});
  let resizeTimer;
  addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { applyPreviewZoom(); updatePreviewGeometry(); renderTimeline(); }, 120);
  });

  qa("[data-media-view]").forEach(button => {
    button.onclick = () => {
      mediaView = button.dataset.mediaView;
      localStorage.setItem("studio-movie-media-view", mediaView);
      renderBin();
    };
  });
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
  q("[data-media-position]")?.addEventListener("click", event => {
    const stage = event.currentTarget.closest(".movie-stage-column");
    stage.classList.toggle("media-bottom");
    event.currentTarget.title = stage.classList.contains("media-bottom") ? "Move media to the top" : "Move media to the bottom";
    sessionStorage.setItem("studio-movie-media-bottom", stage.classList.contains("media-bottom") ? "1" : "0");
  });

  qa("[data-panel-toggle]").forEach(button => {
    const panel = button.closest(".movie-collapsible");
    const key = `studio-movie-panel-${panel?.dataset.panelKey || "panel"}`;
    const apply = collapsed => {
      panel.classList.toggle("collapsed", collapsed);
      button.innerHTML = collapsed ? "&#43;" : "&#8722;";
      button.title = `${collapsed ? "Expand" : "Collapse"} ${panel.dataset.panelKey}`;
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
    applyTrackWidth();
  }
  const stageColumn = q(".movie-stage-column");
  const stageResizer = q("[data-stage-resizer]");
  if (stageColumn && stageResizer) {
    const maximumWidth = () => Math.max(180, stageColumn.clientWidth * .8);
    const storedWidth = clamp(Number(localStorage.getItem("studio-movie-media-width")) || 250, 150, maximumWidth());
    stageColumn.style.setProperty("--movie-media-width", `${storedWidth}px`);
    stageResizer.onpointerdown = event => {
      if (event.button !== 0) return;
      event.preventDefault();
      const startX = event.clientX;
      const startWidth = q("[data-panel-key=media]").getBoundingClientRect().width;
      const move = next => {
        const width = clamp(startWidth - (next.clientX - startX), 150, maximumWidth());
        stageColumn.style.setProperty("--movie-media-width", `${width}px`);
        localStorage.setItem("studio-movie-media-width", String(Math.round(width)));
        applyPreviewZoom();
        updatePreviewGeometry();
      };
      const finish = () => {
        window.removeEventListener("pointermove", move);
        window.removeEventListener("pointerup", finish);
        stageResizer.classList.remove("dragging");
        updatePreviewGeometry();
      };
      stageResizer.classList.add("dragging");
      window.addEventListener("pointermove", move);
      window.addEventListener("pointerup", finish);
    };
  }

  q("[data-movie-ratio]").value = root.dataset.aspectRatio || "16:9";
  q("[data-movie-resolution]").value = root.dataset.resolution || "1920x1080";
  q("[data-movie-fps]").value = root.dataset.fps || "25";
  q(".movie-stage-column")?.classList.toggle("media-bottom", sessionStorage.getItem("studio-movie-media-bottom") === "1");
  normalizeTimeline(); applyCanvas(); renderBin(); renderTimeline(); renderInspector(); renderRenderJobs(); renderLibrary(); scheduleRenderPoll(); setPlayhead(0, true, true);
  savedSignature = signature(); updateDirty(); updateHistoryButtons(); refreshMedia();
})();

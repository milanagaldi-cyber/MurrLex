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
  let selectedId = null;
  let selectedIds = new Set();
  let clipClipboard = [];
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
    const undo = q("[data-editor-undo]");
    const redo = q("[data-editor-redo]");
    if (undo) undo.disabled = !canEdit || !historyUndo.length;
    if (redo) redo.disabled = !canEdit || !historyRedo.length;
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
    timeline.tracks.forEach((track, trackIndex) => {
      track.id ||= uid();
      track.kind = ["VIDEO", "AUDIO", "TEXT"].includes(track.kind) ? track.kind : "VIDEO";
      track.name ||= `${track.kind === "VIDEO" ? "Video" : "Audio"} ${trackIndex + 1}`;
      track.muted = Boolean(track.muted);
      track.locked = Boolean(track.locked);
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
    const ratio = q("[data-movie-ratio]").value || "16:9";
    const available = Math.max(180, body.clientWidth - 36);
    const natural = ratio === "9:16" ? Math.min(available, 300) : ratio === "1:1" ? Math.min(available, 460) : Math.min(available, 818);
    previewStage.style.width = `${Math.max(90, natural * previewZoom)}px`;
    if (previewZoomInput) previewZoomInput.value = previewZoom;
    const output = q("[data-preview-zoom-value]");
    if (output) output.textContent = `${Math.round(previewZoom * 100)}%`;
  }

  function applyCanvas() {
    const ratio = q("[data-movie-ratio]").value || "16:9";
    previewStage.style.aspectRatio = ratio.replace(":", "/");
    previewStage.dataset.ratio = ratio;
    const defaults = {"16:9": "1920x1080", "9:16": "1080x1920", "1:1": "1080x1080"};
    if (!qa(`[data-movie-resolution] option`).some(option => option.value === q("[data-movie-resolution]").value)) {
      q("[data-movie-resolution]").value = defaults[ratio];
    }
    applyPreviewZoom();
    updatePreviewGeometry();
  }

  function previewClipData() {
    if (standalonePreviewAssetId) return {asset: assetMap.get(standalonePreviewAssetId), clip: null, track: null};
    const selected = findClip(selectedId);
    const active = selected && ["VIDEO", "IMAGE"].includes(clipKind(selected.clip))
      ? selected : activeVisualClips(playheadMs).at(-1);
    return active ? {...active, asset: assetMap.get(active.clip.assetId)} : {asset: null, clip: null, track: null};
  }

  const outputDimensions = () => {
    const value = q("[data-movie-resolution]").value || "1920x1080";
    const [width, height] = value.split("x").map(Number);
    return {width: width || 1920, height: height || 1080};
  };

  function clipGeometry(asset, clip) {
    const output = outputDimensions();
    const canvasRatio = output.width / output.height;
    const sourceWidth = asset?.width || 0;
    const sourceHeight = asset?.height || 0;
    const sourceRatio = sourceWidth && sourceHeight ? sourceWidth / sourceHeight : canvasRatio;
    const scale = clamp(Number(clip?.scale ?? 1), .05, 8);
    const widthPx = (sourceRatio >= canvasRatio ? output.height * sourceRatio : output.width) * scale;
    const heightPx = (sourceRatio >= canvasRatio ? output.height : output.width / sourceRatio) * scale;
    const x = Number(clip?.positionX || 0);
    const y = Number(clip?.positionY || 0);
    return {
      output, sourceRatio, widthPx, heightPx,
      width: widthPx / output.width * 100,
      height: heightPx / output.height * 100,
      left: 50 + x / output.width * 100,
      top: 50 - y / output.height * 100,
    };
  }

  function applyGeometry(node, asset, clip) {
    const geometry = clipGeometry(asset, clip);
    node.style.width = `${geometry.width}%`;
    node.style.height = `${geometry.height}%`;
    node.style.left = `${geometry.left}%`;
    node.style.top = `${geometry.top}%`;
    node.style.transform = "translate(-50%,-50%)";
    node.style.opacity = String(clamp(Number(clip?.opacity ?? 1), 0, 1));
    node.style.filter = `brightness(${Math.max(0, 1 + Number(clip?.brightness || 0))}) contrast(${Number(clip?.contrast ?? 1)}) saturate(${Number(clip?.saturation ?? 1)}) blur(${Number(clip?.blur || 0) / 4}px)`;
    return geometry;
  }

  function updatePreviewGeometry() {
    const {asset, clip} = previewClipData();
    const geometry = asset ? applyGeometry(previewOutline, asset, clip) : null;
    if (standalonePreviewAssetId && asset) applyGeometry(preview, asset, null);
    visualPlayers.forEach((node, clipId) => {
      const found = findClip(clipId);
      const media = found ? assetMap.get(found.clip.assetId) : null;
      if (found && media) applyGeometry(node, media, found.clip);
    });
    if (previewOutline) previewOutline.hidden = !asset;
    previewGeometry = geometry ? {clip, ...geometry} : null;
  }

  function beginPreviewPan(event) {
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
      if (snapping && previewGeometry) {
        const edgeTolerance = 8;
        const left = output.width / 2 + nextX - geometry.widthPx / 2;
        const right = output.width / 2 + nextX + geometry.widthPx / 2;
        const top = output.height / 2 - nextY - geometry.heightPx / 2;
        const bottom = output.height / 2 - nextY + geometry.heightPx / 2;
        if (Math.abs(left) <= edgeTolerance) nextX = geometry.widthPx / 2 - output.width / 2;
        else if (Math.abs(right - output.width) <= edgeTolerance) nextX = output.width / 2 - geometry.widthPx / 2;
        if (Math.abs(top) <= edgeTolerance) nextY = output.height / 2 - geometry.heightPx / 2;
        else if (Math.abs(bottom - output.height) <= edgeTolerance) nextY = geometry.heightPx / 2 - output.height / 2;
      }
      clip.positionX = nextX;
      clip.positionY = nextY;
      changed = changed || Math.abs(dx) > 1 || Math.abs(dy) > 1;
      updatePreviewGeometry();
    };
    const finish = () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", finish);
      previewStage.classList.remove("dragging");
      if (!changed) historyUndo.pop();
      else { historyRedo = []; updateDirty(); updateHistoryButtons(); scheduleAutosave(); }
      renderInspector();
    };
    previewStage.classList.add("dragging");
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", finish);
  }

  function mediaStatus(asset) {
    if (asset.status === "READY") return `${clock(asset.durationMs || 0)} / Ready`;
    if (asset.status === "FAILED") return "Processing failed";
    return asset.status === "PROCESSING" ? "Creating proxy" : "Queued";
  }

  function renderBin() {
    assetMap.clear();
    libraryAssets.forEach(item => assetMap.set(item.id, item));
    assets.forEach(item => assetMap.set(item.id, item));
    bin.replaceChildren();
    bin.dataset.view = mediaView;
    qa("[data-media-view]").forEach(button => button.classList.toggle("active", button.dataset.mediaView === mediaView));
    const montageMedia = assets.filter(asset => timeline.mediaAssetIds.includes(asset.id));
    montageMedia.forEach(asset => {
      const item = document.createElement("div");
      const visual = document.createElement("div");
      const copy = document.createElement("div");
      const name = document.createElement("strong");
      const status = document.createElement("span");
      item.className = "movie-bin-item";
      item.classList.toggle("project-attached", Boolean(asset.attached));
      item.draggable = canEdit && asset.status === "READY";
      item.dataset.assetId = asset.id;
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
        visual.append(image);
      } else visual.textContent = asset.kind;
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
        if (item.draggable) event.dataTransfer.setData("text/asset-id", asset.id);
      });
      item.addEventListener("click", () => previewAsset(asset));
      item.addEventListener("dblclick", () => {
        if (!canEdit || asset.status !== "READY") return;
        const trackKind = asset.kind === "AUDIO" ? "AUDIO" : "VIDEO";
        let track = timeline.tracks.find(value => value.kind === trackKind && !value.locked);
        if (!track) track = addTrack(trackKind);
        addClip(track, asset.id, timelineEnd());
      });
      bin.append(item);
    });
    if (!montageMedia.length) bin.innerHTML = '<p class="empty">Open + to add Project or Workspace media</p>';
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
    standalonePreviewAssetId = null;
    playheadMs = clamp(quantize(value), 0, Math.max(0, timelineEnd()));
    playheadNode.style.left = `${playheadMs / 1000 * zoom}px`;
    playheadLabel.textContent = clock(playheadMs);
    previewTime.textContent = `${clock(playheadMs)} / ${clock(timelineEnd())}`;
    previewScrub.max = Math.max(PRECISION_MS, timelineEnd());
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
    selectedId = null;
    selectedIds.clear();
    standalonePreviewAssetId = asset.id;
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
      if (!node) {
        node = document.createElement(asset.kind === "IMAGE" ? "img" : "video");
        node.className = "movie-preview-layer";
        node.dataset.clipId = item.clip.id;
        if (node.tagName === "VIDEO") { node.playsInline = true; node.preload = "auto"; node.muted = true; }
        previewLayers.append(node);
        visualPlayers.set(item.clip.id, node);
      }
      const source = asset.proxyUrl || asset.originalUrl;
      if (node.dataset.assetId !== asset.id) {
        node.dataset.assetId = asset.id;
        node.src = source;
        if (node.tagName === "VIDEO") node.load();
      }
      node.style.zIndex = String(item.clip.id === selectedId ? active.length + 2 : index + 1);
      node.classList.toggle("selected-layer", item.clip.id === selectedId);
      applyGeometry(node, asset, item.clip);
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
      if (shouldPlay) preview.play().catch(error => { previewStatus.textContent = error.message; });
      return;
    }
    syncVisualLayers(shouldPlay, force);
    const activeAudio = [];
    timeline.tracks.forEach(track => {
      if (track.muted) return;
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
    setPlayhead(playbackOrigin + now - playbackStartedAt, false);
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
    const name = document.createElement("input");
    const timeStep = (frameMs() / 1000).toFixed(4);
    const start = numberInput(((clip?.start || 0) / 1000).toFixed(3), timeStep, "0");
    const end = numberInput((((clip?.start || 0) + (clip?.duration || 0)) / 1000).toFixed(3), timeStep, "0");
    const marker = numberInput((playheadMs / 1000).toFixed(3), timeStep, "0");
    const positionX = numberInput(Math.round(Number(clip?.positionX || 0)), "1", "-7680");
    const positionY = numberInput(Math.round(Number(clip?.positionY || 0)), "1", "-7680");
    positionX.max = "7680"; positionY.max = "7680";
    const clipScale = document.createElement("input");
    clipScale.type = "range"; clipScale.min = ".05"; clipScale.max = "8"; clipScale.step = ".01";
    clipScale.value = Number(clip?.scale || 1).toFixed(2);
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
    [name, start, end, positionX, positionY, clipScale, speed, speedMethod, fadeIn, fadeOut, volume].forEach(control => control.disabled = !found || !canEdit || track?.locked);
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
        input.disabled = !canEdit || track.locked;
        input.onchange = () => mutate(() => { clip[key] = clamp(Number(input.value), minimum, maximum); syncPlayers(false, true); });
        return field(label, input);
      });
      inspector.append(inspectorGroup(inspectorTab === "EFFECTS" ? "Visual effects" : "Color correction", ...fields));
      return;
    }
    name.onchange = event => mutate(() => { clip.name = event.target.value; renderTimeline(); });
    start.onchange = event => mutate(() => {
      const requested = quantizeFrame(Number(event.target.value) * 1000);
      const minimum = Math.max(0, clip.start - clip.sourceStart / Number(clip.speed || 1));
      const maximum = clip.start + clip.duration - Math.max(1, Math.round(frameMs()));
      const nextStart = clamp(requested, minimum, maximum);
      const delta = nextStart - clip.start;
      clip.start = nextStart; clip.sourceStart += delta * Number(clip.speed || 1); clip.duration -= delta;
      renderTimeline(); syncPlayers(false, true);
    });
    end.onchange = event => mutate(() => {
      const requested = quantizeFrame(Number(event.target.value) * 1000);
      const minimum = clip.start + Math.max(1, Math.round(frameMs()));
      const maximum = clip.start + (assetDuration(clip) - clip.sourceStart) / Number(clip.speed || 1);
      clip.duration = clamp(requested, minimum, maximum) - clip.start;
      renderTimeline(); syncPlayers(false, true);
    });
    marker.onchange = event => { stopPlayback(); setPlayhead(quantizeFrame(Number(event.target.value) * 1000)); };
    const updatePosition = () => mutate(() => {
      clip.positionX = clamp(Number(positionX.value), -7680, 7680);
      clip.positionY = clamp(Number(positionY.value), -7680, 7680);
      updatePreviewGeometry();
    });
    positionX.onchange = updatePosition; positionY.onchange = updatePosition;
    let scaleRemembered = false;
    const scaleValue = document.createElement("output");
    const updateScaleValue = () => { scaleValue.textContent = `${Math.round(Number(clipScale.value) * 100)}%`; };
    clipScale.onpointerdown = () => { if (!scaleRemembered) { remember(); scaleRemembered = true; } };
    clipScale.oninput = () => {
      clip.scale = clamp(Number(clipScale.value), .05, 8);
      updateScaleValue(); updatePreviewGeometry();
    };
    clipScale.onchange = () => {
      clip.scale = clamp(Number(clipScale.value), .05, 8);
      historyRedo = []; updateDirty(); updateHistoryButtons(); scaleRemembered = false; scheduleAutosave();
      updateScaleValue(); updatePreviewGeometry();
    };
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
    scaleControls.className = "movie-scale-stepper"; scaleControls.append(scaleDown, clipScale, scaleValue, scaleUp); scaleWrap.append(scaleCaption, scaleControls);
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
    const source = document.createElement("div"); source.className = "movie-source-readout"; source.textContent = `Source media: ${asset?.name || clip.sourceAssetId} / ${clip.sourceAssetId}`;
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
      inspectorGroup("Timeline", field("Start", start), field("End", end), field("Playhead", marker), timelinePad),
      inspectorGroup("Transform", coordinates, pad),
      inspectorGroup("Playback", field("Speed", speed), field("Interpolation", speedMethod)),
      source,
    );
  }

  function selectClip(id, movePlayhead = false, additive = false) {
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
    const controls = document.createElement("div");
    const up = document.createElement("button");
    const down = document.createElement("button");
    const mute = document.createElement("button");
    const lock = document.createElement("button");
    const display = document.createElement("button");
    const loudnessGuide = document.createElement("input");
    const height = document.createElement("input");
    const remove = document.createElement("button");
    head.className = `movie-track-head${track.locked ? " locked" : ""}`;
    nameWrap.className = "movie-track-name";
    controls.className = "movie-track-controls";
    head.style.height = `${track.height}px`;
    title.textContent = track.name;
    kind.textContent = `${track.kind} / ${track.clips.length} clip${track.clips.length === 1 ? "" : "s"}`;
    nameWrap.append(title, kind);
    [up, down, mute, lock, display, remove].forEach(button => { button.type = "button"; button.className = "icon-button secondary movie-track-control"; });
    up.innerHTML = "&#8593;"; up.title = "Move track up"; up.onclick = () => moveTrack(track, -1);
    down.innerHTML = "&#8595;"; down.title = "Move track down"; down.onclick = () => moveTrack(track, 1);
    mute.innerHTML = track.muted ? "&#128263;" : "&#128266;"; mute.classList.toggle("off", track.muted); mute.title = track.muted ? "Unmute track" : "Mute track";
    lock.innerHTML = "&#128065;"; lock.classList.toggle("off", track.locked); lock.title = track.locked ? "Show and unlock track" : "Hide and lock track";
    mute.classList.add("movie-track-mute"); lock.classList.add("movie-track-lock");
    display.textContent = track.displayMode === "WAVEFORM" ? "W" : "C";
    display.title = track.displayMode === "WAVEFORM" ? "Show clips and frames" : "Show waveform and loudness guide";
    height.type = "range"; height.min = "56"; height.max = "200"; height.step = "4"; height.value = String(track.height);
    height.className = "movie-track-size"; height.title = "Track height"; height.setAttribute("aria-label", "Track height");
    remove.innerHTML = "&#215;"; remove.title = "Delete empty track";
    mute.onclick = () => mutate(() => { track.muted = !track.muted; renderTimeline(); syncPlayers(false, true); });
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
    controls.append(up, down, mute, lock, display, loudnessGuide, height, remove);
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
    if (!selectedIds.has(clip.id)) selectClip(clip.id, false, additive);
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
    let mode = event.shiftKey ? "marquee" : null;
    const move = next => {
      const dx = next.clientX - startX;
      const dy = next.clientY - startY;
      if (!mode && Math.hypot(dx, dy) >= 4) mode = Math.abs(dx) >= Math.abs(dy) * 1.5 ? "pan" : "marquee";
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
      if (!mode) { selectClip(null); setPlayhead((next.clientX - lane.getBoundingClientRect().left) / zoom * 1000); }
      else if (mode === "marquee") renderInspector();
    };
    window.addEventListener("pointermove", move); window.addEventListener("pointerup", finish);
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
      const lane = document.createElement("div"); lane.className = `movie-track-lane${track.locked ? " locked" : ""}${track.displayMode === "WAVEFORM" ? " waveform-mode" : ""}`; lane.dataset.trackId = track.id;
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
    let items = libraryAssets.filter(asset => (libraryScope === "workspace" || asset.attached) && (kind === "ALL" || asset.kind === kind));
    if (sort === "alpha") items.sort((a, b) => a.name.localeCompare(b.name));
    q("[data-media-library-count]").textContent = items.length;
    libraryGrid.replaceChildren();
    items.forEach(asset => {
      const button = document.createElement("button");
      const visual = document.createElement("div");
      const copy = document.createElement("div");
      button.type = "button"; button.className = `movie-library-card${asset.attached ? " attached" : ""}`;
      visual.className = "movie-library-card-visual"; copy.className = "movie-library-card-copy";
      if (asset.thumbnailUrl || asset.waveformUrl) { const image = document.createElement("img"); image.src = asset.thumbnailUrl || asset.waveformUrl; image.alt = ""; visual.append(image); }
      else visual.textContent = asset.kind;
      copy.innerHTML = `<strong>${asset.name}</strong><small>${asset.kind} / ${mediaStatus(asset)}</small><small>${asset.attached ? "Attached to project" : "Workspace media"}</small>`;
      button.append(visual, copy);
      button.onclick = async () => {
        if (asset.attached) {
          mutate(() => { if (!timeline.mediaAssetIds.includes(asset.id)) timeline.mediaAssetIds.push(asset.id); });
          libraryDialog.close(); renderBin(); bin.querySelector(`[data-asset-id="${asset.id}"]`)?.scrollIntoView({block: "nearest"}); return;
        }
        button.disabled = true;
        try { await requestJson(root.dataset.mediaUrl, {method: "POST", headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()}, body: JSON.stringify({assetId: asset.id})}); mutate(() => { if (!timeline.mediaAssetIds.includes(asset.id)) timeline.mediaAssetIds.push(asset.id); }); await refreshMedia(); libraryDialog.close(); }
        catch (error) { toast(error.message, "error"); }
        button.disabled = false;
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
      const parsed = JSON.parse(await file.text());
      const importedTimeline = parsed.timeline || parsed;
      if (!Array.isArray(importedTimeline.tracks)) throw new Error("The montage file has no tracks");
      pendingImport = {file, parsed, timeline: importedTimeline};
      q("[data-import-file-name]").textContent = file.name;
      const trackSelect = q("[data-import-track]"); trackSelect.replaceChildren();
      importedTimeline.tracks.forEach((track, index) => { const option = document.createElement("option"); option.value = index; option.textContent = `${track.name || track.kind || "Track"} (${track.clips?.length || 0} clips)`; trackSelect.append(option); });
      q("[data-import-mode]").value = "project"; q("[data-import-track-field]").hidden = true;
      importDialog.showModal();
    } catch (error) { toast(error.message || "The montage file is invalid", "error"); }
    event.target.value = "";
  });
  q("[data-import-mode]")?.addEventListener("change", event => { q("[data-import-track-field]").hidden = event.target.value !== "track"; });
  const closeImport = () => { pendingImport = null; importDialog?.close(); };
  q("[data-import-close]")?.addEventListener("click", closeImport); q("[data-import-cancel]")?.addEventListener("click", closeImport);
  q("[data-import-apply]")?.addEventListener("click", async () => {
    if (!pendingImport) return;
    if (q("[data-import-mode]").value === "track") {
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

  q("[data-media-library-open]")?.addEventListener("click", () => { libraryScope = "project"; qa("[data-media-scope]").forEach(button => button.classList.toggle("active", button.dataset.mediaScope === libraryScope)); renderLibrary(); libraryDialog.showModal(); });
  q("[data-media-library-close]")?.addEventListener("click", () => libraryDialog.close());
  libraryDialog?.addEventListener("click", event => { if (event.target === libraryDialog) libraryDialog.close(); });
  qa("[data-media-scope]").forEach(button => button.onclick = () => { libraryScope = button.dataset.mediaScope; qa("[data-media-scope]").forEach(item => item.classList.toggle("active", item === button)); renderLibrary(); });
  q("[data-media-kind]")?.addEventListener("change", renderLibrary);
  q("[data-media-sort]")?.addEventListener("change", renderLibrary);
  q("[data-media-upload-form]")?.addEventListener("submit", event => {
    event.preventDefault();
    const input = q("[data-media-files]");
    const files = [...input.files].slice(0, 10);
    if (!files.length) return;
    const form = new FormData(); files.forEach(file => form.append("files", file));
    const progress = q("[data-media-progress]"); const bar = progress.querySelector("i"); progress.hidden = false; bar.style.width = "0";
    const xhr = new XMLHttpRequest(); xhr.open("POST", root.dataset.mediaUrl); xhr.setRequestHeader("X-CSRFToken", csrfToken());
    xhr.upload.onprogress = update => { if (update.lengthComputable) bar.style.width = `${Math.round(update.loaded / update.total * 100)}%`; };
    xhr.onload = async () => { progress.hidden = true; input.value = ""; let data = {}; try { data = JSON.parse(xhr.responseText); } catch (_) {} if (xhr.status >= 400) toast(data.error || "Upload failed", "error"); else { mutate(() => (data.items || []).forEach(item => { if (!timeline.mediaAssetIds.includes(item.id)) timeline.mediaAssetIds.push(item.id); })); if (data.errors?.length) toast(data.errors.join(" / "), "error"); } await refreshMedia(); };
    xhr.onerror = () => { progress.hidden = true; toast("Upload failed", "error"); };
    xhr.send(form);
  });

  qa("[data-add-track]").forEach(button => button.onclick = () => addTrack(button.dataset.addTrack));
  q("[data-delete-track]")?.addEventListener("click", removeTargetTrack);
  q("[data-auto-cut]").onclick = () => {
    let track = timeline.tracks.find(item => item.kind === "VIDEO" && !item.locked);
    if (!track) track = addTrack("VIDEO");
    const target = track;
    mutate(() => {
      let cursor = 0;
      target.clips = assets.filter(asset => asset.status === "READY" && asset.kind === "VIDEO").map(asset => { const duration = Math.max(200, asset.durationMs || 8000); const clip = {id: uid(), assetId: asset.id, sourceAssetId: asset.id, name: asset.name, start: cursor, sourceStart: 0, duration, volume: 1, speed: 1, speedMethod: "FRAME_SAMPLE", fadeIn: 0, fadeOut: 0, volumeKeyframes: []}; cursor += duration; return clip; });
    });
    renderTimeline();
  };
  q("[data-preview-play]").onclick = togglePlayback;
  q("[data-preview-stop]").onclick = () => stopPlayback(true);
  qa("[data-preview-step]").forEach(button => bindHold(button, () => { stopPlayback(); setPlayhead(playheadMs + Number(button.dataset.previewStep) * frameMs()); }));
  previewScrub.oninput = event => { stopPlayback(); setPlayhead(Number(event.target.value)); };
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
  q("[data-editor-undo]").onclick = undo;
  q("[data-editor-redo]").onclick = redo;
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
        const defaults = {"16:9": "1920x1080", "9:16": "1080x1920", "1:1": "1080x1080"};
        q("[data-movie-resolution]").value = defaults[control.value] || "1920x1080";
        applyCanvas();
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
    if (shortcut && (event.code === "KeyC" || event.key.toLowerCase() === "c") && !editing) { event.preventDefault(); copySelected(); return; }
    if (shortcut && (event.code === "KeyV" || event.key.toLowerCase() === "v") && !editing) { event.preventDefault(); pasteSelected(); return; }
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
  normalizeTimeline(); applyCanvas(); renderBin(); renderTimeline(); renderInspector(); renderRenderJobs(); renderLibrary(); scheduleRenderPoll(); setPlayhead(0, true, true);
  savedSignature = signature(); updateDirty(); updateHistoryButtons(); refreshMedia();
})();

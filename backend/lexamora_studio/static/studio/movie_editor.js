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
  const saved = readJson("movie-timeline-data");
  let timeline = saved && Array.isArray(saved.tracks) ? saved : {schemaVersion: 1, tracks: []};
  let selectedId = null;
  let selectedIds = new Set();
  let clipClipboard = [];
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
  const settingSnapshots = new WeakMap();
  const PRECISION_MS = 10;

  const q = selector => root.querySelector(selector);
  const qa = selector => [...root.querySelectorAll(selector)];
  const bin = q("[data-movie-bin]");
  const tracksNode = q("[data-movie-tracks]");
  const headsNode = q("[data-track-heads]");
  const preview = q("[data-movie-preview]");
  const previewStage = q("[data-preview-stage]");
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
    track.clips.filter(clip => clipKind(clip) === "VIDEO").map(clip => clip.start + clip.duration)
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
  const currentState = () => ({
    timeline: structuredClone(timeline),
    title: q("[data-movie-title]").value,
    aspectRatio: q("[data-movie-ratio]").value,
    resolution: q("[data-movie-resolution]").value,
    fps: q("[data-movie-fps]").value,
    selectedId,
  });
  const signature = state => JSON.stringify(state || currentState());
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
  };

  function normalizeTimeline() {
    timeline.schemaVersion = 1;
    timeline.tracks = Array.isArray(timeline.tracks) ? timeline.tracks : [];
    timeline.tracks.forEach((track, trackIndex) => {
      track.id ||= uid();
      track.kind = ["VIDEO", "AUDIO", "TEXT"].includes(track.kind) ? track.kind : "VIDEO";
      track.name ||= `${track.kind === "VIDEO" ? "Video" : "Audio"} ${trackIndex + 1}`;
      track.muted = Boolean(track.muted);
      track.locked = Boolean(track.locked);
      track.height = clamp(Number(track.height || defaultTrackHeight), 56, 200);
      track.clips = Array.isArray(track.clips) ? track.clips : [];
      track.clips.forEach(clip => {
        clip.start = Math.max(0, quantize(clip.start));
        clip.sourceStart = Math.max(0, quantize(clip.sourceStart));
        const available = Math.max(200, assetDuration(clip) - clip.sourceStart);
        clip.duration = clamp(quantize(clip.duration || 200), 200, available);
        clip.volume = clamp(Number(clip.volume ?? 1), 0, 2);
        clip.scale = clamp(Number(clip.scale ?? 1), .05, 8);
        clip.positionX = clamp(Number(clip.positionX ?? 0), -500, 500);
        clip.positionY = clamp(Number(clip.positionY ?? 0), -500, 500);
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
    const active = activeClip("VIDEO", playheadMs);
    return active ? {...active, asset: assetMap.get(active.clip.assetId)} : {asset: null, clip: null, track: null};
  }

  function updatePreviewGeometry() {
    const {asset, clip} = previewClipData();
    const [canvasWidth, canvasHeight] = (q("[data-movie-ratio]").value || "16:9").split(":").map(Number);
    const canvasRatio = canvasWidth / canvasHeight;
    const sourceWidth = asset?.width || preview.videoWidth;
    const sourceHeight = asset?.height || preview.videoHeight;
    const sourceRatio = sourceWidth && sourceHeight ? sourceWidth / sourceHeight : canvasRatio;
    const scale = clamp(Number(clip?.scale ?? 1), .05, 8);
    const width = (sourceRatio >= canvasRatio ? sourceRatio / canvasRatio * 100 : 100) * scale;
    const height = (sourceRatio >= canvasRatio ? 100 : canvasRatio / sourceRatio * 100) * scale;
    const left = 50 + Number(clip?.positionX || 0);
    const top = 50 + Number(clip?.positionY || 0);
    [preview, previewOutline].forEach(node => {
      if (!node) return;
      node.style.width = `${width}%`;
      node.style.height = `${height}%`;
      node.style.left = `${left}%`;
      node.style.top = `${top}%`;
      node.style.transform = "translate(-50%,-50%)";
    });
    if (previewOutline) previewOutline.hidden = !asset;
    previewStage.classList.toggle("source-mismatch", Math.abs(sourceRatio - canvasRatio) > .01);
    previewGeometry = {clip, width, height};
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
    let changed = false;
    const move = next => {
      const dx = next.clientX - startX;
      const dy = next.clientY - startY;
      clip.positionX = clamp(originalX + dx / Math.max(1, previewStage.clientWidth) * 100, -500, 500);
      clip.positionY = clamp(originalY + dy / Math.max(1, previewStage.clientHeight) * 100, -500, 500);
      changed = changed || Math.abs(dx) > 1 || Math.abs(dy) > 1;
      updatePreviewGeometry();
    };
    const finish = () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", finish);
      previewStage.classList.remove("dragging");
      if (!changed) historyUndo.pop();
      else { historyRedo = []; updateDirty(); updateHistoryButtons(); }
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
    assets.forEach(asset => {
      const item = document.createElement("div");
      const visual = document.createElement("div");
      const copy = document.createElement("div");
      const name = document.createElement("strong");
      const status = document.createElement("span");
      item.className = "movie-bin-item";
      item.draggable = canEdit && asset.status === "READY";
      item.dataset.assetId = asset.id;
      visual.className = "movie-bin-visual";
      copy.className = "movie-bin-copy";
      name.textContent = mediaView === "list" ? asset.name : asset.name.slice(0, 3);
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
        let track = timeline.tracks.find(value => value.kind === asset.kind && !value.locked);
        if (!track) track = addTrack(asset.kind);
        addClip(track, asset.id, timelineEnd());
      });
      bin.append(item);
    });
    if (!assets.length) bin.innerHTML = '<p class="empty">Open + to choose Project or Workspace media</p>';
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
      const data = await requestJson(root.dataset.renderUrl, {headers: {"X-Requested-With": "XMLHttpRequest"}});
      renderJobs = data.items || [];
      renderRenderJobs();
    } catch (error) { toast(error.message, "error"); }
    scheduleRenderPoll();
  }

  function scheduleRenderPoll() {
    clearTimeout(renderPoll);
    if (renderJobs.some(job => ["QUEUED", "RUNNING"].includes(job.status))) renderPoll = setTimeout(refreshRenders, document.hidden ? 5000 : 1800);
  }

  async function saveTimeline(silent = false) {
    if (!canEdit) return true;
    normalizeTimeline();
    stateNode.textContent = "Saving";
    stateNode.classList.remove("error");
    try {
      const data = await requestJson(root.dataset.saveUrl, {
        method: "POST",
        headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
        body: JSON.stringify({title: q("[data-movie-title]").value, aspectRatio: q("[data-movie-ratio]").value, resolution: q("[data-movie-resolution]").value, fps: Number(q("[data-movie-fps]").value), timeline}),
      });
      if (data.timeline) timeline = data.timeline;
      normalizeTimeline();
      renderTimeline();
      renderInspector();
      savedSignature = signature();
      dirty = false;
      stateNode.textContent = "Saved";
      historyUndo = [];
      historyRedo = [];
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
    }
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
        body: JSON.stringify({title: q("[data-movie-title]").value, profile: q("[data-render-profile]").value, audioProfile: q("[data-render-audio-profile]").value, targetLufs: Number(q("[data-render-target-lufs]").value)}),
      });
      renderJobs.unshift(data);
      renderRenderJobs();
      renderPanel.hidden = false;
      toast("MP4 render queued");
      scheduleRenderPoll();
    } catch (error) { toast(error.message, "error"); }
    finally { button.disabled = false; button.textContent = "Save and render MP4"; }
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
      const top = activeClip("VIDEO", playheadMs)?.clip.id || null;
      if (top !== selectedId || selectedIds.size > 1) {
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
    const target = Math.max(0, (clip.sourceStart + playheadMs - clip.start) / 1000);
    const changed = player.dataset.assetId !== asset.id || player.dataset.clipId !== clip.id;
    const seek = () => {
      const maximum = Number.isFinite(player.duration) ? Math.max(0, player.duration - 0.01) : target;
      const safeTarget = clamp(target, 0, maximum);
      if (force || changed || Math.abs((player.currentTime || 0) - safeTarget) > (shouldPlay ? 0.65 : 0.04)) player.currentTime = safeTarget;
      player.volume = track?.muted ? 0 : Math.min(1, clip.volume ?? 1);
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

  function syncPlayers(shouldPlay, force = false) {
    if (standalonePreviewAssetId) {
      if (shouldPlay) preview.play().catch(error => { previewStatus.textContent = error.message; });
      return;
    }
    const visibleVideo = activeClip("VIDEO", playheadMs);
    setPlayer(preview, visibleVideo, shouldPlay, force);
    const activeAudio = [];
    timeline.tracks.forEach(track => {
      if (track.muted) return;
      track.clips.forEach(clip => {
        const asset = assetMap.get(clip.assetId);
        const hasAudio = asset?.hasAudio || Boolean(asset?.waveformUrl) || clipKind(clip) === "AUDIO";
        const isPreviewAudio = visibleVideo?.clip.id === clip.id;
        if (hasAudio && !isPreviewAudio && playheadMs >= clip.start && playheadMs < clip.start + clip.duration) {
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
  const inspectorButton = (text, title, action) => {
    const button = document.createElement("button");
    button.type = "button"; button.className = "icon-button secondary"; button.textContent = text;
    button.title = title; button.setAttribute("aria-label", title); button.onclick = action;
    return button;
  };

  function renderInspector() {
    inspector.replaceChildren();
    inspectorActions.replaceChildren();
    qa("[data-inspector-tab]").forEach(button => button.classList.toggle("active", button.dataset.inspectorTab === inspectorTab));
    const actionSet = disabled => {
      const actions = [
        inspectorButton("S", "Split at playhead", splitSelected),
        inspectorButton("C", "Copy selected clips", copySelected),
        inspectorButton("x", "Delete selection", deleteSelected),
      ];
      actions.forEach(button => { button.disabled = disabled; });
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
    if (["EFFECTS", "COLOR"].includes(inspectorTab)) {
      const empty = document.createElement("p");
      empty.className = "movie-inspector-empty";
      empty.textContent = found ? `${inspectorTab === "EFFECTS" ? "Effects" : "Color"} controls are ready for the next editing module` : "Select a timeline clip";
      inspector.append(empty);
      return;
    }
    const clip = found?.clip;
    const track = found?.track;
    const asset = clip ? assetMap.get(clip.assetId) : null;
    const name = document.createElement("input");
    const timeStep = (frameMs() / 1000).toFixed(4);
    const start = numberInput(((clip?.start || 0) / 1000).toFixed(3), timeStep, "0");
    const end = numberInput((((clip?.start || 0) + (clip?.duration || 0)) / 1000).toFixed(3), timeStep, "0");
    const marker = numberInput((playheadMs / 1000).toFixed(3), timeStep, "0");
    const positionX = numberInput(Number(clip?.positionX || 0).toFixed(2), ".25", "-500");
    const positionY = numberInput(Number(clip?.positionY || 0).toFixed(2), ".25", "-500");
    positionX.max = "500"; positionY.max = "500";
    const clipScale = numberInput(Number(clip?.scale || 0).toFixed(2), ".01", ".05");
    clipScale.max = "8";
    const volume = document.createElement("input");
    name.value = clip?.name || asset?.name || "";
    marker.dataset.inspectorPlayhead = "";
    volume.type = "range"; volume.min = "0"; volume.max = "2"; volume.step = ".05"; volume.value = clip?.volume ?? 0;
    [name, start, end, positionX, positionY, clipScale, volume].forEach(control => control.disabled = !found || !canEdit || track?.locked);
    marker.disabled = !timelineEnd();
    if (!found) {
      inspector.append(field("Name", name), field("Start", start), field("End", end), field("Playhead", marker));
      return;
    }
    name.onchange = event => mutate(() => { clip.name = event.target.value; renderTimeline(); });
    start.onchange = event => mutate(() => {
      const requested = quantizeFrame(Number(event.target.value) * 1000);
      const minimum = Math.max(0, clip.start - clip.sourceStart);
      const maximum = clip.start + clip.duration - Math.max(1, Math.round(frameMs()));
      const nextStart = clamp(requested, minimum, maximum);
      const delta = nextStart - clip.start;
      clip.start = nextStart; clip.sourceStart += delta; clip.duration -= delta;
      renderTimeline(); syncPlayers(false, true);
    });
    end.onchange = event => mutate(() => {
      const requested = quantizeFrame(Number(event.target.value) * 1000);
      const minimum = clip.start + Math.max(1, Math.round(frameMs()));
      const maximum = clip.start + assetDuration(clip) - clip.sourceStart;
      clip.duration = clamp(requested, minimum, maximum) - clip.start;
      renderTimeline(); syncPlayers(false, true);
    });
    marker.onchange = event => { stopPlayback(); setPlayhead(quantizeFrame(Number(event.target.value) * 1000)); };
    const updatePosition = () => mutate(() => {
      clip.positionX = clamp(Number(positionX.value), -500, 500);
      clip.positionY = clamp(Number(positionY.value), -500, 500);
      updatePreviewGeometry();
    });
    positionX.onchange = updatePosition; positionY.onchange = updatePosition;
    clipScale.onchange = () => mutate(() => { clip.scale = clamp(Number(clipScale.value), .05, 8); updatePreviewGeometry(); });
    volume.onchange = event => mutate(() => { clip.volume = Number(event.target.value); syncPlayers(false, true); });
    if (inspectorTab === "AUDIO") {
      inspector.append(field("Volume", volume), field("Start", start), field("End", end), field("Playhead", marker));
      return;
    }
    const scaleWrap = document.createElement("label");
    const scaleCaption = document.createElement("span");
    const scaleControls = document.createElement("span");
    const scaleDown = inspectorButton("-", "Reduce clip scale by 1%", () => {
      if (!found || track.locked) return;
      mutate(() => { clip.scale = clamp(Number(clip.scale || 1) - .01, .05, 8); updatePreviewGeometry(); });
      renderInspector();
    });
    const scaleUp = inspectorButton("+", "Increase clip scale by 1%", () => {
      if (!found || track.locked) return;
      mutate(() => { clip.scale = clamp(Number(clip.scale || 1) + .01, .05, 8); updatePreviewGeometry(); });
      renderInspector();
    });
    scaleDown.disabled = scaleUp.disabled = !canEdit || track.locked;
    scaleWrap.className = "movie-inspector-field"; scaleCaption.className = "movie-inspector-label"; scaleCaption.textContent = "Scale";
    scaleControls.className = "movie-scale-stepper"; scaleControls.append(scaleDown, clipScale, scaleUp); scaleWrap.append(scaleCaption, scaleControls);
    const pad = document.createElement("div");
    pad.className = "movie-position-pad"; pad.title = "Move rendered video inside the output frame";
    const nudgePosition = (dx, dy) => {
      mutate(() => { clip.positionX = clamp(Number(clip.positionX || 0) + dx, -500, 500); clip.positionY = clamp(Number(clip.positionY || 0) + dy, -500, 500); updatePreviewGeometry(); });
      renderInspector();
    };
    [["&#8593;", 0, -1, "Move video up"], ["&#8592;", -1, 0, "Move video left"], ["&#9679;", 0, 0, "Center video"], ["&#8594;", 1, 0, "Move video right"], ["&#8595;", 0, 1, "Move video down"]].forEach(([symbol, dx, dy, title]) => {
      const button = document.createElement("button"); button.type = "button"; button.className = "icon-button secondary"; button.innerHTML = symbol; button.title = title;
      button.disabled = !canEdit || track.locked;
      if (dx) button.dataset.positionX = dx; if (dy) button.dataset.positionY = dy;
      if (!dx && !dy) button.classList.add("movie-position-home");
      button.onclick = () => {
        if (dx || dy) nudgePosition(dx, dy);
        else {
          mutate(() => { clip.positionX = 0; clip.positionY = 0; updatePreviewGeometry(); });
          renderInspector();
        }
      };
      pad.append(button);
    });
    inspector.append(field("Name", name), field("Start", start), field("End", end), field("Playhead", marker), field("X", positionX), field("Y", positionY), scaleWrap, pad);
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
      return found ? {clip: structuredClone(found.clip), trackId: found.track.id} : null;
    }).filter(Boolean);
    if (!items.length) return toast("Select one or more clips first");
    const origin = Math.min(...items.map(item => item.clip.start));
    clipClipboard = items.map(item => ({...item, offset: item.clip.start - origin}));
    toast(`${clipClipboard.length} clip${clipClipboard.length === 1 ? "" : "s"} copied`);
  }

  function pasteSelected() {
    if (!clipClipboard.length) return toast("Copy clips first");
    const pasted = [];
    mutate(() => {
      clipClipboard.forEach(item => {
        const originalTrack = timeline.tracks.find(track => track.id === item.trackId && !track.locked);
        const target = originalTrack || timeline.tracks.find(track => !track.locked);
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
      const right = {...clip, id: uid(), name: `${clip.name} B`, start: clip.start + offset, sourceStart: clip.sourceStart + offset, duration: clip.duration - offset};
      clip.duration = offset;
      track.clips.push(right);
      selectedId = right.id;
      selectedIds = new Set([right.id]);
    });
    renderTimeline(); renderInspector();
  }

  function nudgeSelected(direction) {
    const found = [...selectedIds].map(findClip).filter(item => item && !item.track.locked);
    if (!found.length) return;
    const delta = direction * frameMs();
    mutate(() => { found.forEach(item => { item.clip.start = Math.max(0, quantize(item.clip.start + delta)); }); });
    renderTimeline(); renderInspector(); setPlayhead(Math.min(...found.map(item => item.clip.start)), false);
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
      clip = {id: uid(), assetId, name: asset.name, start: snapTime(start, null), sourceStart: 0, duration: Math.max(200, asset.durationMs || 8000), volume: 1};
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
    const height = document.createElement("input");
    const remove = document.createElement("button");
    head.className = `movie-track-head${track.locked ? " locked" : ""}`;
    nameWrap.className = "movie-track-name";
    controls.className = "movie-track-controls";
    head.style.height = `${track.height}px`;
    title.textContent = track.name;
    kind.textContent = `${track.kind} / ${track.clips.length} clip${track.clips.length === 1 ? "" : "s"}`;
    nameWrap.append(title, kind);
    [up, down, mute, lock, remove].forEach(button => { button.type = "button"; button.className = "icon-button secondary movie-track-control"; });
    up.innerHTML = "&#8593;"; up.title = "Move track up"; up.onclick = () => moveTrack(track, -1);
    down.innerHTML = "&#8595;"; down.title = "Move track down"; down.onclick = () => moveTrack(track, 1);
    mute.innerHTML = track.muted ? "&#128263;" : "&#128266;"; mute.classList.toggle("off", track.muted); mute.title = track.muted ? "Unmute track" : "Mute track";
    lock.innerHTML = "&#128065;"; lock.classList.toggle("off", track.locked); lock.title = track.locked ? "Show and unlock track" : "Hide and lock track";
    height.type = "range"; height.min = "56"; height.max = "200"; height.step = "4"; height.value = String(track.height);
    height.className = "movie-track-size"; height.title = "Track height"; height.setAttribute("aria-label", "Track height");
    remove.innerHTML = "&#215;"; remove.title = "Delete empty track";
    mute.onclick = () => mutate(() => { track.muted = !track.muted; renderTimeline(); syncPlayers(false, true); });
    lock.onclick = () => mutate(() => { track.locked = !track.locked; renderTimeline(); renderInspector(); });
    height.oninput = () => {
      const next = clamp(Number(height.value), 56, 200);
      head.style.height = `${next}px`;
      const lane = q(`.movie-track-lane[data-track-id="${CSS.escape(track.id)}"]`);
      if (lane) lane.style.height = `${next}px`;
    };
    height.onchange = () => {
      mutate(() => { track.height = clamp(Number(height.value), 56, 200); });
      localStorage.setItem("studio-movie-track-height", String(track.height));
      renderTimeline();
    };
    remove.onclick = () => {
      if (track.clips.length) return toast("Remove clips before deleting this track");
      mutate(() => { timeline.tracks = timeline.tracks.filter(item => item.id !== track.id); });
      renderTimeline();
    };
    controls.append(up, down, mute, lock, height, remove);
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
    if (mode === "move" && (event.ctrlKey || event.metaKey || event.shiftKey)) {
      selectClip(clip.id, false, true);
      node.dataset.suppressClick = "true";
      return;
    }
    if (!selectedIds.has(clip.id)) selectClip(clip.id);
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
        shift = clamp(quantize(shift), -anchor.original.sourceStart, anchor.original.duration - 200);
        clip.start = anchor.original.start + shift; clip.sourceStart = anchor.original.sourceStart + shift; clip.duration = anchor.original.duration - shift;
      } else {
        const nextEnd = snapTime(anchor.original.start + anchor.original.duration + delta, clip.id);
        clip.duration = clamp(quantize(nextEnd - anchor.original.start), 200, assetDuration(clip) - anchor.original.sourceStart);
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
        historyRedo = []; updateDirty(); updateHistoryButtons();
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
      selectClip(clip.id, true, event.ctrlKey || event.metaKey || event.shiftKey);
    };
    node.ondblclick = event => { event.stopPropagation(); selectClip(clip.id, true); togglePlayback(); };
    body.onpointerdown = event => beginClipGesture(event, clip, track, "move", node);
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
    normalizeTimeline();
    const duration = visibleDuration();
    timelineCanvas.style.width = `${Math.max(timelineScroll.clientWidth || 600, duration / 1000 * zoom)}px`;
    const minor = Math.max(4, (zoom >= 50 ? 200 : zoom >= 20 ? 1000 : 2000) / 1000 * zoom);
    tracksNode.style.setProperty("--grid-step", `${minor}px`); tracksNode.style.setProperty("--grid-step-minus", `${Math.max(1, minor - 1)}px`);
    headsNode.replaceChildren(); tracksNode.replaceChildren(); renderRuler(duration);
    timeline.tracks.forEach(track => {
      headsNode.append(makeTrackHead(track));
      const lane = document.createElement("div"); lane.className = "movie-track-lane"; lane.dataset.trackId = track.id;
      lane.style.height = `${track.height}px`;
      lane.onpointerdown = event => {
        if (event.target !== lane) return;
        if (event.ctrlKey || event.metaKey || event.shiftKey) beginMarquee(event, lane);
        else beginTimelinePan(event, lane);
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
      if (!track.clips.length) lane.innerHTML = '<span class="movie-empty">Drop a clip anywhere across this track</span>';
      tracksNode.append(lane);
    });
    setPlayhead(playheadMs, false, true);
    q("[data-zoom-label]").textContent = `${zoom} px/s`;
    zoomInput.value = zoom;
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
        if (asset.attached) { libraryDialog.close(); bin.querySelector(`[data-asset-id="${asset.id}"]`)?.scrollIntoView({block: "nearest"}); return; }
        button.disabled = true;
        try { await requestJson(root.dataset.mediaUrl, {method: "POST", headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()}, body: JSON.stringify({assetId: asset.id})}); await refreshMedia(); }
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
      const data = await requestJson(root.dataset.historyUrl);
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
    xhr.onload = async () => { progress.hidden = true; input.value = ""; let data = {}; try { data = JSON.parse(xhr.responseText); } catch (_) {} if (xhr.status >= 400) toast(data.error || "Upload failed", "error"); else if (data.errors?.length) toast(data.errors.join(" / "), "error"); await refreshMedia(); };
    xhr.onerror = () => { progress.hidden = true; toast("Upload failed", "error"); };
    xhr.send(form);
  });

  qa("[data-add-track]").forEach(button => button.onclick = () => addTrack(button.dataset.addTrack));
  q("[data-auto-cut]").onclick = () => {
    let track = timeline.tracks.find(item => item.kind === "VIDEO" && !item.locked);
    if (!track) track = addTrack("VIDEO");
    const target = track;
    mutate(() => {
      let cursor = 0;
      target.clips = assets.filter(asset => asset.status === "READY" && asset.kind === "VIDEO").map(asset => { const duration = Math.max(200, asset.durationMs || 8000); const clip = {id: uid(), assetId: asset.id, name: asset.name, start: cursor, sourceStart: 0, duration, volume: 1}; cursor += duration; return clip; });
    });
    renderTimeline();
  };
  q("[data-preview-play]").onclick = togglePlayback;
  q("[data-preview-stop]").onclick = () => stopPlayback(true);
  qa("[data-preview-step]").forEach(button => button.onclick = () => { stopPlayback(); setPlayhead(playheadMs + Number(button.dataset.previewStep) * frameMs()); });
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
  qa("[data-clip-nudge]").forEach(button => button.onclick = () => nudgeSelected(Number(button.dataset.clipNudge)));
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
  q("[data-export-timeline]").onclick = () => { const blob = new Blob([JSON.stringify(timeline, null, 2)], {type: "application/json"}); const link = document.createElement("a"); link.href = URL.createObjectURL(blob); link.download = `${q("[data-movie-title]").value || "draft"}-timeline.json`; link.click(); URL.revokeObjectURL(link.href); };
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
      updateDirty(); updateHistoryButtons();
    });
  });

  document.addEventListener("keydown", event => {
    const editing = event.target.matches("input,textarea,select") || event.target.isContentEditable;
    const shortcut = event.ctrlKey || event.metaKey;
    if (shortcut && (event.code === "KeyZ" || event.key.toLowerCase() === "z")) { event.preventDefault(); event.shiftKey ? redo() : undo(); return; }
    if (shortcut && (event.code === "KeyY" || event.key.toLowerCase() === "y")) { event.preventDefault(); redo(); return; }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "c" && !editing) { event.preventDefault(); copySelected(); return; }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "v" && !editing) { event.preventDefault(); pasteSelected(); return; }
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
    qa("details[open]").forEach(details => {
      if (!event.target.closest("summary") || !details.contains(event.target)) details.removeAttribute("open");
    });
  });
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
    const applyTrackWidth = () => timelineShell.style.setProperty("--track-sidebar-width", `${trackWidth.value}px`);
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

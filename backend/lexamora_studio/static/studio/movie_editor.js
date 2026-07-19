(() => {
  const root = document.querySelector("[data-movie-editor]");
  if (!root) return;

  const canEdit = root.dataset.canEdit === "true";
  const canExport = root.dataset.canExport === "true";
  let assets = JSON.parse(document.getElementById("movie-assets-data").textContent || "[]");
  let renderJobs = JSON.parse(document.getElementById("movie-render-jobs-data")?.textContent || "[]");
  const saved = JSON.parse(document.getElementById("movie-timeline-data").textContent || "{}");
  let timeline = saved && Array.isArray(saved.tracks) ? saved : {schemaVersion: 1, tracks: []};
  let selectedId = null;
  let playheadMs = 0;
  let dirty = false;
  let snapping = true;
  let zoom = Math.max(6, Math.min(80, Number(localStorage.getItem("studio-movie-zoom")) || 24));
  let playing = false;
  let playbackFrame = 0;
  let playbackOrigin = 0;
  let playbackStartedAt = 0;
  let mediaPoll = null;
  let renderPoll = null;

  const bin = root.querySelector("[data-movie-bin]");
  const tracksNode = root.querySelector("[data-movie-tracks]");
  const headsNode = root.querySelector("[data-track-heads]");
  const preview = root.querySelector("[data-movie-preview]");
  const inspector = root.querySelector("[data-clip-inspector]");
  const stateNode = root.querySelector("[data-movie-state]");
  const playheadLabel = root.querySelector("[data-playhead-label]");
  const previewTime = root.querySelector("[data-preview-time]");
  const playheadNode = root.querySelector("[data-playhead]");
  const snapGuide = root.querySelector("[data-snap-guide]");
  const timelineScroll = root.querySelector("[data-timeline-scroll]");
  const timelineCanvas = root.querySelector("[data-timeline-canvas]");
  const ruler = root.querySelector("[data-movie-ruler]");
  const historyPanel = root.querySelector("[data-movie-history]");
  const historyList = root.querySelector("[data-movie-history-list]");
  const renderPanel = root.querySelector("[data-movie-renders]");
  const renderList = root.querySelector("[data-render-list]");
  const zoomInput = root.querySelector("[data-timeline-zoom]");
  const assetMap = new Map();
  const audioPlayers = new Map();

  const toast = (message, type) => window.studioToast ? window.studioToast(message, type) : console.info(message);
  const csrfToken = () => document.cookie.match(/csrftoken=([^;]+)/)?.[1] || "";
  const uid = () => crypto.randomUUID ? crypto.randomUUID() : `clip-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const clamp = (value, minimum, maximum) => Math.max(minimum, Math.min(maximum, value));
  const clock = ms => {
    const total = Math.max(0, Math.round(ms));
    const minutes = Math.floor(total / 60000);
    const seconds = Math.floor(total % 60000 / 1000);
    const millis = total % 1000;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
  };
  const frameMs = () => 1000 / Math.max(1, Number(root.querySelector("[data-movie-fps]").value) || 25);
  const markDirty = () => {
    if (!canEdit) return;
    dirty = true;
    stateNode.textContent = "Unsaved changes";
  };
  const timelineEnd = () => Math.max(0, ...timeline.tracks.flatMap(track => track.clips.map(clip => clip.start + clip.duration)));
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
      if (track.kind !== kind || track.muted) continue;
      const clip = track.clips.find(item => at >= item.start && at < item.start + item.duration);
      if (clip) return {clip, track};
    }
    return null;
  };
  const numberInput = (value, step, minimum) => {
    const input = document.createElement("input");
    input.type = "number";
    input.value = value;
    input.step = step;
    input.min = minimum;
    return input;
  };
  const field = (text, input) => {
    const label = document.createElement("label");
    label.append(text, input);
    return label;
  };

  function normalizeTimeline() {
    timeline.schemaVersion = 1;
    timeline.tracks = Array.isArray(timeline.tracks) ? timeline.tracks : [];
    timeline.tracks.forEach(track => {
      track.clips = Array.isArray(track.clips) ? track.clips : [];
      track.muted = Boolean(track.muted);
      track.locked = Boolean(track.locked);
      track.clips.forEach(clip => {
        clip.start = Math.max(0, Math.round(Number(clip.start) || 0));
        clip.sourceStart = Math.max(0, Math.round(Number(clip.sourceStart) || 0));
        clip.duration = Math.max(200, Math.round(Number(clip.duration) || 200));
        clip.volume = clamp(Number(clip.volume ?? 1), 0, 2);
      });
    });
  }

  function mediaStatus(asset) {
    if (asset.status === "READY") return `${clock(asset.durationMs || 0)} / Ready`;
    if (asset.status === "FAILED") return "Processing failed";
    return asset.status === "PROCESSING" ? "Creating proxy" : "Queued";
  }

  function renderBin() {
    assetMap.clear();
    assets.forEach(item => assetMap.set(item.id, item));
    bin.replaceChildren();
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
      name.textContent = asset.name;
      name.title = asset.name;
      status.className = `movie-media-status${asset.status === "FAILED" ? " failed" : ""}`;
      status.textContent = mediaStatus(asset);
      if (asset.thumbnailUrl || asset.waveformUrl) {
        const image = document.createElement("img");
        image.src = asset.thumbnailUrl || asset.waveformUrl;
        image.alt = "";
        visual.appendChild(image);
      } else {
        visual.textContent = asset.kind;
      }
      copy.append(name, status);
      if (asset.error) {
        const detail = document.createElement("span");
        detail.textContent = asset.error;
        detail.title = asset.error;
        copy.appendChild(detail);
      }
      if (asset.status === "FAILED" && canEdit) {
        const retry = document.createElement("button");
        retry.type = "button";
        retry.className = "secondary movie-retry";
        retry.textContent = "Retry";
        retry.onclick = async () => {
          retry.disabled = true;
          const response = await fetch(asset.retryUrl, {method: "POST", headers: {"X-CSRFToken": csrfToken()}});
          const data = await response.json();
          if (!response.ok) toast(data.error || "Retry failed", "error");
          await refreshMedia();
        };
        copy.appendChild(retry);
      }
      item.append(visual, copy);
      item.addEventListener("dragstart", event => {
        if (item.draggable) event.dataTransfer.setData("text/asset-id", asset.id);
      });
      item.addEventListener("dblclick", () => {
        if (!canEdit || asset.status !== "READY") return;
        let track = timeline.tracks.find(value => value.kind === asset.kind && !value.locked);
        if (!track) track = addTrack(asset.kind);
        addClip(track, asset.id, timelineEnd());
      });
      bin.appendChild(item);
    });
    if (!assets.length) bin.innerHTML = '<p class="empty">Upload the first video or audio file</p>';
  }

  async function refreshMedia() {
    const response = await fetch(root.dataset.mediaUrl);
    if (!response.ok) return;
    const data = await response.json();
    assets = data.items || [];
    renderBin();
    renderTimeline();
    clearTimeout(mediaPoll);
    if (assets.some(item => ["QUEUED", "PROCESSING"].includes(item.status))) mediaPoll = setTimeout(refreshMedia, 2500);
  }

  const renderDate = value => value ? new Date(value).toLocaleString([], {dateStyle: "medium", timeStyle: "short"}) : "";

  async function renderAction(job, action) {
    const response = await fetch(job.actionUrl, {
      method: "POST",
      headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
      body: JSON.stringify({action}),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) return toast(data.error || `Could not ${action} render`, "error");
    const index = renderJobs.findIndex(item => item.id === data.id);
    if (index >= 0) renderJobs[index] = data;
    renderRenderJobs();
    scheduleRenderPoll();
  }

  function renderRenderJobs() {
    if (!renderList) return;
    renderList.replaceChildren();
    if (!renderJobs.length) {
      const empty = document.createElement("p");
      empty.className = "muted";
      empty.textContent = "No exports yet";
      renderList.append(empty);
      return;
    }
    renderJobs.forEach(job => {
      const row = document.createElement("div");
      const copy = document.createElement("div");
      const title = document.createElement("strong");
      const details = document.createElement("span");
      const progress = document.createElement("div");
      const bar = document.createElement("i");
      const actions = document.createElement("div");
      row.className = "movie-render-row";
      copy.className = "movie-render-copy";
      actions.className = "movie-render-actions";
      progress.className = "movie-render-progress";
      title.textContent = job.title;
      details.textContent = `${job.profileLabel} / ${job.audioProfileLabel} ${job.targetLufs} LUFS / ${job.width}x${job.height} / ${job.statusLabel} / ${renderDate(job.createdAt)}`;
      bar.style.width = `${job.progress || 0}%`;
      progress.append(bar);
      copy.append(title, details);
      if (["QUEUED", "RUNNING"].includes(job.status)) copy.append(progress);
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
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "secondary";
        cancel.textContent = "Cancel";
        cancel.addEventListener("click", () => renderAction(job, "cancel"));
        actions.append(cancel);
      }
      if (job.canRetry && canExport) {
        const retry = document.createElement("button");
        retry.type = "button";
        retry.className = "secondary";
        retry.textContent = "Retry";
        retry.addEventListener("click", () => renderAction(job, "retry"));
        actions.append(retry);
      }
      row.append(copy, actions);
      renderList.append(row);
    });
  }

  async function refreshRenders() {
    if (!root.dataset.renderUrl) return;
    const response = await fetch(root.dataset.renderUrl, {headers: {"X-Requested-With": "XMLHttpRequest"}});
    const data = await response.json().catch(() => ({}));
    if (response.ok) {
      renderJobs = data.items || [];
      renderRenderJobs();
    }
    scheduleRenderPoll();
  }

  function scheduleRenderPoll() {
    clearTimeout(renderPoll);
    if (renderJobs.some(job => ["QUEUED", "RUNNING"].includes(job.status))) {
      renderPoll = setTimeout(refreshRenders, document.hidden ? 5000 : 1800);
    }
  }

  async function queueRender() {
    if (dirty) return toast("Save the timeline before rendering", "error");
    const button = root.querySelector("[data-render-start]");
    button.disabled = true;
    button.textContent = "Queueing";
    const response = await fetch(root.dataset.renderUrl, {
      method: "POST",
      headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
      body: JSON.stringify({
        title: root.querySelector("[data-movie-title]").value,
        profile: root.querySelector("[data-render-profile]").value,
        audioProfile: root.querySelector("[data-render-audio-profile]").value,
        targetLufs: Number(root.querySelector("[data-render-target-lufs]").value),
      }),
    });
    const data = await response.json().catch(() => ({}));
    button.disabled = false;
    button.textContent = "Queue MP4 render";
    if (!response.ok) return toast(data.error || "Could not queue render", "error");
    renderJobs.unshift(data);
    renderRenderJobs();
    toast("MP4 render queued");
    scheduleRenderPoll();
  }

  function timelineCandidates(excludeId) {
    const values = [0, playheadMs];
    timeline.tracks.forEach(track => track.clips.forEach(clip => {
      if (clip.id !== excludeId) values.push(clip.start, clip.start + clip.duration);
    }));
    return values;
  }

  function snapTime(value, excludeId) {
    snapGuide.hidden = true;
    if (!snapping) return Math.max(0, value);
    const threshold = 10 / zoom * 1000;
    let closest = null;
    let distance = threshold + 1;
    timelineCandidates(excludeId).forEach(candidate => {
      const nextDistance = Math.abs(candidate - value);
      if (nextDistance < distance) {
        closest = candidate;
        distance = nextDistance;
      }
    });
    if (closest === null || distance > threshold) return Math.max(0, value);
    snapGuide.style.left = `${closest / 1000 * zoom}px`;
    snapGuide.hidden = false;
    return Math.max(0, closest);
  }

  function hideSnapGuide() {
    snapGuide.hidden = true;
  }

  function setPlayhead(value, sync = true) {
    playheadMs = clamp(Math.round(value), 0, visibleDuration());
    playheadNode.style.left = `${playheadMs / 1000 * zoom}px`;
    playheadLabel.textContent = clock(playheadMs);
    previewTime.textContent = `${clock(playheadMs)} / ${clock(timelineEnd())}`;
    if (sync && !playing) syncPlayers(false);
  }

  function setPlayer(player, active, shouldPlay) {
    if (!active) {
      player.pause();
      player.dataset.clipId = "";
      return;
    }
    const {clip} = active;
    const asset = assetMap.get(clip.assetId);
    if (!asset) return;
    const source = asset.proxyUrl || asset.originalUrl;
    const target = (clip.sourceStart + playheadMs - clip.start) / 1000;
    const seek = () => {
      if (Number.isFinite(player.duration)) player.currentTime = clamp(target, 0, Math.max(0, player.duration - 0.01));
      player.volume = Math.min(1, clip.volume ?? 1);
      if (shouldPlay) player.play().catch(() => {});
    };
    if (player.dataset.assetId !== asset.id) {
      player.dataset.assetId = asset.id;
      player.dataset.clipId = clip.id;
      player.src = source;
      player.load();
      player.addEventListener("loadedmetadata", seek, {once: true});
    } else {
      player.dataset.clipId = clip.id;
      if (Math.abs((player.currentTime || 0) - target) > 0.3 || !shouldPlay) seek();
      else if (shouldPlay && player.paused) player.play().catch(() => {});
    }
  }

  function syncPlayers(shouldPlay) {
    setPlayer(preview, activeClip("VIDEO", playheadMs), shouldPlay);
    const audioTracks = timeline.tracks.filter(track => track.kind === "AUDIO");
    audioTracks.forEach(track => {
      let player = audioPlayers.get(track.id);
      if (!player) {
        player = new Audio();
        player.preload = "metadata";
        audioPlayers.set(track.id, player);
      }
      const clip = !track.muted ? track.clips.find(item => playheadMs >= item.start && playheadMs < item.start + item.duration) : null;
      setPlayer(player, clip ? {clip, track} : null, shouldPlay);
    });
    [...audioPlayers.entries()].forEach(([trackId, player]) => {
      if (!audioTracks.some(track => track.id === trackId)) {
        player.pause();
        audioPlayers.delete(trackId);
      }
    });
  }

  function stopPlayback(reset = false) {
    playing = false;
    cancelAnimationFrame(playbackFrame);
    preview.pause();
    audioPlayers.forEach(player => player.pause());
    root.querySelector("[data-preview-play]").innerHTML = "&#9654;";
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
    syncPlayers(true);
    const playheadX = playheadMs / 1000 * zoom;
    if (playheadX > timelineScroll.scrollLeft + timelineScroll.clientWidth - 36) {
      timelineScroll.scrollLeft = Math.max(0, playheadX - timelineScroll.clientWidth * 0.2);
    }
    playbackFrame = requestAnimationFrame(playbackTick);
  }

  function togglePlayback() {
    if (playing) {
      stopPlayback();
      return;
    }
    if (!timelineEnd()) return toast("Add clips to the timeline first");
    if (playheadMs >= timelineEnd()) setPlayhead(0, false);
    playing = true;
    playbackOrigin = playheadMs;
    playbackStartedAt = performance.now();
    root.querySelector("[data-preview-play]").innerHTML = "&#10074;&#10074;";
    syncPlayers(true);
    playbackFrame = requestAnimationFrame(playbackTick);
  }

  function renderInspector() {
    inspector.replaceChildren();
    const found = findClip(selectedId);
    if (!found) {
      inspector.innerHTML = '<p class="muted">Select a timeline clip</p>';
      return;
    }
    const {clip, track} = found;
    const asset = assetMap.get(clip.assetId);
    const name = document.createElement("input");
    const start = numberInput((clip.start / 1000).toFixed(3), "0.04", "0");
    const sourceStart = numberInput((clip.sourceStart / 1000).toFixed(3), "0.04", "0");
    const duration = numberInput((clip.duration / 1000).toFixed(3), "0.04", "0.2");
    const volume = document.createElement("input");
    name.value = clip.name || asset?.name || "Clip";
    volume.type = "range";
    volume.min = "0";
    volume.max = "2";
    volume.step = "0.05";
    volume.value = clip.volume ?? 1;
    [name, start, sourceStart, duration, volume].forEach(control => control.disabled = !canEdit || track.locked);
    name.oninput = event => {
      clip.name = event.target.value;
      markDirty();
      root.querySelector(`[data-clip-id="${clip.id}"] strong`)?.replaceChildren(clip.name);
    };
    start.onchange = event => {
      clip.start = Math.max(0, Math.round(Number(event.target.value) * 1000));
      markDirty();
      renderTimeline();
    };
    sourceStart.onchange = event => {
      clip.sourceStart = clamp(Math.round(Number(event.target.value) * 1000), 0, assetDuration(clip) - 200);
      clip.duration = Math.min(clip.duration, assetDuration(clip) - clip.sourceStart);
      markDirty();
      renderTimeline();
    };
    duration.onchange = event => {
      clip.duration = clamp(Math.round(Number(event.target.value) * 1000), 200, assetDuration(clip) - clip.sourceStart);
      markDirty();
      renderTimeline();
    };
    volume.oninput = event => {
      clip.volume = Number(event.target.value);
      markDirty();
      syncPlayers(false);
    };
    const readout = document.createElement("div");
    readout.className = "movie-inspector-readout";
    readout.innerHTML = `<span>Source: ${clock(assetDuration(clip))}</span><span>End: ${clock(clip.start + clip.duration)}</span><span>In: ${clock(clip.sourceStart)}</span><span>Out: ${clock(clip.sourceStart + clip.duration)}</span>`;
    const actions = document.createElement("div");
    actions.className = "movie-inspector-actions";
    const split = document.createElement("button");
    split.type = "button";
    split.className = "secondary";
    split.textContent = "Split";
    split.disabled = !canEdit || track.locked;
    split.onclick = splitSelected;
    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "secondary";
    remove.textContent = "Delete";
    remove.disabled = !canEdit || track.locked;
    remove.onclick = deleteSelected;
    actions.append(split, remove);
    inspector.append(field("Name", name), field("Timeline start", start), field("Source in", sourceStart), field("Duration", duration), field("Volume", volume), readout, actions);
  }

  function selectClip(id, movePlayhead = false) {
    selectedId = id;
    const found = findClip(id);
    if (found && movePlayhead && (playheadMs < found.clip.start || playheadMs >= found.clip.start + found.clip.duration)) {
      setPlayhead(found.clip.start);
    } else {
      syncPlayers(false);
    }
    renderInspector();
    root.querySelectorAll(".movie-clip").forEach(node => node.classList.toggle("selected", node.dataset.clipId === id));
  }

  function deleteSelected() {
    const found = findClip(selectedId);
    if (!found || found.track.locked || !canEdit) return;
    found.track.clips = found.track.clips.filter(clip => clip.id !== selectedId);
    selectedId = null;
    markDirty();
    renderTimeline();
    renderInspector();
  }

  function splitSelected() {
    const found = findClip(selectedId);
    if (!found || found.track.locked || !canEdit) return;
    const {clip, track} = found;
    const splitAt = Math.round(playheadMs);
    if (splitAt <= clip.start + 199 || splitAt >= clip.start + clip.duration - 199) {
      return toast("Place the playhead inside the selected clip");
    }
    const leftDuration = splitAt - clip.start;
    const right = {
      ...clip,
      id: uid(),
      name: `${clip.name} (part 2)`,
      start: splitAt,
      sourceStart: clip.sourceStart + leftDuration,
      duration: clip.duration - leftDuration,
    };
    clip.duration = leftDuration;
    track.clips.push(right);
    selectedId = right.id;
    markDirty();
    renderTimeline();
    renderInspector();
  }

  function addTrack(kind) {
    const track = {
      id: uid(),
      name: `${kind === "VIDEO" ? "Video" : "Audio"} ${timeline.tracks.filter(item => item.kind === kind).length + 1}`,
      kind,
      muted: false,
      locked: false,
      clips: [],
    };
    timeline.tracks.push(track);
    markDirty();
    renderTimeline();
    return track;
  }

  function addClip(track, assetId, start) {
    const asset = assetMap.get(assetId);
    if (!asset) return;
    if (asset.status !== "READY" || asset.kind !== track.kind) {
      return toast(asset.kind !== track.kind ? `Drop ${asset.kind.toLowerCase()} onto a matching track` : "Wait until the proxy is ready");
    }
    if (track.locked || !canEdit) return toast("Unlock this track before editing");
    const clip = {
      id: uid(),
      assetId,
      name: asset.name,
      start: snapTime(Math.max(0, start), null),
      sourceStart: 0,
      duration: Math.max(200, asset.durationMs || 8000),
      volume: 1,
    };
    hideSnapGuide();
    track.clips.push(clip);
    selectedId = clip.id;
    markDirty();
    renderTimeline();
    selectClip(clip.id, true);
  }

  function makeTrackHead(track) {
    const head = document.createElement("div");
    const title = document.createElement("strong");
    const mute = document.createElement("button");
    const lock = document.createElement("button");
    const remove = document.createElement("button");
    head.className = `movie-track-head${track.locked ? " locked" : ""}`;
    title.textContent = track.name;
    mute.type = lock.type = remove.type = "button";
    mute.className = lock.className = remove.className = "icon-button secondary";
    mute.textContent = track.muted ? "M" : "S";
    mute.title = track.muted ? "Unmute track" : "Mute track";
    lock.textContent = track.locked ? "L" : "U";
    lock.title = track.locked ? "Unlock track" : "Lock track";
    remove.textContent = "×";
    remove.title = "Delete empty track";
    mute.onclick = () => {
      if (!canEdit) return;
      track.muted = !track.muted;
      markDirty();
      renderTimeline();
      syncPlayers(false);
    };
    lock.onclick = () => {
      if (!canEdit) return;
      track.locked = !track.locked;
      markDirty();
      renderTimeline();
      renderInspector();
    };
    remove.onclick = () => {
      if (!canEdit) return;
      if (track.clips.length) return toast("Remove clips before deleting this track");
      timeline.tracks = timeline.tracks.filter(item => item.id !== track.id);
      markDirty();
      renderTimeline();
    };
    head.append(title, mute, lock, remove);
    return head;
  }

  function beginClipGesture(event, clip, track, mode, node) {
    if (!canEdit || track.locked || event.button !== 0) return;
    event.preventDefault();
    event.stopPropagation();
    selectClip(clip.id);
    const pointerId = event.pointerId;
    const originX = event.clientX;
    const originScroll = timelineScroll.scrollLeft;
    const original = {start: clip.start, sourceStart: clip.sourceStart, duration: clip.duration};
    let targetTrack = track;
    let changed = false;
    node.classList.add("dragging");
    node.setPointerCapture(pointerId);

    const move = next => {
      const scrollBounds = timelineScroll.getBoundingClientRect();
      if (next.clientX > scrollBounds.right - 28) timelineScroll.scrollLeft += 18;
      else if (next.clientX < scrollBounds.left + 28) timelineScroll.scrollLeft = Math.max(0, timelineScroll.scrollLeft - 18);
      const delta = (next.clientX - originX + timelineScroll.scrollLeft - originScroll) / zoom * 1000;
      if (mode === "move") {
        clip.start = Math.round(snapTime(original.start + delta, clip.id));
        const candidateLane = document.elementFromPoint(next.clientX, next.clientY)?.closest(".movie-track-lane");
        const candidateTrack = candidateLane ? timeline.tracks.find(item => item.id === candidateLane.dataset.trackId) : null;
        if (candidateTrack && candidateTrack.kind === track.kind && !candidateTrack.locked) targetTrack = candidateTrack;
        document.querySelectorAll(".movie-track-lane").forEach(lane => lane.classList.toggle("drop-target", lane.dataset.trackId === targetTrack.id));
      } else if (mode === "left") {
        let nextStart = snapTime(original.start + delta, clip.id);
        let shift = nextStart - original.start;
        shift = clamp(shift, -original.sourceStart, original.duration - 200);
        clip.start = original.start + shift;
        clip.sourceStart = original.sourceStart + shift;
        clip.duration = original.duration - shift;
      } else {
        let nextEnd = snapTime(original.start + original.duration + delta, clip.id);
        clip.duration = clamp(nextEnd - original.start, 200, assetDuration(clip) - original.sourceStart);
      }
      changed = true;
      node.style.left = `${clip.start / 1000 * zoom}px`;
      node.style.width = `${Math.max(8, clip.duration / 1000 * zoom)}px`;
      node.querySelector("small").textContent = `${clock(clip.sourceStart)} / ${clock(clip.duration)}`;
    };
    const finish = () => {
      node.removeEventListener("pointermove", move);
      node.removeEventListener("pointerup", finish);
      node.removeEventListener("pointercancel", cancel);
      node.classList.remove("dragging");
      document.querySelectorAll(".movie-track-lane").forEach(lane => lane.classList.remove("drop-target"));
      hideSnapGuide();
      if (changed && mode === "move" && targetTrack !== track) {
        track.clips = track.clips.filter(item => item.id !== clip.id);
        targetTrack.clips.push(clip);
      }
      if (changed) markDirty();
      renderTimeline();
      renderInspector();
    };
    const cancel = () => {
      Object.assign(clip, original);
      changed = false;
      finish();
    };
    node.addEventListener("pointermove", move);
    node.addEventListener("pointerup", finish);
    node.addEventListener("pointercancel", cancel);
  }

  function makeClipNode(clip, track) {
    const node = document.createElement("div");
    const body = document.createElement("div");
    const title = document.createElement("strong");
    const detail = document.createElement("small");
    const left = document.createElement("i");
    const right = document.createElement("i");
    const asset = assetMap.get(clip.assetId);
    node.className = `movie-clip ${track.kind.toLowerCase()}${selectedId === clip.id ? " selected" : ""}`;
    node.dataset.clipId = clip.id;
    node.style.left = `${clip.start / 1000 * zoom}px`;
    node.style.width = `${Math.max(8, clip.duration / 1000 * zoom)}px`;
    body.className = "movie-clip-body";
    title.textContent = clip.name;
    detail.textContent = `${clock(clip.sourceStart)} / ${clock(clip.duration)}`;
    body.append(title, detail);
    if (track.kind === "AUDIO" && asset?.waveformUrl) {
      const wave = document.createElement("img");
      wave.className = "movie-clip-wave";
      wave.src = asset.waveformUrl;
      wave.alt = "";
      node.appendChild(wave);
    }
    left.className = "movie-trim left";
    right.className = "movie-trim right";
    node.append(left, body, right);
    node.addEventListener("click", event => {
      event.stopPropagation();
      selectClip(clip.id, true);
    });
    body.addEventListener("pointerdown", event => beginClipGesture(event, clip, track, "move", node));
    left.addEventListener("pointerdown", event => beginClipGesture(event, clip, track, "left", node));
    right.addEventListener("pointerdown", event => beginClipGesture(event, clip, track, "right", node));
    return node;
  }

  function renderRuler(durationMs) {
    ruler.replaceChildren();
    const major = zoom >= 50 ? 1000 : zoom >= 20 ? 5000 : 10000;
    const minor = major / 5;
    for (let at = 0; at <= durationMs; at += minor) {
      const tick = document.createElement("i");
      tick.className = `movie-ruler-tick${at % major === 0 ? " major" : ""}`;
      tick.style.left = `${at / 1000 * zoom}px`;
      ruler.appendChild(tick);
      if (at % major === 0) {
        const label = document.createElement("span");
        label.className = "movie-ruler-label";
        label.style.left = tick.style.left;
        label.textContent = clock(at).slice(0, 5);
        ruler.appendChild(label);
      }
    }
  }

  function renderTimeline() {
    normalizeTimeline();
    const duration = visibleDuration();
    const width = Math.max(timelineScroll.clientWidth || 600, duration / 1000 * zoom);
    timelineCanvas.style.width = `${width}px`;
    const minor = (zoom >= 50 ? 200 : zoom >= 20 ? 1000 : 2000) / 1000 * zoom;
    tracksNode.style.setProperty("--grid-step", `${minor}px`);
    tracksNode.style.setProperty("--grid-step-minus", `${Math.max(1, minor - 1)}px`);
    headsNode.replaceChildren();
    tracksNode.replaceChildren();
    renderRuler(duration);
    timeline.tracks.forEach(track => {
      headsNode.appendChild(makeTrackHead(track));
      const lane = document.createElement("div");
      lane.className = "movie-track-lane";
      lane.dataset.trackId = track.id;
      lane.addEventListener("pointerdown", event => {
        if (event.target === lane) setPlayhead((event.clientX - lane.getBoundingClientRect().left) / zoom * 1000);
      });
      lane.addEventListener("dragover", event => {
        if (!track.locked && canEdit) {
          event.preventDefault();
          lane.classList.add("drop-target");
        }
      });
      lane.addEventListener("dragleave", () => lane.classList.remove("drop-target"));
      lane.addEventListener("drop", event => {
        event.preventDefault();
        lane.classList.remove("drop-target");
        const start = (event.clientX - lane.getBoundingClientRect().left) / zoom * 1000;
        addClip(track, event.dataTransfer.getData("text/asset-id"), start);
      });
      track.clips.sort((a, b) => a.start - b.start).forEach(clip => lane.appendChild(makeClipNode(clip, track)));
      if (!track.clips.length) lane.innerHTML = '<span class="movie-empty">Drop a clip here</span>';
      tracksNode.appendChild(lane);
    });
    setPlayhead(playheadMs, false);
    root.querySelector("[data-zoom-label]").textContent = `${zoom} px/s`;
    zoomInput.value = zoom;
  }

  function changeZoom(next) {
    const previous = zoom;
    const centerMs = (timelineScroll.scrollLeft + timelineScroll.clientWidth / 2) / previous * 1000;
    zoom = clamp(Math.round(next / 2) * 2, 6, 80);
    localStorage.setItem("studio-movie-zoom", String(zoom));
    renderTimeline();
    requestAnimationFrame(() => {
      timelineScroll.scrollLeft = Math.max(0, centerMs / 1000 * zoom - timelineScroll.clientWidth / 2);
    });
  }

  async function loadHistory() {
    historyList.innerHTML = '<p class="muted">Loading history</p>';
    const response = await fetch(root.dataset.historyUrl);
    const data = await response.json();
    historyList.replaceChildren();
    if (!response.ok) {
      historyList.textContent = data.error || "History could not be loaded";
      return;
    }
    if (!data.items.length) {
      historyList.innerHTML = '<p class="muted">No previous saves yet</p>';
      return;
    }
    data.items.forEach(item => {
      const row = document.createElement("div");
      const copy = document.createElement("div");
      const title = document.createElement("strong");
      const detail = document.createElement("span");
      row.className = "movie-history-row";
      copy.className = "movie-history-copy";
      title.textContent = item.title;
      detail.textContent = `${item.reason} / ${new Date(item.createdAt).toLocaleString()} / ${item.createdBy}`;
      copy.append(title, detail);
      row.appendChild(copy);
      if (data.canRestore) {
        const restore = document.createElement("button");
        restore.type = "button";
        restore.className = "secondary";
        restore.textContent = "Restore";
        restore.onclick = async () => {
          restore.disabled = true;
          const result = await fetch(item.restoreUrl, {method: "POST", headers: {"X-CSRFToken": csrfToken()}});
          const restored = await result.json();
          if (!result.ok) {
            restore.disabled = false;
            return toast(restored.error || "Restore failed", "error");
          }
          timeline = restored.timeline;
          selectedId = null;
          dirty = false;
          stateNode.textContent = "Restored";
          root.querySelector("[data-movie-title]").value = restored.title;
          root.querySelector("[data-movie-ratio]").value = restored.aspectRatio;
          root.querySelector("[data-movie-resolution]").value = restored.resolution;
          root.querySelector("[data-movie-fps]").value = restored.fps;
          setPlayhead(0, false);
          renderTimeline();
          renderInspector();
          toast("Timeline restored");
          await loadHistory();
        };
        row.appendChild(restore);
      }
      historyList.appendChild(row);
    });
  }

  root.querySelector("[data-media-upload]")?.addEventListener("click", () => root.querySelector("[data-media-files]").click());
  root.querySelector("[data-media-files]")?.addEventListener("change", event => {
    const files = [...event.target.files];
    if (!files.length) return;
    const form = new FormData();
    files.slice(0, 10).forEach(file => form.append("files", file));
    const progress = root.querySelector("[data-media-progress]");
    const bar = progress.querySelector("i");
    progress.hidden = false;
    bar.style.width = "0";
    const xhr = new XMLHttpRequest();
    xhr.open("POST", root.dataset.mediaUrl);
    xhr.setRequestHeader("X-CSRFToken", csrfToken());
    xhr.upload.onprogress = update => {
      if (update.lengthComputable) bar.style.width = `${Math.round(update.loaded / update.total * 100)}%`;
    };
    xhr.onload = async () => {
      progress.hidden = true;
      event.target.value = "";
      let data = {};
      try { data = JSON.parse(xhr.responseText); } catch (_) {}
      if (xhr.status >= 400) toast(data.error || "Upload failed", "error");
      else if (data.errors?.length) toast(data.errors.join(" / "), "error");
      await refreshMedia();
    };
    xhr.onerror = () => {
      progress.hidden = true;
      toast("Upload failed", "error");
    };
    xhr.send(form);
  });

  root.querySelectorAll("[data-add-track]").forEach(button => button.addEventListener("click", () => addTrack(button.dataset.addTrack)));
  root.querySelector("[data-auto-cut]").addEventListener("click", () => {
    if (!canEdit) return;
    let track = timeline.tracks.find(item => item.kind === "VIDEO" && !item.locked);
    if (!track) track = addTrack("VIDEO");
    let cursor = 0;
    track.clips = assets.filter(asset => asset.status === "READY" && asset.kind === "VIDEO").map(asset => {
      const duration = Math.max(200, asset.durationMs || 8000);
      const clip = {id: uid(), assetId: asset.id, name: asset.name, start: cursor, sourceStart: 0, duration, volume: 1};
      cursor += duration;
      return clip;
    });
    markDirty();
    renderTimeline();
  });
  root.querySelector("[data-preview-play]").addEventListener("click", togglePlayback);
  root.querySelector("[data-preview-stop]").addEventListener("click", () => stopPlayback(true));
  root.querySelector("[data-timeline-split]").addEventListener("click", splitSelected);
  root.querySelector("[data-timeline-snap]").addEventListener("click", event => {
    snapping = !snapping;
    event.currentTarget.classList.toggle("active", snapping);
    toast(snapping ? "Snapping enabled" : "Snapping disabled");
  });
  ruler.addEventListener("pointerdown", event => {
    stopPlayback();
    const update = next => setPlayhead((next.clientX - ruler.getBoundingClientRect().left) / zoom * 1000);
    update(event);
    ruler.setPointerCapture(event.pointerId);
    ruler.onpointermove = update;
    ruler.onpointerup = () => { ruler.onpointermove = null; ruler.onpointerup = null; };
  });
  timelineScroll.addEventListener("scroll", () => {
    headsNode.style.transform = `translateY(-${timelineScroll.scrollTop}px)`;
  }, {passive: true});
  zoomInput.addEventListener("input", event => changeZoom(Number(event.target.value)));
  root.querySelector("[data-zoom-out]").addEventListener("click", () => changeZoom(zoom - 4));
  root.querySelector("[data-zoom-in]").addEventListener("click", () => changeZoom(zoom + 4));
  root.querySelector("[data-export-timeline]").addEventListener("click", () => {
    const blob = new Blob([JSON.stringify(timeline, null, 2)], {type: "application/json"});
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `${root.querySelector("[data-movie-title]").value || "draft"}-timeline.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  });
  root.querySelector("[data-movie-history-toggle]").addEventListener("click", async () => {
    historyPanel.hidden = !historyPanel.hidden;
    if (!historyPanel.hidden) await loadHistory();
  });
  root.querySelector("[data-movie-history-close]").addEventListener("click", () => { historyPanel.hidden = true; });
  root.querySelector("[data-render-toggle]")?.addEventListener("click", () => {
    renderPanel.hidden = !renderPanel.hidden;
    if (!renderPanel.hidden) refreshRenders();
  });
  root.querySelector("[data-render-close]")?.addEventListener("click", () => { renderPanel.hidden = true; });
  root.querySelector("[data-render-start]")?.addEventListener("click", queueRender);
  root.querySelector("[data-save-movie]")?.addEventListener("click", async () => {
    stateNode.textContent = "Saving";
    const response = await fetch(root.dataset.saveUrl, {
      method: "POST",
      headers: {"Content-Type": "application/json", "X-CSRFToken": csrfToken()},
      body: JSON.stringify({
        title: root.querySelector("[data-movie-title]").value,
        aspectRatio: root.querySelector("[data-movie-ratio]").value,
        resolution: root.querySelector("[data-movie-resolution]").value,
        fps: root.querySelector("[data-movie-fps]").value,
        timeline,
      }),
    });
    const data = await response.json();
    if (!response.ok) {
      stateNode.textContent = data.error || "Save failed";
      return toast(stateNode.textContent, "error");
    }
    dirty = false;
    stateNode.textContent = "Saved";
    toast("Timeline saved");
    if (!historyPanel.hidden) await loadHistory();
  });
  root.querySelectorAll("[data-movie-title],[data-movie-ratio],[data-movie-resolution],[data-movie-fps]").forEach(control => control.addEventListener("change", markDirty));

  document.addEventListener("keydown", event => {
    if (event.target.matches("input,textarea,select") || event.target.isContentEditable) return;
    if (event.code === "Space") {
      event.preventDefault();
      togglePlayback();
    } else if (event.key.toLowerCase() === "s" && !event.ctrlKey && !event.metaKey) {
      event.preventDefault();
      splitSelected();
    } else if ((event.key === "Delete" || event.key === "Backspace") && selectedId) {
      event.preventDefault();
      deleteSelected();
    } else if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
      event.preventDefault();
      const direction = event.key === "ArrowLeft" ? -1 : 1;
      setPlayhead(playheadMs + direction * (event.shiftKey ? 1000 : frameMs()));
    } else if ((event.ctrlKey || event.metaKey) && ["+", "=", "-"].includes(event.key)) {
      event.preventDefault();
      changeZoom(zoom + (event.key === "-" ? -4 : 4));
    }
  });
  addEventListener("beforeunload", event => {
    if (dirty) {
      event.preventDefault();
      event.returnValue = "";
    }
  });
  let resizeTimer;
  addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(renderTimeline, 120);
  });

  root.querySelector("[data-movie-ratio]").value = root.dataset.aspectRatio || "16:9";
  root.querySelector("[data-movie-resolution]").value = root.dataset.resolution || "1920x1080";
  root.querySelector("[data-movie-fps]").value = root.dataset.fps || "25";
  normalizeTimeline();
  renderBin();
  renderTimeline();
  renderInspector();
  renderRenderJobs();
  scheduleRenderPoll();
  setPlayhead(0, false);
  refreshMedia();
})();

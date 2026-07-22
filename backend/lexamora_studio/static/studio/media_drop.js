(() => {
  const marker = document.querySelector("[data-page-media-drop]");
  const uploadUrl = marker?.dataset.mediaUploadUrl;
  const csrf = () => document.cookie.match(/(?:^|; )csrftoken=([^;]+)/)?.[1] || "";
  const fileList = value => [...(value || [])].filter(file => /^(image|video|audio)\//.test(file.type)).slice(0, 10);
  const overlay = document.createElement("div");
  overlay.className = "direct-media-drop-overlay";
  overlay.innerHTML = '<div><b>Drop to upload</b><span>Images, videos, and audio are attached automatically</span><i></i></div>';

  const buildPlaceholders = files => files.map(file => {
    const grid = file.type.startsWith("video/")
      ? document.querySelector("[data-video-grid]")
      : file.type.startsWith("audio/")
        ? document.querySelector("[data-audio-grid]")
        : document.querySelector("[data-media-grid]");
    if (!grid) return null;
    const card = document.createElement("figure");
    card.className = "video-upload-placeholder";
    card.innerHTML = '<div class="video-upload-placeholder-preview"><i class="direct-media-spinner" aria-hidden="true"></i></div><figcaption class="video-upload-placeholder-copy"><strong></strong><small>Uploading <span>0%</span></small><div class="video-upload-progress"><i></i></div></figcaption>';
    card.querySelector("strong").textContent = file.name;
    const add = grid.querySelector(".asset-add-tile");
    if (add) add.insertAdjacentElement("afterend", card); else grid.prepend(card);
    return card;
  }).filter(Boolean);

  const upload = filesValue => {
    const files = fileList(filesValue);
    if (!uploadUrl || !files.length) return;
    const placeholders = buildPlaceholders(files);
    if (!overlay.isConnected) document.body.append(overlay);
    overlay.classList.add("uploading");
    overlay.querySelector("b").textContent = `Uploading ${files.length} file${files.length === 1 ? "" : "s"}`;
    const form = new FormData();
    files.forEach(file => form.append("files", file));
    const xhr = new XMLHttpRequest();
    xhr.open("POST", uploadUrl);
    xhr.setRequestHeader("X-CSRFToken", csrf());
    xhr.upload.onprogress = event => {
      if (event.lengthComputable) {
        const percent = Math.round(event.loaded / event.total * 100);
        overlay.style.setProperty("--upload-progress", `${percent}%`);
        placeholders.forEach(card => {
          card.querySelector(".video-upload-progress i").style.width = `${percent}%`;
          card.querySelector("small span").textContent = `${percent}%`;
        });
      }
    };
    xhr.onload = () => {
      let data = {};
      try { data = JSON.parse(xhr.responseText); } catch (_) {}
      if (xhr.status >= 400 || !data.items?.length) {
        placeholders.forEach(card => card.remove());
        overlay.remove();
        window.studioToast?.(data.error || data.errors?.join(" / ") || "Upload failed", "error");
        return;
      }
      try { sessionStorage.setItem("studio-direct-media-scroll", String(scrollY)); } catch (_) {}
      if (data.errors?.length) window.studioToast?.(data.errors.join(" / "), "error");
      location.reload();
    };
    xhr.onerror = () => { placeholders.forEach(card => card.remove()); overlay.remove(); window.studioToast?.("Upload failed", "error"); };
    xhr.send(form);
  };

  document.querySelectorAll("[data-direct-media-input]").forEach(input => input.addEventListener("change", () => { upload(input.files); input.value = ""; }));
  if (marker && uploadUrl) {
    let depth = 0;
    document.addEventListener("dragenter", event => {
      if (![...event.dataTransfer.types].includes("Files")) return;
      event.preventDefault(); depth += 1;
      if (!overlay.isConnected) document.body.append(overlay);
    });
    document.addEventListener("dragover", event => {
      if (![...event.dataTransfer.types].includes("Files")) return;
      event.preventDefault(); event.dataTransfer.dropEffect = "copy";
    });
    document.addEventListener("dragleave", () => { depth = Math.max(0, depth - 1); if (!depth && !overlay.classList.contains("uploading")) overlay.remove(); });
    document.addEventListener("drop", event => {
      if (!event.dataTransfer.files.length) return;
      event.preventDefault(); depth = 0; upload(event.dataTransfer.files);
    });
  }
  const saved = sessionStorage.getItem("studio-direct-media-scroll");
  if (saved !== null) { sessionStorage.removeItem("studio-direct-media-scroll"); requestAnimationFrame(() => scrollTo(0, Number(saved) || 0)); }
})();

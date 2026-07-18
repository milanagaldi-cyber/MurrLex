import React, { useMemo } from "react";
import { createRoot } from "react-dom/client";
import {
  INITIAL_TIMELINE_DATA,
  LivePlayerProvider,
  TimelineProvider,
  TwickStudio
} from "@twick/studio";
import "@twick/studio/dist/studio.css";
import "./studio-shell.css";

const rootNode = document.getElementById("movie-editor-2-root");
const configNode = document.getElementById("movie-editor-2-config");
const query = new URLSearchParams(window.location.search);
const config = configNode ? JSON.parse(configNode.textContent || "{}") : {
  projectId: query.get("projectId") || "project",
  projectTitle: query.get("projectTitle") || "Lexamora project"
};

function MovieEditor() {
  const contextId = `lexamora-${config.projectId || "project"}`;
  const initialData = useMemo(() => ({ ...INITIAL_TIMELINE_DATA }), []);

  return (
    <div className="lexamora-twick-shell">
      <div className="lexamora-twick-note">
        <span className="lexamora-ready-dot" />
        <span>Professional multi-track editor</span>
        <span className="lexamora-twick-project">{config.projectTitle}</span>
      </div>
      <LivePlayerProvider>
        <TimelineProvider contextId={contextId} initialData={initialData}>
          <TwickStudio
            studioConfig={{
              videoProps: { width: 1920, height: 1080 },
              editor: { contextId }
            }}
          />
        </TimelineProvider>
      </LivePlayerProvider>
    </div>
  );
}

if (rootNode) createRoot(rootNode).render(<MovieEditor />);

# Lexamora Movie Editor 2

This package builds the authenticated `Movie Editor 2` screen from Twick Studio.
The production server does not run Node.js: Vite emits static files into
`backend/lexamora_studio/static/studio/movie-editor-2`, and Django serves the
editor inside the existing project page.

Build from this directory:

```powershell
npm install
npm run build
```

The first integration intentionally keeps Twick media in its browser-side asset
library. Server asset synchronization and server-side rendering can be added
after the editor workflow is validated with production users.

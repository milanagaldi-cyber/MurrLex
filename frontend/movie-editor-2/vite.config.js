import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "/static/studio/movie-editor-2/",
  build: {
    outDir: "../../backend/lexamora_studio/static/studio/movie-editor-2",
    emptyOutDir: true,
    sourcemap: false,
    rollupOptions: {
      output: {
        entryFileNames: "movie-editor-2.js",
        chunkFileNames: "movie-editor-2-[name].js",
        assetFileNames: "movie-editor-2[extname]"
      }
    }
  }
});

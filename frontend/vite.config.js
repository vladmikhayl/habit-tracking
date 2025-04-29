import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Сервер для фронтенда запускается на порту 3000
    port: 3000,
    // Прокси, чтобы в путях можно было писать /api, и оно менялось на http://localhost:8080/api/v1
    proxy: {
      "/api": {
        target: "http://localhost:8080/api/v1",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});

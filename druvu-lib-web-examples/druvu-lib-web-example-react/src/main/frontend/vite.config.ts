import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  build: {
    // Straight onto the jar's classpath, into the webapp directory druvu-lib-web serves static files from;
    // `mvn clean` removes it with the rest of target.
    outDir: "../../../target/classes/webapp",
    // Vite empties a directory outside this folder only when told to.
    emptyOutDir: true,
  },
});

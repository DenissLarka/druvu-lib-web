import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";

// index.html holds the one element React renders into.
const root = document.getElementById("root");
if (root === null) {
  throw new Error("index.html has no element with id 'root'");
}

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

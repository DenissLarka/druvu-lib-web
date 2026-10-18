import { useState } from "react";

/** The first screen: the bundle loads and React renders, until the app has a server to talk to. */
export function App() {
  const [clicks, setClicks] = useState(0);

  return (
    <main>
      <h1>druvu-lib-web + React</h1>
      <p>Built by Vite inside the Maven build.</p>
      <button type="button" onClick={() => setClicks(clicks + 1)}>
        Clicks: {clicks}
      </button>
    </main>
  );
}

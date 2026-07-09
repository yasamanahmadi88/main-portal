import { useEffect, useState } from "react";

type PortalInfo = {
  name: string;
  version: string;
  status: string;
  phase: string;
  timestamp: string;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";

async function fetchPortalInfo(): Promise<PortalInfo> {
  const response = await fetch(`${API_BASE}/api/info`);
  if (!response.ok) {
    throw new Error(`API responded with ${response.status}`);
  }
  return response.json() as Promise<PortalInfo>;
}

export default function App() {
  const [info, setInfo] = useState<PortalInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    fetchPortalInfo()
      .then((data) => {
        if (!cancelled) {
          setInfo(data);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Unable to reach API");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="shell">
      <header className="hero">
        <p className="brand">Main Portal</p>
        <h1>Foundation online</h1>
        <p className="lede">
          Backend and frontend scaffolds are wired for the first delivery phase.
        </p>
      </header>

      <section className="status" aria-live="polite">
        <h2>API status</h2>
        {loading && <p>Checking `/api/info`…</p>}
        {!loading && error && (
          <p className="error">
            API unreachable ({error}). Start the backend on port 8080, or use the
            Vite proxy during local development.
          </p>
        )}
        {!loading && info && (
          <dl>
            <div>
              <dt>Name</dt>
              <dd>{info.name}</dd>
            </div>
            <div>
              <dt>Version</dt>
              <dd>{info.version}</dd>
            </div>
            <div>
              <dt>Phase</dt>
              <dd>{info.phase}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd className="ok">{info.status}</dd>
            </div>
          </dl>
        )}
      </section>
    </div>
  );
}

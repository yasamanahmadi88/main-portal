import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("renders brand and API status when healthy", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          name: "main-portal",
          version: "0.1.0",
          status: "UP",
          phase: "portal-foundation",
          timestamp: "2026-07-09T00:00:00Z",
        }),
      }),
    );

    render(<App />);

    expect(screen.getByText("Main Portal")).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText("portal-foundation")).toBeInTheDocument();
    });
    expect(screen.getByText("UP")).toBeInTheDocument();
  });
});

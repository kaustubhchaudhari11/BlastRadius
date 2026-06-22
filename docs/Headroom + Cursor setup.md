# Headroom + Cursor setup

[Headroom](https://github.com/chopratejas/headroom) compresses large tool outputs, logs, and files **before** they hit the LLM — typically 60–95% fewer tokens with the same answers. Useful when Cursor reads big diffs, logs, or many files during Blast Radius development.

## What Headroom does for this project

| Without Headroom | With Headroom |
|------------------|---------------|
| Full `pom.xml`, stack traces, OSV JSON in context | Compressed summary; full text retrievable by hash |
| Context window fills fast in Phase 3–5 | More room for agent prompts and your code |

Headroom is **optional** for Phase 1. It pays off most in **Phase 3+** (OSV responses) and **Phase 5** (multi-agent pipeline).

---

## Install (Windows)

Headroom requires **Python 3.10+** and on Windows may need **Rust** to build native wheels.

### Step 1 — Rust (if pip install fails)

1. Install Rust: https://rustup.rs/ (run `rustup-init.exe`, default options)
2. Restart terminal
3. Verify: `cargo --version`

### Step 2 — Install Headroom

```powershell
pip install "headroom-ai[mcp]"
headroom --help
```

If install still fails, try WSL2 Ubuntu and install there, or use Headroom only in Phase 5 via the Python library inside the Spring app (not MCP).

### Step 3 — Cursor MCP config

This repo includes `.cursor/mcp.json`. After Headroom installs:

1. **Cursor → Settings → Tools & MCP** (or `Ctrl+Shift+J`)
2. Confirm **headroom** appears under MCP servers (green = connected)
3. If not: **Restart Cursor**

Manual config (project-level, already in repo):

```json
{
  "mcpServers": {
    "headroom": {
      "command": "headroom",
      "args": ["mcp", "serve"]
    }
  }
}
```

If `headroom` is not on PATH, use full path to the executable, e.g.:

```json
"command": "C:\\Users\\kaust\\AppData\\Local\\Programs\\Python\\Python312\\Scripts\\headroom.exe"
```

### Step 4 — Verify in chat

Ask Cursor Agent: *"Use headroom_compress on this log snippet…"* — if MCP is connected, Agent can call `headroom_compress`, `headroom_retrieve`, and `headroom_stats`.

---

## Modes (pick one)

| Mode | When to use |
|------|-------------|
| **MCP server** (`headroom mcp serve`) | Cursor compresses on demand — **best for this workflow** |
| **Proxy** (`headroom proxy --port 8787`) | Automatic compression of all API traffic |
| **Library** (`from headroom import compress`) | Inside Blast Radius backend (Phase 5 LLM calls) |

For Cursor coding sessions: **MCP** is enough. For production LLM calls in your Spring app (Phase 5), use the **Java RestClient** to your LLM provider; optionally add Headroom library in a sidecar Python service later.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Failed to build headroom-ai` | Install Rust via rustup.rs, retry pip |
| MCP server red in Cursor | Check `headroom` on PATH; use full `.exe` path in mcp.json |
| Tools not used by Agent | Enable MCP for Agent in Cursor settings; restart |

---

## Links

- Repo: https://github.com/chopratejas/headroom
- MCP docs: https://github.com/chopratejas/headroom/blob/main/docs/mcp.md

# How to use this vault

This `docs/` folder is an **Obsidian-ready knowledge base** for Blast Radius. You can also import it into Notion.

---

## Option A — Obsidian (recommended for graphs)

1. Open **Obsidian**
2. **Open folder as vault** → select `C:\Users\kaust\projects\blastradius\docs`
3. Install community plugin **Mermaid** (Settings → Community plugins → Browse → "Mermaid") — or use built-in Mermaid in reading view
4. Start at [[Home]] — click wiki-links to navigate
5. **Graph view** (left sidebar) shows how notes connect — great for seeing phase dependencies

### Tips

- Pin [[Roadmap]] and [[Architecture]] in the sidebar
- Use **Daily notes** or a `Progress.md` note to log what you finished each session
- Commit doc updates with `docs:` commits when phases complete

---

## Option B — Notion

### Quick import (markdown)

1. In Notion: **Import** → **Markdown**
2. Select all `.md` files from `docs/` folder
3. Notion creates pages — manually link them or use a "Home" page with links

### Manual structure (cleaner)

Create a Notion page **Blast Radius** with subpages:

| Notion page | Copy from |
|-------------|-----------|
| Home | `Home.md` |
| Roadmap | `Roadmap.md` |
| Architecture | `Architecture.md` |
| Data Model | `Data Model.md` |
| Git Workflow | `Git Workflow.md` |
| Phase 1 | `Phase 1 — Persistence.md` |

For **Mermaid diagrams**: Notion doesn't render Mermaid natively. Options:
- Paste diagram into [mermaid.live](https://mermaid.live) → export PNG → embed in Notion
- Use Notion's `/code` block and keep Mermaid source for reference
- Use Obsidian for diagrams, Notion for task tracking

### Notion database (optional)

Create a **Phases** database with columns:
- Phase (number)
- Name
- Status (Not started / In progress / Done)
- Branch
- Depends on (relation)
- Tag (v0.1.0 etc.)

---

## Option C — Both (best of both worlds)

| Tool | Use for |
|------|---------|
| **Obsidian** (`docs/` vault) | Architecture, mermaid graphs, deep technical notes |
| **Notion** | Weekly calendar, interview prep checklist, demo script |
| **GitHub** | Source of truth for code + PR history |

Keep `docs/` in the repo so roadmap travels with the code.

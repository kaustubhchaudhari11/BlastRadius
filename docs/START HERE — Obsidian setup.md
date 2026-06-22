# START HERE — Obsidian setup

If you just opened this folder in Obsidian, you're in the right place.

## First 60 seconds

1. Click **Dashboard** in the left file list
2. Press **Ctrl+E** to toggle Reading view (renders links and diagrams)
3. Click **[[Roadmap]]** from Dashboard
4. Open **Graph view** (left sidebar icon — dots connected by lines)

That's your project command center.

## If Obsidian looks empty or confusing

You may have opened the **wrong folder**. The vault must be this exact path:

```
C:\Users\kaust\projects\blastradius\docs
```

**Not** the parent `blastradius` folder (that shows Java code, `pom.xml`, `target/` — wrong for Obsidian).

---

## How to open correctly (Windows, step by step)

### If Obsidian is closed

1. Open **Obsidian** from Start menu
2. On the welcome screen, look at the **bottom-left** for **"Open folder as vault"**
   - If you don't see it: click **"Manage vaults"** (folder icon, bottom-left) → **Open**
3. In the file picker, paste this path in the address bar:
   ```
   C:\Users\kaust\projects\blastradius\docs
   ```
4. Click **Select Folder**
5. If asked "Trust authors?" → **Trust** (these are your own markdown files)
6. You should see 9 `.md` files in the left sidebar, starting with **START HERE**

### If Obsidian already has another vault open

1. Click the **vault name** (bottom-left corner, e.g. "My Vault")
2. Click **"Open another vault"**
3. Click **"Open folder as vault"**
4. Select `C:\Users\kaust\projects\blastradius\docs`

### If you accidentally opened the Java project root

Symptoms: you see `pom.xml`, `src/`, `target/`, `.git/` in the file tree.

Fix:
1. Bottom-left → vault name → **Manage vaults**
2. Remove that vault (optional)
3. **Open folder as vault** → pick **`docs`** subfolder only

---

## Maximum benefit workflow

| When | Do this in Obsidian |
|------|---------------------|
| **Start of session** | Open **Dashboard** → check Today's boxes |
| **Planning** | **Roadmap** → read dependency graph (Reading view) |
| **While coding** | Split screen: IntelliJ/Cursor + Obsidian **Phase 1** note |
| **After a commit** | Check off one item on **Dashboard** |
| **End of session** | Add one line to **Session log** on Dashboard |
| **Weekly review** | **Graph view** → see what's connected; **Roadmap** status table |

### Recommended plugins (optional, 2 minutes)

1. **Settings** (gear, bottom-left) → **Community plugins** → **Turn on**
2. **Browse** → install **Tasks**
3. Create a note **All Open Tasks** with:
   ```
   ```tasks
   not done
   ```
   ```
   (Obsidian aggregates every `- [ ]` checkbox across all notes)

### Reading view vs Edit mode

- **Edit mode** (pencil icon): see raw markdown, edit checkboxes
- **Reading view** (book icon or **Ctrl+E**): see rendered links, tables, Mermaid diagrams

Use **Reading view** for Roadmap and Architecture diagrams.

---

## Next step for the project

PostgreSQL 17 is already installed on your machine. See [[Phase 1 — Persistence]] for local DB setup (no Docker required during development).

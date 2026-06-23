# Data Model

## ER diagram

```mermaid
erDiagram
    PROJECT ||--o{ DEPENDENCY : has
    PROJECT ||--o{ FINDING : has
    DEPENDENCY ||--o{ USAGE_SITE : has
    DEPENDENCY ||--o{ FINDING : has
    ADVISORY ||--o{ FINDING : matches

    PROJECT {
        bigint id PK
        string name
        string source_path
        string repo_url
        timestamp created_at
    }

    DEPENDENCY {
        bigint id PK
        bigint project_id FK
        string ecosystem
        string group_or_pkg
        string artifact_or_name
        string current_version
        string latest_version
    }

    USAGE_SITE {
        bigint id PK
        bigint dependency_id FK
        string file_path
        int line_number
        string symbol
        string snippet
    }

    ADVISORY {
        bigint id PK
        string source
        string external_id
        string ecosystem
        string pkg_name
        text affected_versions
        string summary
        string severity
        timestamp published_at
    }

    FINDING {
        bigint id PK
        bigint project_id FK
        bigint dependency_id FK
        bigint advisory_id FK
        float relevance_score
        string triage_status
        text migration_suggestion
        boolean eval_passed
        timestamp created_at
    }
```

## Key design notes

- **`ecosystem`** on `Dependency` and `Advisory` is the multi-language seam (`maven`, `pypi`).
- **`Advisory`** is global — not tied to a project. Matched to deps by `ecosystem + package name`.
- **`Finding`** is the product: the join of project + dependency + advisory + agent output.

CREATE TABLE project (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    source_path     VARCHAR(1024) NOT NULL,
    repo_url        VARCHAR(1024),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE dependency (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    ecosystem           VARCHAR(32) NOT NULL,
    group_or_pkg        VARCHAR(512) NOT NULL,
    artifact_or_name    VARCHAR(512) NOT NULL,
    current_version     VARCHAR(128) NOT NULL,
    latest_version      VARCHAR(128)
);

CREATE INDEX idx_dependency_project_id ON dependency(project_id);
CREATE INDEX idx_dependency_ecosystem_pkg ON dependency(ecosystem, group_or_pkg, artifact_or_name);

CREATE TABLE usage_site (
    id              BIGSERIAL PRIMARY KEY,
    dependency_id   BIGINT NOT NULL REFERENCES dependency(id) ON DELETE CASCADE,
    file_path       VARCHAR(1024) NOT NULL,
    line_number     INTEGER NOT NULL,
    symbol          VARCHAR(512) NOT NULL,
    snippet         TEXT
);

CREATE INDEX idx_usage_site_dependency_id ON usage_site(dependency_id);

CREATE TABLE advisory (
    id                  BIGSERIAL PRIMARY KEY,
    source              VARCHAR(64) NOT NULL,
    external_id         VARCHAR(255) NOT NULL UNIQUE,
    ecosystem           VARCHAR(32) NOT NULL,
    pkg_name            VARCHAR(512) NOT NULL,
    affected_versions   TEXT,
    summary             TEXT,
    severity            VARCHAR(64),
    published_at        TIMESTAMPTZ
);

CREATE INDEX idx_advisory_ecosystem_pkg ON advisory(ecosystem, pkg_name);

CREATE TABLE finding (
    id                      BIGSERIAL PRIMARY KEY,
    project_id              BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    dependency_id           BIGINT NOT NULL REFERENCES dependency(id) ON DELETE CASCADE,
    advisory_id             BIGINT NOT NULL REFERENCES advisory(id) ON DELETE CASCADE,
    relevance_score         DOUBLE PRECISION,
    triage_status           VARCHAR(64),
    migration_suggestion    TEXT,
    eval_passed             BOOLEAN,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_finding_project_id ON finding(project_id);
CREATE INDEX idx_finding_dependency_id ON finding(dependency_id);
CREATE INDEX idx_finding_advisory_id ON finding(advisory_id);

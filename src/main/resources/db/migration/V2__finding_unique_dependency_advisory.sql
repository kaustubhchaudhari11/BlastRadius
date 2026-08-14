-- A finding is the statement "this advisory applies to this dependency", so the pair must be
-- unique. Refreshing advisories is expected to be repeatable, and without this constraint a
-- second refresh would silently accumulate duplicate findings for the same pair.
ALTER TABLE finding
    ADD CONSTRAINT uq_finding_dependency_advisory UNIQUE (dependency_id, advisory_id);

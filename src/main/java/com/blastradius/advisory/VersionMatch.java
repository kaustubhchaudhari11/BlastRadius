package com.blastradius.advisory;

/**
 * Whether a project's current version falls inside an advisory's vulnerable range.
 *
 * <p>Deliberately tri-state rather than boolean: {@link #UNKNOWN} is what keeps the product
 * honest. A dependency whose version could not be resolved (see {@code MavenVersionResolver}
 * and the {@code "unspecified"} sentinel) must be surfaced for manual review, never silently
 * treated as affected or unaffected. Phase 5 maps these onto triage statuses.
 */
public enum VersionMatch {

	/** Current version is inside a vulnerable range — a real finding. */
	AFFECTED,

	/** Current version is outside every vulnerable range — filtered out as noise. */
	NOT_AFFECTED,

	/** Version unresolved or unparseable — cannot decide, needs human review. */
	UNKNOWN
}

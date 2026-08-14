package com.blastradius.model;

/**
 * Lifecycle state of a {@link Finding}.
 *
 * <p>Phase 3 sets the initial value from version-range matching. Later phases refine it:
 * usage analysis (P4) can demote an {@link #AFFECTED} finding whose vulnerable symbol is never
 * called, and triage (P5) can move findings to {@link #DISMISSED}.
 *
 * <p>Stored as a lowercase code rather than {@code name()} so the column stays readable in SQL
 * and is not coupled to Java identifier changes.
 */
public enum TriageStatus {

	/** Current version falls inside the advisory's vulnerable range. */
	AFFECTED("affected"),

	/** Current version is outside every vulnerable range — kept for auditability. */
	NOT_AFFECTED("not_affected"),

	/** Version could not be resolved, so no automated verdict is trustworthy. */
	NEEDS_REVIEW("needs_review"),

	/** A human decided this finding does not apply. */
	DISMISSED("dismissed");

	private final String code;

	TriageStatus(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}
}

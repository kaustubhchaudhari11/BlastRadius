package com.blastradius.advisory;

import java.util.List;

/**
 * Outcome of an advisory refresh.
 *
 * @param projectId           project that was refreshed
 * @param dependenciesQueried how many distinct packages were sent to OSV
 * @param advisoriesFound     total advisories returned (including already-known ones)
 * @param advisoriesCreated   new {@code advisory} rows inserted
 * @param findingsCreated     new {@code finding} rows linking a dependency to an advisory
 * @param affected            advisories whose vulnerable range contains the current version
 * @param notAffected         advisories filtered out as noise — the product's core value
 * @param unknown             advisories we could not decide on (unresolved version) → manual review
 * @param errors              per-package failures; refresh continues rather than aborting
 */
public record AdvisoryRefreshResult(
		Long projectId,
		int dependenciesQueried,
		int advisoriesFound,
		int advisoriesCreated,
		int findingsCreated,
		int affected,
		int notAffected,
		int unknown,
		List<String> errors) {
}

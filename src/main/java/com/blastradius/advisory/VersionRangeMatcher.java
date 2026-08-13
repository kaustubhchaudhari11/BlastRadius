package com.blastradius.advisory;

import com.blastradius.advisory.osv.OsvModels.Affected;
import com.blastradius.advisory.osv.OsvModels.Event;
import com.blastradius.advisory.osv.OsvModels.Range;
import java.util.List;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.stereotype.Component;

/**
 * Decides whether a concrete version is inside an advisory's vulnerable range.
 *
 * <p>This is the noise filter at the heart of the product: without it every advisory for a
 * package would be reported regardless of the version actually in use.
 *
 * <p>Version ordering uses Maven's own {@link ComparableVersion} rather than semver parsing,
 * because real Maven coordinates are not semver — {@code 2.0.0.RELEASE}, {@code 1.0-RC1},
 * and {@code 3.12.0} all have to order the way Maven itself would.
 *
 * <p>OSV models a range as an ordered event list: {@code introduced} opens a vulnerable
 * window, {@code fixed} closes it (exclusive), {@code last_affected} closes it (inclusive).
 * A version is affected if it lands inside any open window, or is listed explicitly in the
 * {@code versions} array.
 */
@Component
public class VersionRangeMatcher {

	/** Sentinel written by MavenVersionResolver when a version could not be resolved. */
	static final String UNSPECIFIED = "unspecified";

	private static final String TYPE_GIT = "GIT";

	/**
	 * @param currentVersion the project's installed version, possibly {@code "unspecified"}
	 * @param affected       the advisory's affected entries
	 * @return tri-state verdict; {@link VersionMatch#UNKNOWN} when no decision is possible
	 */
	public VersionMatch match(String currentVersion, List<Affected> affected) {
		if (isUnresolved(currentVersion)) {
			return VersionMatch.UNKNOWN;
		}
		if (affected == null || affected.isEmpty()) {
			// OSV returned the advisory but told us nothing about affected versions.
			return VersionMatch.UNKNOWN;
		}

		ComparableVersion current = new ComparableVersion(currentVersion);
		boolean sawUsableRange = false;

		for (Affected entry : affected) {
			if (entry == null) {
				continue;
			}
			if (entry.versionsOrEmpty().contains(currentVersion)) {
				return VersionMatch.AFFECTED;
			}
			for (Range range : entry.rangesOrEmpty()) {
				if (range == null || TYPE_GIT.equalsIgnoreCase(range.type())) {
					continue; // commit ranges say nothing about released versions
				}
				if (range.eventsOrEmpty().isEmpty()) {
					continue;
				}
				sawUsableRange = true;
				if (withinRange(current, range)) {
					return VersionMatch.AFFECTED;
				}
			}
			if (!entry.versionsOrEmpty().isEmpty()) {
				sawUsableRange = true;
			}
		}

		// Only claim "not affected" when there was something concrete to compare against.
		return sawUsableRange ? VersionMatch.NOT_AFFECTED : VersionMatch.UNKNOWN;
	}

	/**
	 * Walks the event list in order, tracking whether the window is currently open. OSV
	 * guarantees events are sorted, so a single pass is enough.
	 */
	private boolean withinRange(ComparableVersion current, Range range) {
		boolean introduced = false;

		for (Event event : range.eventsOrEmpty()) {
			if (event == null) {
				continue;
			}
			if (event.introduced() != null) {
				// "0" means "from the beginning of time".
				introduced = "0".equals(event.introduced())
						|| current.compareTo(new ComparableVersion(event.introduced())) >= 0;
			}
			else if (event.fixed() != null && introduced) {
				if (current.compareTo(new ComparableVersion(event.fixed())) < 0) {
					return true;
				}
				introduced = false; // window closed at/after the fix
			}
			else if (event.lastAffected() != null && introduced) {
				if (current.compareTo(new ComparableVersion(event.lastAffected())) <= 0) {
					return true;
				}
				introduced = false;
			}
			else if (event.limit() != null && introduced) {
				if (current.compareTo(new ComparableVersion(event.limit())) < 0) {
					return true;
				}
				introduced = false;
			}
		}

		// An "introduced" with no closing event means everything from there on is affected.
		return introduced;
	}

	static boolean isUnresolved(String version) {
		return version == null || version.isBlank() || UNSPECIFIED.equalsIgnoreCase(version);
	}
}

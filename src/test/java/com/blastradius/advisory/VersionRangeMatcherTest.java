package com.blastradius.advisory;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastradius.advisory.osv.OsvModels.Affected;
import com.blastradius.advisory.osv.OsvModels.Event;
import com.blastradius.advisory.osv.OsvModels.OsvPackage;
import com.blastradius.advisory.osv.OsvModels.Range;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The highest-value tests in the repo: a wrong verdict here either hides a real
 * vulnerability or floods the user with noise.
 */
class VersionRangeMatcherTest {

	private final VersionRangeMatcher matcher = new VersionRangeMatcher();

	private static Affected introducedFixed(String introduced, String fixed) {
		return new Affected(
				new OsvPackage("com.google.guava:guava", "Maven"),
				List.of(new Range("ECOSYSTEM", List.of(
						new Event(introduced, null, null, null),
						new Event(null, fixed, null, null)))),
				List.of());
	}

	@ParameterizedTest
	@CsvSource({
			// version, introduced, fixed, expectAffected
			"31.0.0,  0,      32.0.0, true",   // inside window
			"32.0.0,  0,      32.0.0, false",  // fixed is exclusive
			"32.1.0,  0,      32.0.0, false",  // after the fix
			"1.0.0,   1.0.0,  2.0.0,  true",   // introduced is inclusive
			"0.9.0,   1.0.0,  2.0.0,  false",  // before introduction
	})
	void appliesIntroducedInclusiveAndFixedExclusive(
			String version, String introduced, String fixed, boolean expectAffected) {
		VersionMatch result = matcher.match(version, List.of(introducedFixed(introduced, fixed)));
		assertThat(result).isEqualTo(expectAffected ? VersionMatch.AFFECTED : VersionMatch.NOT_AFFECTED);
	}

	@Test
	void treatsUnspecifiedVersionAsUnknown() {
		assertThat(matcher.match("unspecified", List.of(introducedFixed("0", "32.0.0"))))
				.isEqualTo(VersionMatch.UNKNOWN);
	}

	@Test
	void treatsBlankVersionAsUnknown() {
		assertThat(matcher.match("", List.of(introducedFixed("0", "32.0.0"))))
				.isEqualTo(VersionMatch.UNKNOWN);
	}

	@Test
	void returnsUnknownWhenAdvisoryHasNoAffectedInfo() {
		assertThat(matcher.match("1.0.0", List.of())).isEqualTo(VersionMatch.UNKNOWN);
	}

	@Test
	void matchesExplicitVersionList() {
		Affected affected = new Affected(
				new OsvPackage("org.example:lib", "Maven"), List.of(), List.of("1.2.3", "1.2.4"));
		assertThat(matcher.match("1.2.4", List.of(affected))).isEqualTo(VersionMatch.AFFECTED);
		assertThat(matcher.match("1.2.5", List.of(affected))).isEqualTo(VersionMatch.NOT_AFFECTED);
	}

	@Test
	void openEndedIntroducedAffectsEverythingAfter() {
		Affected affected = new Affected(
				new OsvPackage("org.example:lib", "Maven"),
				List.of(new Range("ECOSYSTEM", List.of(new Event("2.0.0", null, null, null)))),
				List.of());
		assertThat(matcher.match("99.0.0", List.of(affected))).isEqualTo(VersionMatch.AFFECTED);
		assertThat(matcher.match("1.9.9", List.of(affected))).isEqualTo(VersionMatch.NOT_AFFECTED);
	}

	@Test
	void honoursLastAffectedAsInclusive() {
		Affected affected = new Affected(
				new OsvPackage("org.example:lib", "Maven"),
				List.of(new Range("ECOSYSTEM", List.of(
						new Event("1.0.0", null, null, null),
						new Event(null, null, "1.5.0", null)))),
				List.of());
		assertThat(matcher.match("1.5.0", List.of(affected))).isEqualTo(VersionMatch.AFFECTED);
		assertThat(matcher.match("1.5.1", List.of(affected))).isEqualTo(VersionMatch.NOT_AFFECTED);
	}

	@Test
	void ignoresGitRangesWhichSayNothingAboutReleases() {
		Affected gitOnly = new Affected(
				new OsvPackage("org.example:lib", "Maven"),
				List.of(new Range("GIT", List.of(new Event("abc123", null, null, null)))),
				List.of());
		assertThat(matcher.match("1.0.0", List.of(gitOnly))).isEqualTo(VersionMatch.UNKNOWN);
	}

	/** Maven versions are not semver; these would break a naive string/semver comparison. */
	@Test
	void ordersNonSemverMavenVersionsCorrectly() {
		Affected affected = introducedFixed("0", "2.0.0.RELEASE");
		assertThat(matcher.match("1.9.9.RELEASE", List.of(affected))).isEqualTo(VersionMatch.AFFECTED);
		assertThat(matcher.match("2.0.0.RELEASE", List.of(affected))).isEqualTo(VersionMatch.NOT_AFFECTED);
	}

	@Test
	void treatsReleaseCandidateAsEarlierThanFinal() {
		// 1.0-RC1 sorts before 1.0 in Maven ordering, so a fix at 1.0 still leaves RC1 affected.
		assertThat(matcher.match("1.0-RC1", List.of(introducedFixed("0", "1.0"))))
				.isEqualTo(VersionMatch.AFFECTED);
	}

	@Test
	void multipleRangesAreOredTogether() {
		Affected affected = new Affected(
				new OsvPackage("org.example:lib", "Maven"),
				List.of(
						new Range("ECOSYSTEM", List.of(
								new Event("1.0.0", null, null, null), new Event(null, "1.1.0", null, null))),
						new Range("ECOSYSTEM", List.of(
								new Event("2.0.0", null, null, null), new Event(null, "2.1.0", null, null)))),
				List.of());
		assertThat(matcher.match("2.0.5", List.of(affected))).isEqualTo(VersionMatch.AFFECTED);
		assertThat(matcher.match("1.5.0", List.of(affected))).isEqualTo(VersionMatch.NOT_AFFECTED);
	}
}

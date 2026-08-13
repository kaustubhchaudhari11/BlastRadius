package com.blastradius.advisory.osv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Wire model for the subset of the OSV.dev schema we consume.
 *
 * <p>Only the fields Blast Radius needs are modelled; unknown properties are ignored so
 * OSV can evolve without breaking ingestion. Schema: <a href="https://ossf.github.io/osv-schema/">osv-schema</a>.
 *
 * <p>Note: OSV names a field {@code package}, which is a Java keyword, so it is mapped to
 * {@code pkg} via {@link JsonProperty}.
 */
public final class OsvModels {

	private OsvModels() {
	}

	/** Request body for {@code POST /v1/query}. */
	public record Query(@JsonProperty("package") OsvPackage pkg, String version) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OsvPackage(String name, String ecosystem) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record QueryResponse(List<Vulnerability> vulns) {
		public List<Vulnerability> vulnsOrEmpty() {
			return vulns == null ? List.of() : vulns;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Vulnerability(
			String id,
			String summary,
			String details,
			Instant published,
			List<Severity> severity,
			List<Affected> affected,
			List<String> aliases) {

		public List<Affected> affectedOrEmpty() {
			return affected == null ? List.of() : affected;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Severity(String type, String score) {
	}

	/** One affected package plus the version ranges that are vulnerable. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Affected(
			@JsonProperty("package") OsvPackage pkg,
			List<Range> ranges,
			List<String> versions) {

		public List<Range> rangesOrEmpty() {
			return ranges == null ? List.of() : ranges;
		}

		public List<String> versionsOrEmpty() {
			return versions == null ? List.of() : versions;
		}
	}

	/**
	 * A range expressed as ordered events. {@code type} is {@code SEMVER}, {@code ECOSYSTEM},
	 * or {@code GIT}; we only interpret the first two.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Range(String type, List<Event> events) {
		public List<Event> eventsOrEmpty() {
			return events == null ? List.of() : events;
		}
	}

	/** Exactly one of these fields is set per event. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Event(
			String introduced,
			String fixed,
			@JsonProperty("last_affected") String lastAffected,
			String limit) {
	}
}

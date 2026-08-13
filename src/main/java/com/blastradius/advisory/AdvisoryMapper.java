package com.blastradius.advisory;

import com.blastradius.advisory.osv.OsvModels.Affected;
import com.blastradius.advisory.osv.OsvModels.Severity;
import com.blastradius.advisory.osv.OsvModels.Vulnerability;
import com.blastradius.model.Advisory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns an OSV vulnerability into an {@link Advisory} row.
 *
 * <p>The raw {@code affected} ranges are stored as JSON in {@code affected_versions} rather
 * than normalised into tables: OSV's range shape is expressive and still evolving, and
 * Phase 5 re-reads it through {@link VersionRangeMatcher} anyway. Keeping the source of
 * truth verbatim avoids a lossy migration every time OSV adds a field.
 */
@Component
public class AdvisoryMapper {

	private static final Logger log = LoggerFactory.getLogger(AdvisoryMapper.class);
	private static final String SOURCE = "osv";

	private final ObjectMapper objectMapper;

	public AdvisoryMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Advisory toAdvisory(Vulnerability vuln, String ecosystem, String pkgName) {
		if (vuln == null || vuln.id() == null || vuln.id().isBlank()) {
			throw new AdvisoryException("OSV vulnerability is missing an id");
		}
		return Advisory.builder()
				.source(SOURCE)
				.externalId(vuln.id())
				.ecosystem(ecosystem)
				.pkgName(pkgName)
				.affectedVersions(serializeAffected(vuln.affectedOrEmpty()))
				.summary(bestSummary(vuln))
				.severity(bestSeverity(vuln.severity()))
				.publishedAt(vuln.published())
				.build();
	}

	/** Copies mutable advisory fields onto an existing row so refreshes stay current. */
	public void updateInPlace(Advisory existing, Vulnerability vuln) {
		existing.setAffectedVersions(serializeAffected(vuln.affectedOrEmpty()));
		existing.setSummary(bestSummary(vuln));
		existing.setSeverity(bestSeverity(vuln.severity()));
		if (vuln.published() != null) {
			existing.setPublishedAt(vuln.published());
		}
	}

	private String serializeAffected(List<Affected> affected) {
		try {
			return objectMapper.writeValueAsString(affected);
		}
		catch (JsonProcessingException e) {
			log.warn("Could not serialize affected ranges: {}", e.getMessage());
			return null;
		}
	}

	/** OSV puts a one-liner in {@code summary}; fall back to {@code details} when absent. */
	private String bestSummary(Vulnerability vuln) {
		if (vuln.summary() != null && !vuln.summary().isBlank()) {
			return vuln.summary();
		}
		return vuln.details();
	}

	/** Prefers a CVSS vector when several severity encodings are present. */
	private String bestSeverity(List<Severity> severities) {
		if (severities == null || severities.isEmpty()) {
			return null;
		}
		return severities.stream()
				.filter(s -> s != null && s.score() != null && !s.score().isBlank())
				.filter(s -> s.type() != null && s.type().startsWith("CVSS"))
				.map(Severity::score)
				.findFirst()
				.orElseGet(() -> severities.stream()
						.filter(s -> s != null && s.score() != null && !s.score().isBlank())
						.map(Severity::score)
						.findFirst()
						.orElse(null));
	}
}

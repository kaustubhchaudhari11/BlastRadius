package com.blastradius.ingestion;

/** Raised when a manifest cannot be read or parsed, or a repo path is invalid. */
public class IngestionException extends RuntimeException {

	public IngestionException(String message) {
		super(message);
	}

	public IngestionException(String message, Throwable cause) {
		super(message, cause);
	}
}

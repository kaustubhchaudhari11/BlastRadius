package com.blastradius.advisory;

/** Raised when advisory ingestion cannot complete (OSV unreachable, bad mapping, etc.). */
public class AdvisoryException extends RuntimeException {

	public AdvisoryException(String message) {
		super(message);
	}

	public AdvisoryException(String message, Throwable cause) {
		super(message, cause);
	}
}

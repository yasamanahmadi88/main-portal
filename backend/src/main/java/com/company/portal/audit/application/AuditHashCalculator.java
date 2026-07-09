package com.company.portal.audit.application;

import com.company.portal.audit.domain.AuditEventEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Canonical hash computation for audit events.
 *
 * <p>{@code current_hash = SHA-256(previous_hash || '|' || canonicalFields)}
 * where fields are joined with {@code '|'} and any null field is rendered as
 * empty string. Byte encoding is UTF-8. The result is lowercase hex.</p>
 *
 * <p>The canonical field list is intentionally frozen: adding a field later
 * would break integrity verification of past events. If a new field is
 * required, extend the schema and expose it in {@code payload_json}.</p>
 */
public final class AuditHashCalculator {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String algorithm;

    public AuditHashCalculator(String algorithm) {
        this.algorithm = algorithm == null ? "SHA-256" : algorithm;
    }

    public String compute(AuditEventEntity event, String previousHash) {
        String canonical = canonicalize(event, previousHash);
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Audit hash algorithm not available: " + algorithm, e);
        }
    }

    public String canonicalize(AuditEventEntity e, String previousHash) {
        StringBuilder sb = new StringBuilder(512);
        appendField(sb, previousHash);
        appendField(sb, e.getStream());
        appendField(sb, Long.toString(e.getSequenceNumber()));
        appendField(sb, e.getId() == null ? "" : e.getId().toString());
        appendField(sb, toIsoUtc(e.getOccurredAt()));
        appendField(sb, e.getEventType());
        appendField(sb, e.getCategory());
        appendField(sb, e.getSeverity() == null ? "" : e.getSeverity().name());
        appendField(sb, e.getActorType());
        appendField(sb, e.getActorId() == null ? "" : e.getActorId().toString());
        appendField(sb, e.getActorDisplay());
        appendField(sb, e.getTargetType());
        appendField(sb, e.getTargetId());
        appendField(sb, e.getTargetDisplay());
        appendField(sb, e.getAction());
        appendField(sb, e.getOutcome());
        appendField(sb, e.getIpAddress());
        appendField(sb, e.getUserAgent());
        appendField(sb, e.getCorrelationId());
        appendField(sb, e.getRequestId());
        appendField(sb, e.getTraceId());
        appendField(sb, e.getSessionId());
        // JSONB round-trips may reformat whitespace; canonicalize before hashing.
        appendField(sb, normalizeJson(e.getPayloadJson()));
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String v) {
        if (sb.length() > 0) {
            sb.append('|');
        }
        sb.append(v == null ? "" : v);
    }

    /**
     * PostgreSQL {@code timestamptz} stores microsecond precision and Hibernate may
     * reload offsets as {@code +00:00} instead of {@code Z}. Canonicalize via
     * {@link java.time.Instant} so write-time and verify-time hashes match.
     */
    private static String toIsoUtc(OffsetDateTime dt) {
        if (dt == null) {
            return "";
        }
        return dt.toInstant().truncatedTo(ChronoUnit.MICROS).toString();
    }

    static String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        try {
            JsonNode node = JSON.readTree(raw);
            return JSON.writeValueAsString(node);
        } catch (RuntimeException ex) {
            return raw.trim();
        }
    }
}

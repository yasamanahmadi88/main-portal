package com.company.portal.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.portal.audit.application.AuditHashCalculator;
import com.company.portal.audit.domain.AuditEventEntity;
import com.company.portal.audit.domain.AuditSeverity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifies invariants of the tamper-evident audit chain:
 * <ul>
 *   <li>hashes are deterministic for a given input;</li>
 *   <li>changing any field flips the hash;</li>
 *   <li>the previous_hash → current_hash relationship links events into
 *       an unbroken chain.</li>
 * </ul>
 */
class AuditHashChainTest {

    private final AuditHashCalculator calc = new AuditHashCalculator("SHA-256");

    @Test
    void hashIsDeterministicForEqualInputs() {
        AuditEventEntity a = sampleEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L);
        AuditEventEntity b = sampleEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L);
        String h1 = calc.compute(a, null);
        String h2 = calc.compute(b, null);
        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(64);
    }

    @Test
    void changingActorFlipsHash() {
        AuditEventEntity a = sampleEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L);
        AuditEventEntity b = sampleEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L);
        b.setActorId(UUID.randomUUID());
        assertThat(calc.compute(a, null)).isNotEqualTo(calc.compute(b, null));
    }

    @Test
    void chainLinksEvents() {
        AuditEventEntity first = sampleEvent(UUID.randomUUID(), 1L);
        String firstHash = calc.compute(first, null);
        first.setCurrentHash(firstHash);

        AuditEventEntity second = sampleEvent(UUID.randomUUID(), 2L);
        second.setAction("LOGOUT");
        second.setPreviousHash(firstHash);
        String secondHash = calc.compute(second, firstHash);
        second.setCurrentHash(secondHash);

        assertThat(second.getPreviousHash()).isEqualTo(first.getCurrentHash());
        // If we tamper with sequence, hash must change.
        second.setSequenceNumber(99L);
        String tampered = calc.compute(second, firstHash);
        assertThat(tampered).isNotEqualTo(secondHash);
    }

    private static AuditEventEntity sampleEvent(UUID id, long sequenceNumber) {
        AuditEventEntity event = new AuditEventEntity(id);
        event.setOccurredAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        event.setRecordedAt(event.getOccurredAt());
        event.setEventType("AUTH_LOGIN_SUCCESS");
        event.setCategory("AUTH");
        event.setSeverity(AuditSeverity.NOTICE);
        event.setActorType("USER");
        event.setActorId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        event.setActorDisplay("alice@example.com");
        event.setAction("LOGIN");
        event.setOutcome("SUCCESS");
        event.setPayloadJson("{}");
        event.setStream("global");
        event.setSequenceNumber(sequenceNumber);
        return event;
    }
}

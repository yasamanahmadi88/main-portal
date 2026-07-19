package com.company.portal.audit.application;

import com.company.portal.audit.domain.AuditEventEntity;
import com.company.portal.audit.repository.AuditEventRepository;
import com.company.portal.shared.config.PortalProperties;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Walks the audit chain and re-computes hashes to detect tampering. Complexity
 * is linear in the number of events; run in slices when the chain is large.
 */
@Service
public class AuditIntegrityVerifier {

    private static final String DEFAULT_STREAM = "global";
    private static final int BATCH_SIZE = 500;

    private final AuditEventRepository events;
    private final AuditHashCalculator hashCalculator;

    public AuditIntegrityVerifier(AuditEventRepository events, PortalProperties properties) {
        this.events = events;
        this.hashCalculator = new AuditHashCalculator(properties.getAudit().getHashAlgorithm());
    }

    public record Result(boolean valid, long checkedEvents, UUID firstInvalidEventId) { }

    @Transactional(readOnly = true)
    public Result verify() {
        return verify(0L, Long.MAX_VALUE);
    }

    @Transactional(readOnly = true)
    public Result verify(long fromSequenceExclusive, long toSequenceInclusive) {
        long checked = 0;
        String previousHash = null;
        long cursor = fromSequenceExclusive;
        while (cursor < toSequenceInclusive) {
            long batchEnd = Math.min(cursor + BATCH_SIZE, toSequenceInclusive);
            List<AuditEventEntity> batch = events.loadRange(DEFAULT_STREAM, cursor, batchEnd);
            if (batch.isEmpty()) {
                break;
            }
            for (AuditEventEntity event : batch) {
                if (previousHash == null && cursor == 0L) {
                    // Start of chain — event.previousHash must be null.
                    if (event.getPreviousHash() != null && !event.getPreviousHash().isEmpty()) {
                        return new Result(false, checked, event.getId());
                    }
                } else if (!Objects.equals(previousHash, event.getPreviousHash())) {
                    return new Result(false, checked, event.getId());
                }
                String recomputed = hashCalculator.compute(event, event.getPreviousHash());
                if (!recomputed.equals(event.getCurrentHash())) {
                    return new Result(false, checked, event.getId());
                }
                previousHash = event.getCurrentHash();
                checked++;
            }
            cursor = batch.get(batch.size() - 1).getSequenceNumber();
        }
        return new Result(true, checked, null);
    }
}

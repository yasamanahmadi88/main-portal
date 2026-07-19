package com.company.portal.identity.application;

import com.company.portal.identity.domain.UserSessionMetadataEntity;
import com.company.portal.identity.repository.UserSessionMetadataRepository;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session lifecycle helpers exposed to the identity controllers. Adds a
 * "revoke" thin layer on top of Spring Session's Redis repository so we can
 * mark sessions as revoked in {@code user_session_metadata} while also
 * evicting them from the store.
 */
@Service
public class SessionService {

    private final UserSessionMetadataRepository sessionMetadata;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public SessionService(UserSessionMetadataRepository sessionMetadata,
                          FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionMetadata = sessionMetadata;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public UserSessionMetadataEntity recordSession(UUID userId, String sessionId, String ip,
                                                    String userAgent, boolean mfaVerified) {
        UserSessionMetadataEntity entity = sessionMetadata.findBySessionId(sessionId).orElseGet(() ->
                new UserSessionMetadataEntity(UUID.randomUUID(), userId, sessionId));
        entity.setIpAddress(ip);
        entity.setUserAgent(userAgent);
        entity.setLastSeenAt(OffsetDateTime.now());
        if (mfaVerified) {
            entity.setMfaVerified(true);
            entity.setMfaVerifiedAt(OffsetDateTime.now());
        }
        return sessionMetadata.save(entity);
    }

    @Transactional(readOnly = true)
    public List<UserSessionMetadataEntity> listSessions(UUID userId) {
        return sessionMetadata.findActiveByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<UserSessionMetadataEntity> listAllSessions(Pageable pageable) {
        return sessionMetadata.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<UserSessionMetadataEntity> findBySessionId(String sessionId) {
        return sessionMetadata.findBySessionId(sessionId);
    }

    @Transactional
    public void adminRevokeSession(String sessionId, String reason) {
        UserSessionMetadataEntity entity = sessionMetadata.findBySessionId(sessionId).orElse(null);
        if (entity == null) return;
        entity.setRevokedAt(OffsetDateTime.now());
        entity.setRevokeReason(reason == null ? "ADMIN_REVOKED" : reason);
        sessionMetadata.save(entity);
        deleteStoreSession(sessionId);
    }

    @Transactional
    public void revokeSession(UUID userId, String sessionId, String reason) {
        UserSessionMetadataEntity entity = sessionMetadata.findBySessionId(sessionId)
                .orElseThrow(() -> new PortalException.NotFound("Session not found"));
        if (!entity.getUserId().equals(userId)) {
            throw new PortalException.Forbidden("Session does not belong to caller");
        }
        entity.setRevokedAt(OffsetDateTime.now());
        entity.setRevokeReason(reason == null ? "USER_REVOKED" : reason);
        sessionMetadata.save(entity);
        deleteStoreSession(sessionId);
    }

    @Transactional
    public int revokeOtherSessions(UUID userId, String keepSessionId) {
        List<UserSessionMetadataEntity> active = sessionMetadata.findActiveByUserId(userId);
        int revoked = 0;
        for (UserSessionMetadataEntity s : active) {
            if (keepSessionId != null && s.getSessionId().equals(keepSessionId)) continue;
            s.setRevokedAt(OffsetDateTime.now());
            s.setRevokeReason("USER_REVOKED_OTHERS");
            sessionMetadata.save(s);
            deleteStoreSession(s.getSessionId());
            revoked++;
        }
        return revoked;
    }

    @Transactional
    public int adminRevokeAll(UUID userId, String reason) {
        int updated = sessionMetadata.revokeAllForUser(userId, null,
                reason == null ? "ADMIN_REVOKED" : reason, OffsetDateTime.now());
        for (UserSessionMetadataEntity s : sessionMetadata.findActiveByUserId(userId)) {
            deleteStoreSession(s.getSessionId());
        }
        return updated;
    }

    private void deleteStoreSession(String sessionId) {
        try {
            sessionRepository.deleteById(sessionId);
        } catch (RuntimeException ignored) {
            // best-effort — session already gone
        }
    }
}

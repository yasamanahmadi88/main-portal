package com.company.portal.administration.web;

import com.company.portal.identity.application.MeService;
import com.company.portal.identity.application.SessionService;
import com.company.portal.identity.application.UserMapper;
import com.company.portal.identity.web.SessionDto;
import com.company.portal.shared.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative visibility into and control over user sessions across all
 * users. Individual users manage their own sessions via {@code /me/sessions}.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionAdminController {

    private final SessionService sessionService;
    private final MeService meService;
    private final UserMapper userMapper;

    public SessionAdminController(SessionService sessionService,
                                  MeService meService,
                                  UserMapper userMapper) {
        this.sessionService = sessionService;
        this.meService = meService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('session:read')")
    public PageResponse<SessionDto> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(sessionService.listAllSessions(
                        PageRequest.of(page, Math.min(size, 200),
                                Sort.by(Sort.Direction.DESC, "lastSeenAt")))
                .map(s -> userMapper.toDto(s, null,
                        meService.usernameForUserId(s.getUserId()).orElse(null))));
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('session:revoke')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String sessionId) {
        sessionService.adminRevokeSession(sessionId, "ADMIN_REVOKED");
    }
}

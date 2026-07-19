package com.company.portal.securityevent.web;

import com.company.portal.securityevent.application.SecurityEventService;
import com.company.portal.shared.security.CurrentUserAccessor;
import com.company.portal.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/security-events")
public class SecurityEventController {

    private final SecurityEventService service;
    private final CurrentUserAccessor currentUser;

    public SecurityEventController(SecurityEventService service, CurrentUserAccessor currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('security:read')")
    public PageResponse<SecurityEventDto> list(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(service.search(from, to, type, acknowledged,
                PageRequest.of(page, Math.min(size, 200),
                        Sort.by(Sort.Direction.DESC, "occurredAt"))));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('security:write')")
    public SecurityEventDto acknowledge(@PathVariable UUID id,
                                        @RequestBody(required = false) @Valid AcknowledgeRequest request) {
        String note = request == null ? null : request.note();
        UUID actorId = currentUser.currentUserIdOrThrow();
        return service.acknowledge(id, actorId, note);
    }

    public record AcknowledgeRequest(@Size(max = 1000) String note) { }
}

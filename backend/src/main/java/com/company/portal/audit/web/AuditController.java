package com.company.portal.audit.web;

import com.company.portal.audit.application.AuditIntegrityVerifier;
import com.company.portal.audit.application.AuditQueryService;
import com.company.portal.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Audit REST controller. All endpoints require the {@code audit:read} or
 * {@code audit:export} permission; contents map to the OpenAPI schemas
 * {@code AuditEvent}, {@code AuditEventPage} and {@code AuditIntegrityResult}.
 */
@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditController {

    private final AuditQueryService queryService;
    private final AuditIntegrityVerifier integrityVerifier;

    public AuditController(AuditQueryService queryService,
                           AuditIntegrityVerifier integrityVerifier) {
        this.queryService = queryService;
        this.integrityVerifier = integrityVerifier;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public PageResponse<AuditEventDto> list(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditEventDto> results = queryService.search(from, to, actorId, eventType, category, pageable);
        return PageResponse.of(results);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('audit:read')")
    public AuditEventDto get(@PathVariable UUID id) {
        return queryService.findById(id);
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('audit:export')")
    public ResponseEntity<StreamingResponseBody> export(@RequestBody @Valid AuditExportRequest request) {
        String format = request.format() == null ? "CSV" : request.format().toUpperCase();
        Pageable everything = PageRequest.of(0, 10_000, Sort.by(Sort.Direction.ASC, "sequenceNumber"));
        Page<AuditEventDto> page = queryService.search(request.from(), request.to(),
                null, null, null, everything);

        String contentType = format.equals("JSON") ? "application/json" : "text/csv";
        String extension = format.equals("JSON") ? "json" : "csv";

        StreamingResponseBody body = out -> {
            try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                if (format.equals("JSON")) {
                    w.write("[");
                    boolean first = true;
                    for (AuditEventDto dto : page.getContent()) {
                        if (!first) w.write(",");
                        w.write("{\"id\":\"" + dto.id() + "\","
                                + "\"occurredAt\":\"" + dto.occurredAt() + "\","
                                + "\"action\":\"" + jsonEsc(dto.action()) + "\","
                                + "\"outcome\":\"" + jsonEsc(dto.outcome()) + "\","
                                + "\"hash\":\"" + jsonEsc(dto.hash()) + "\"}");
                        first = false;
                    }
                    w.write("]");
                } else {
                    w.write("id,occurredAt,actorType,actorId,action,resourceType,resourceId,outcome,previousHash,hash\n");
                    for (AuditEventDto dto : page.getContent()) {
                        w.write(String.join(",",
                                csv(dto.id()), csv(dto.occurredAt()),
                                csv(dto.actor().type()), csv(dto.actor().userId()),
                                csv(dto.action()), csv(dto.resource().type()), csv(dto.resource().id()),
                                csv(dto.outcome()), csv(dto.previousHash()), csv(dto.hash())));
                        w.write("\n");
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write audit export", e);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-export." + extension + "\"")
                .body(body);
    }

    @PostMapping("/verify-integrity")
    @PreAuthorize("hasAuthority('audit:read')")
    public Map<String, Object> verifyIntegrity(@RequestBody(required = false) AuditIntegrityRequest ignoredBody) {
        AuditIntegrityVerifier.Result r = integrityVerifier.verify();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("valid", r.valid());
        body.put("checkedEvents", r.checkedEvents());
        body.put("firstInvalidEventId", r.firstInvalidEventId());
        body.put("verifiedAt", OffsetDateTime.now());
        return body;
    }

    private static String jsonEsc(Object v) {
        if (v == null) return "";
        return v.toString().replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String csv(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public record AuditExportRequest(String format, OffsetDateTime from, OffsetDateTime to,
                                     Map<String, Object> filters) { }

    public record AuditIntegrityRequest(OffsetDateTime from, OffsetDateTime to) { }
}

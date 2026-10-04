package com.synapse.waypoint.issue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

@RestController
@RequestMapping("/api")
class IssueController {
    private final JdbcTemplate jdbc;
    private final CurrentUser user;
    private final DemoClock clock;
    private final OrderService orders;
    private final NotificationService notifications;

    IssueController(JdbcTemplate jdbc, CurrentUser user, DemoClock clock, OrderService orders,
                    NotificationService notifications) {
        this.jdbc = jdbc; this.user = user; this.clock = clock; this.orders = orders;
        this.notifications = notifications;
    }

    private String outletId() {
        return user.outletId().orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN, "Your account is not linked to an outlet."));
    }

    @PostMapping("/store/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    Issue create(@Valid @RequestBody NewIssue body) {
        if (body.orderId() != null) orders.get(body.orderId());
        String id = UUID.randomUUID().toString();
        String ref = "ISS-" + id.replace("-", "").substring(0, 8).toUpperCase();
        jdbc.update("INSERT INTO issues(id,ref,outlet_id,order_id,type,units,wants,created_by,created_at) VALUES (?,?,?,?,?,?,?,?,?)",
                id, ref, outletId(), body.orderId(), body.type(), body.units(), body.wants(), user.id(), Timestamp.from(clock.now()));
        if (body.note() != null && !body.note().isBlank()) addMessage(id, body.note());
        notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(outletId()),
                NotificationSeverity.INFO, "ISSUE_RECORDED", "Issue " + ref + " recorded",
                "Your reported delivery issue has been recorded. Dispatch can now review it.",
                "/store/issues/" + id);
        return get(id, true);
    }

    @GetMapping("/store/issues")
    ListResponse<Issue> storeList() { return ListResponse.of(list("WHERE outlet_id = ?", outletId())); }

    @GetMapping("/store/issues/{id}")
    Issue storeDetail(@PathVariable String id) { return get(id, true); }

    @PostMapping("/store/issues/{id}/messages")
    @Transactional
    Issue storeMessage(@PathVariable String id, @Valid @RequestBody MessageRequest body) {
        get(id, true); addMessage(id, body.text()); return get(id, true);
    }

    @GetMapping("/dispatch/issues")
    ListResponse<Issue> dispatchList(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) return ListResponse.of(list("", new Object[0]));
        if (!List.of("OPEN", "ANSWERED", "RESOLVED").contains(status))
            throw new DomainException(ErrorCode.VALIDATION, "Unknown issue status.");
        return ListResponse.of(list("WHERE status = ?", status));
    }

    @GetMapping("/dispatch/issues/{id}")
    Issue dispatchDetail(@PathVariable String id) { return get(id, false); }

    @PostMapping("/dispatch/issues/{id}/reply")
    @Transactional
    Issue reply(@PathVariable String id, @Valid @RequestBody MessageRequest body) {
        Issue issue = get(id, false);
        if (issue.status().equals("RESOLVED")) throw new DomainException(ErrorCode.INVALID_STATUS, "Issue is resolved.");
        addMessage(id, body.text());
        jdbc.update("UPDATE issues SET status = 'ANSWERED', version = version + 1 WHERE id = ?", id);
        notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(issue.outletId()),
                NotificationSeverity.WARNING, "ISSUE_REPLY", "Dispatch replied to " + issue.ref(),
                "Dispatch has responded to your issue. Open the conversation to read the reply.",
                "/store/issues/" + id);
        return get(id, false);
    }

    @PostMapping("/dispatch/issues/{id}/resolve")
    @Transactional
    Issue resolve(@PathVariable String id) {
        Issue issue = get(id, false);
        if (issue.status().equals("RESOLVED"))
            throw new DomainException(ErrorCode.INVALID_STATUS, "Issue is already resolved.");
        jdbc.update("UPDATE issues SET status = 'RESOLVED', resolved_by = ?, resolved_at = ?, version = version + 1 WHERE id = ?",
                user.id(), Timestamp.from(clock.now()), id);
        notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(issue.outletId()),
                NotificationSeverity.INFO, "ISSUE_RESOLVED", "Issue " + issue.ref() + " resolved",
                "Dispatch marked your issue as resolved. Open the conversation for details.",
                "/store/issues/" + id);
        return get(id, false);
    }

    @PostMapping(value = "/store/issues/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    Map<String, String> uploadPhoto(@PathVariable String id, @RequestParam("file") MultipartFile file) throws Exception {
        get(id, true);
        String type = file.getContentType();
        if (file.isEmpty() || file.getSize() > 1_000_000 || !List.of("image/jpeg", "image/png", "image/webp").contains(type))
            throw new DomainException(ErrorCode.VALIDATION, "Upload a JPEG, PNG, or WebP image under 1 MB.");
        String photoId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO issue_photos(id,issue_id,content_type,content,uploaded_at) VALUES (?,?,?,?,?)",
                photoId, id, type, file.getBytes(), Timestamp.from(clock.now()));
        return Map.of("id", photoId, "url", "/api/store/issues/" + id + "/photos/" + photoId);
    }

    @GetMapping({"/store/issues/{id}/photos/{photoId}", "/dispatch/issues/{id}/photos/{photoId}"})
    ResponseEntity<byte[]> photo(@PathVariable String id, @PathVariable String photoId,
                                 jakarta.servlet.http.HttpServletRequest request) {
        get(id, request.getRequestURI().contains("/store/"));
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT content_type,content FROM issue_photos WHERE id = ? AND issue_id = ?", photoId, id);
        if (rows.isEmpty()) throw new NotFoundException("Photo", photoId);
        Map<String, Object> row = rows.get(0);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) row.get("content_type")))
                .header("Cache-Control", "private, max-age=300").body((byte[]) row.get("content"));
    }

    private void addMessage(String id, String text) {
        jdbc.update("INSERT INTO issue_messages(id,issue_id,author_id,text,created_at) VALUES (?,?,?,?,?)",
                UUID.randomUUID().toString(), id, user.id(), text.strip(), Timestamp.from(clock.now()));
    }

    private List<Issue> list(String clause, Object... args) {
        return jdbc.query("SELECT id FROM issues " + clause + " ORDER BY created_at DESC", (rs, row) -> get(rs.getString(1), false), args);
    }

    private Issue get(String id, boolean storeScoped) {
        List<Issue> issues = jdbc.query("SELECT id,ref,outlet_id,order_id,type,units,wants,status,created_at,resolved_at FROM issues WHERE id = ?" + (storeScoped ? " AND outlet_id = ?" : ""),
                (rs, row) -> new Issue(rs.getString("id"), rs.getString("ref"), rs.getString("outlet_id"),
                        rs.getString("order_id"), rs.getString("type"), (Integer) rs.getObject("units"),
                        rs.getString("wants"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toInstant(),
                        messages(id), photoIds(id)), storeScoped ? new Object[]{id, outletId()} : new Object[]{id});
        if (issues.isEmpty()) throw new NotFoundException("Issue", id);
        return issues.get(0);
    }

    private List<IssueMessage> messages(String id) {
        return jdbc.query("""
                SELECT m.id,m.author_id,u.name,m.text,m.created_at
                FROM issue_messages m JOIN users u ON u.id = m.author_id
                WHERE m.issue_id = ? ORDER BY m.created_at,m.id""",
                (rs, row) -> new IssueMessage(rs.getString("id"), rs.getString("author_id"),
                        rs.getString("name"), rs.getString("text"), rs.getTimestamp("created_at").toInstant()), id);
    }

    private List<String> photoIds(String id) {
        return jdbc.queryForList("SELECT id FROM issue_photos WHERE issue_id = ? ORDER BY uploaded_at", String.class, id);
    }

    record NewIssue(String orderId,
                    @NotBlank @Pattern(regexp = "DAMAGED|MISSING|WRONG_ITEM|TEMPERATURE|LATE|OTHER") String type,
                    @Min(1) Integer units,
                    @NotBlank @Pattern(regexp = "REPLACE|CREDIT|NOTHING") String wants,
                    @NotBlank @Size(max = 2000) String note) {}
    record MessageRequest(@NotBlank @Size(max = 2000) String text) {}
    record Issue(String id, String ref, String outletId, String orderId, String type, Integer units, String wants,
                 String status, Instant createdAt, Instant resolvedAt, List<IssueMessage> messages, List<String> photoIds) {}
    record IssueMessage(String id, String authorId, String authorName, String text, Instant createdAt) {}
}

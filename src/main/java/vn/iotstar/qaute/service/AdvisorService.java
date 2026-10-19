package vn.iotstar.qaute.service;

import vn.iotstar.qaute.dto.AdvisorForm;
import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdvisorService {
    private final AdvisorProfileRepository advisors;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final ActivityLogService logs;

    public List<AdvisorProfile> search(String kw, Long deptId, AdvisorStatus status) {
        String k = kw == null ? "" : kw.trim().toLowerCase();
        return advisors.findAll().stream()
                .filter(a -> !a.isDeleted())
                .filter(a -> deptId == null || a.getDepartment().getId().equals(deptId))
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> k.isEmpty()
                        || a.getUser().getEmail().toLowerCase().contains(k)
                        || (a.getUser().getFullName() != null && a.getUser().getFullName().toLowerCase().contains(k)))
                .toList();
    }

    /** allowed: null = tất cả (Admin); rỗng = không khoa nào; còn lại = các khoa được phép. */
    public List<AdvisorProfile> inDepartments(Set<Long> allowed) {
        return advisors.findAll().stream()
                .filter(a -> !a.isDeleted())
                .filter(a -> ScopeService.allows(allowed, a.getDepartment()))
                .sorted((x, y) -> x.getDepartment().getName().compareToIgnoreCase(y.getDepartment().getName()))
                .toList();
    }

    /** Hồ sơ chưa bị gỡ. Hồ sơ đã gỡ coi như không tồn tại. */
    public AdvisorProfile get(Long id) {
        return advisors.findById(id).filter(a -> !a.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tư vấn viên"));
    }

    /** NGƯỜI 2: dùng hàm này để kiểm tra quyền tư vấn viên (ACTIVE và chưa bị gỡ). */
    public boolean isActiveAdvisor(Long userId) {
        return advisors.existsByUserIdAndStatusAndDeletedFalse(userId, AdvisorStatus.ACTIVE);
    }

    @Transactional
    public AdvisorProfile create(AdvisorForm f, String actor) {
        User u = users.findByEmailIgnoreCase(f.getEmail().trim())
                .orElseThrow(() -> new FieldException("email",
                        "Không tìm thấy tài khoản với email này hoặc email sai, mời nhập lại"));
        Optional<AdvisorProfile> existing = advisors.findByUserId(u.getId());
        if (existing.isPresent() && !existing.get().isDeleted())
            throw new FieldException("email", "Người dùng này đã là tư vấn viên");
        boolean managerAsTeamLead = u.getRole() == Role.MANAGER
                && f.getAdvisorType() == AdvisorType.TEAM_LEADER;
        if (u.getRole() != Role.USER && u.getRole() != Role.CONSULTANT && !managerAsTeamLead)
            throw new FieldException("email", "Chỉ tài khoản người dùng hoặc trưởng ban cố vấn mới có thể làm tư vấn viên");
        Department d = departments.findById(f.getDepartmentId())
                .orElseThrow(() -> new FieldException("departmentId", "Khoa không tồn tại"));
        u.setFullName(f.getFullName().trim());
        u.setPhone(f.getPhone().trim());

        boolean restored = existing.isPresent();      // từng bị gỡ -> khôi phục hồ sơ cũ, không tạo trùng
        AdvisorProfile p = existing.orElseGet(AdvisorProfile::new);
        p.setDeleted(false); p.setDeletedAt(null); p.setDeletedBy(null); p.setHead(false);
        p.setUser(u); p.setDepartment(d); p.setAdvisorType(f.getAdvisorType());
        p.setAudience(f.getAudience()); p.setStatus(f.getStatus());
        advisors.save(p);
        if (f.isHead()) applyHead(p, true);
        logs.log(actor, "ADMIN", restored ? "RESTORE_ADVISOR" : "ADD_ADVISOR", u.getEmail() + " -> " + d.getName());
        return p;
    }

    @Transactional
    public void update(Long id, AdvisorForm f, String actor) {
        AdvisorProfile p = get(id);
        Department d = departments.findById(f.getDepartmentId())
                .orElseThrow(() -> new FieldException("departmentId", "Khoa không tồn tại"));
        p.getUser().setFullName(f.getFullName().trim());
        p.getUser().setPhone(f.getPhone().trim());
        boolean moved = !p.getDepartment().getId().equals(d.getId());
        p.setDepartment(d); p.setAdvisorType(f.getAdvisorType());
        p.setAudience(f.getAudience()); p.setStatus(f.getStatus());
        if (moved || f.getStatus() == AdvisorStatus.INACTIVE) p.setHead(false);
        if (f.getStatus() == AdvisorStatus.ACTIVE) applyHead(p, f.isHead());
        logs.log(actor, "ADMIN", "UPDATE_ADVISOR", p.getUser().getEmail());
    }

    /** INACTIVE: khoá quyền tư vấn viên, user vẫn hỏi được như user thường. */
    @Transactional
    public AdvisorStatus toggleStatus(Long id, String actor) {
        AdvisorProfile p = get(id);
        p.setStatus(p.getStatus() == AdvisorStatus.ACTIVE ? AdvisorStatus.INACTIVE : AdvisorStatus.ACTIVE);
        if (p.getStatus() == AdvisorStatus.INACTIVE) p.setHead(false);
        logs.log(actor, "ADMIN", p.getStatus() == AdvisorStatus.ACTIVE ? "ACTIVATE_ADVISOR" : "DEACTIVATE_ADVISOR",
                p.getUser().getEmail());
        return p.getStatus();
    }

    /**
     * XOÁ MỀM: gỡ khỏi đội ngũ. KHÔNG xoá dòng nào: tài khoản User, Question, Answer, ActivityLog đều còn nguyên
     * (Answer trỏ tới User, không trỏ tới AdvisorProfile). Hồ sơ được đánh dấu deleted để còn lịch sử và khôi phục được.
     */
    @Transactional
    public void delete(Long id, String actor) {
        AdvisorProfile p = get(id);
        p.setDeleted(true); p.setDeletedAt(LocalDateTime.now()); p.setDeletedBy(actor);
        p.setStatus(AdvisorStatus.INACTIVE); p.setHead(false);
        logs.log(actor, "ADMIN", "REMOVE_ADVISOR", p.getUser().getEmail() + " @ " + p.getDepartment().getName());
    }

    /**
     * Chỉ định / gỡ trưởng chuyên viên. allowedDepts giới hạn khoa được thao tác
     * (null = Admin; rỗng = không khoa nào). Vượt phạm vi -> AccessDeniedException (403).
     */
    @Transactional
    public boolean toggleHead(Long id, String actor, String role, Set<Long> allowedDepts) {
        AdvisorProfile p = get(id);
        if (!ScopeService.allows(allowedDepts, p.getDepartment()))
            throw new AccessDeniedException("Tư vấn viên không thuộc khoa bạn phụ trách");
        if (p.getStatus() != AdvisorStatus.ACTIVE)
            throw new IllegalArgumentException("Chỉ chỉ định trưởng cho tư vấn viên đang hoạt động");
        boolean now = !p.isHead();
        applyHead(p, now);
        logs.log(actor, role, now ? "SET_HEAD_ADVISOR" : "UNSET_HEAD_ADVISOR",
                p.getUser().getEmail() + " @ " + p.getDepartment().getName());
        return now;
    }

    private void applyHead(AdvisorProfile p, boolean head) {
        if (head) {
            advisors.findByDepartmentIdAndHeadTrue(p.getDepartment().getId())
                    .forEach(o -> { if (o.getId() != null && !o.getId().equals(p.getId())) o.setHead(false); });
        }
        p.setHead(head);
    }
}


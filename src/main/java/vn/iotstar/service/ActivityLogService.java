package vn.iotstar.service;

import vn.iotstar.entity.ActivityLog;
import vn.iotstar.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** NGƯỜI 1 & 2: gọi log(...) khi user hỏi / tư vấn viên trả lời, chuyển câu hỏi, đăng nhập... */
@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository repo;

    public void log(String email, String role, String action, String target) {
        ActivityLog l = new ActivityLog();
        l.setActorEmail(email);
        l.setActorRole(role);
        l.setAction(action);
        l.setTarget(target);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            HttpServletRequest r = a.getRequest();
            String xff = r.getHeader("X-Forwarded-For");
            l.setIp(xff != null ? xff.split(",")[0].trim() : r.getRemoteAddr());
        }
        repo.save(l);
    }
}


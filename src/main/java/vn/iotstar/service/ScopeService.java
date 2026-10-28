package vn.iotstar.service;

import vn.iotstar.entity.Department;
import vn.iotstar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phạm vi khoa của người dùng.
 *  - null            : không giới hạn (chỉ Admin truyền null)
 *  - tập rỗng        : KHÔNG được truy cập khoa nào (Manager chưa được gán khoa)
 *  - tập có phần tử  : chỉ các khoa trong tập
 */
@Service
@RequiredArgsConstructor
public class ScopeService {
    private final UserRepository users;

    public Set<Long> managerDepartments(String email) {
        return users.findByEmailIgnoreCase(email)
                .map(u -> u.getManagedDepartments().stream().map(Department::getId).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public static boolean allows(Set<Long> allowed, Department d) {
        return allowed == null || (d != null && d.getId() != null && allowed.contains(d.getId()));
    }
}


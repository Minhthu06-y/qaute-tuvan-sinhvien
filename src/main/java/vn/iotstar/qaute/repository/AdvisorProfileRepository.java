package vn.iotstar.qaute.repository;
import vn.iotstar.qaute.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface AdvisorProfileRepository extends JpaRepository<AdvisorProfile, Long> {
    Optional<AdvisorProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndStatusAndDeletedFalse(Long userId, AdvisorStatus status);
    List<AdvisorProfile> findByDepartmentIdAndHeadTrue(Long departmentId);
}


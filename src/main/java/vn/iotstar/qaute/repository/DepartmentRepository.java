package vn.iotstar.qaute.repository;
import vn.iotstar.qaute.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface DepartmentRepository extends JpaRepository<Department, Long> {}


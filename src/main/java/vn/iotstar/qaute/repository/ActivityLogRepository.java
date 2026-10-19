package vn.iotstar.qaute.repository;
import vn.iotstar.qaute.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    @Query("select l from ActivityLog l where l.createdAt >= :from and l.createdAt < :to and " +
           "(lower(l.actorEmail) like lower(concat('%', :kw, '%')) or lower(l.action) like lower(concat('%', :kw, '%'))) " +
           "order by l.createdAt desc")
    Page<ActivityLog> search(@Param("kw") String kw, @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to, Pageable p);
}


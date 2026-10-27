package vn.iotstar.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.entity.*;
import vn.iotstar.enums.*;


public interface QauteAnswerRepository extends JpaRepository<Answer, Long> {
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        LocalDateTime from,
        LocalDateTime to
    );

    long countByApprovalStatus(ApprovalStatus status);

    @Query("""
        select a from QauteAnswer a
        where a.question.id = :questionId
          and (a.published = true or a.approved = true)
        order by a.createdAt asc
        """)
    List<Answer> findByQuestion_IdAndPublishedTrueOrderByCreatedAtAsc(
        @Param("questionId") Long questionId
    );

    List<Answer> findByQuestion_IdOrderByCreatedAtAsc(Long questionId);

    @Query("select a.createdAt from QauteAnswer a where a.createdAt >= :from and a.createdAt < :to")
    List<LocalDateTime> createdTimes(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("""
        select coalesce(a.advisor.fullName, a.author.fullName),
               coalesce(a.advisor.email, a.author.email), count(a)
        from QauteAnswer a
        where a.createdAt >= :from and a.createdAt < :to
        group by coalesce(a.advisor.fullName, a.author.fullName),
                 coalesce(a.advisor.email, a.author.email)
        order by count(a) desc
        """)
    List<Object[]> topAdvisors(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

    @Query("select a from QauteAnswer a where a.approvalStatus = :status order by a.createdAt desc")
    List<Answer> findByStatus(@Param("status") ApprovalStatus status);
}


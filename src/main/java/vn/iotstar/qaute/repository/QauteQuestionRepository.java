package vn.iotstar.qaute.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.qaute.entity.Question;
import vn.iotstar.qaute.entity.QuestionStatus;

public interface QauteQuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
        select q from QauteQuestion q
        where q.publicVisible = true or q.publicQuestion = true
        order by q.createdAt desc
        """)
    List<Question> findByPublicVisibleTrueOrderByCreatedAtDesc();

    @Query("""
        select q from QauteQuestion q
        left join q.asker asker
        left join q.author author
        where asker.id = :userId or author.id = :userId
        order by q.createdAt desc
        """)
    List<Question> findByAsker_IdOrderByCreatedAtDesc(
        @Param("userId") Long userId
    );

    List<Question> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        LocalDateTime from,
        LocalDateTime to
    );

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        LocalDateTime from,
        LocalDateTime to
    );

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        QuestionStatus status,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query("select q.createdAt from QauteQuestion q where q.createdAt >= :from and q.createdAt < :to")
    List<LocalDateTime> createdTimes(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}


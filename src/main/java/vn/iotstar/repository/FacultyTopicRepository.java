package vn.iotstar.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.model.FacultyTopic;

public interface FacultyTopicRepository extends JpaRepository<FacultyTopic, Long> {
    List<FacultyTopic> findByFacultyIdOrderByIdAsc(Long facultyId);
    List<FacultyTopic> findAllByOrderByFacultyIdAscIdAsc();
}

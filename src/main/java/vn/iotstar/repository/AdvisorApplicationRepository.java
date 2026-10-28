package vn.iotstar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.entity.AdvisorApplication;
import vn.iotstar.enums.AdvisorApplicationStatus;

public interface AdvisorApplicationRepository extends JpaRepository<AdvisorApplication, Long> {
    Optional<AdvisorApplication> findFirstByApplicantIdOrderByCreatedAtDesc(Long applicantId);
    Optional<AdvisorApplication> findFirstByApplicantIdAndStatusOrderByCreatedAtDesc(
            Long applicantId, AdvisorApplicationStatus status);
    List<AdvisorApplication> findByStatusOrderByCreatedAtAsc(AdvisorApplicationStatus status);
}

package vn.iotstar;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.entity.User;
import vn.iotstar.entity.Department;
import vn.iotstar.enums.*;
import vn.iotstar.dto.ReviewForm;
import vn.iotstar.model.Answer;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.Question;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.AnswerReviewRepository;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.ReviewService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class QauteTuvanSinhvienApplicationTests {
	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository users;

	@Autowired
	DepartmentRepository departments;

	@Autowired
	FacultyRepository faculties;

	@Autowired
	QuestionRepository questions;

	@Autowired
	AnswerRepository answers;

	@Autowired
	AnswerReviewRepository reviews;

	@Autowired
	ReviewService reviewService;

	@BeforeEach
	void ensureAdminAccountExists() {
		User admin = users.findByEmailIgnoreCase("admin@qaute.edu.vn").orElseGet(User::new);
		admin.setFullName("QAUTE Test Admin");
		admin.setEmail("admin@qaute.edu.vn");
		admin.setPasswordHash("test-only");
		admin.setRole(Role.ADMIN);
		admin.setStatus(UserStatus.ACTIVE);
		admin.setEnabled(true);
		users.save(admin);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void adminAdvisorsPageRenders() throws Exception {
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(
				"admin@qaute.edu.vn", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		SecurityContextHolder.setContext(context);

		try {
			mockMvc.perform(get("/admin/advisors"))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("Quản lý tư vấn viên")));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void adminCanOpenQuestionDashboard() throws Exception {
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(
				"admin@qaute.edu.vn", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		SecurityContextHolder.setContext(context);

		try {
			mockMvc.perform(get("/questions"))
					.andExpect(status().isOk());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void approvingAnswerPublishesItAndUpdatesQuestionStatus() {
		Department department = departments.save(new Department("Test Department"));
		Faculty faculty = faculties.save(new Faculty("Test Faculty", "TF"));
		User manager = activeUser("manager@qaute.edu.vn", Role.MANAGER);
		manager.getManagedDepartments().add(department);
		manager = users.save(manager);
		User asker = users.save(activeUser("student@qaute.edu.vn", Role.USER));
		User consultant = users.save(activeUser("consultant@qaute.edu.vn", Role.CONSULTANT));

		Question question = new Question(asker, faculty, "Test question", "Question body", null);
		question.setDepartment(department);
		question = questions.save(question);
		Answer answer = answers.save(new Answer(question, consultant, "Pending answer", null, ApprovalStatus.PENDING));

		ReviewForm form = new ReviewForm();
		form.setDecision(ReviewDecision.APPROVED);
		form.setReason("Approved for publication");
		reviewService.decide(answer.getId(), form, manager.getEmail());

		assertEquals(ApprovalStatus.APPROVED, answers.findById(answer.getId()).orElseThrow().getApprovalStatus());
		assertEquals(QuestionStatus.ANSWERED, questions.findById(question.getId()).orElseThrow().getStatus());
		assertTrue(reviews.findByAnswerIdOrderByCreatedAtDesc(answer.getId()).stream()
				.anyMatch(review -> review.getDecision() == ReviewDecision.APPROVED));
	}

	private User activeUser(String email, Role role) {
		User user = new User();
		user.setFullName(role.name() + " Test");
		user.setEmail(email);
		user.setPasswordHash("test-only");
		user.setRole(role);
		user.setStatus(UserStatus.ACTIVE);
		user.setEnabled(true);
		return user;
	}

}

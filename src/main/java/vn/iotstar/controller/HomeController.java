package vn.iotstar.controller;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.Question;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.QuestionRepository;

/** Public landing page for questions shared with everyone. */
@Controller
public class HomeController {
    private final QuestionRepository questions;
    private final FacultyRepository faculties;

    public HomeController(QuestionRepository questions, FacultyRepository faculties) {
        this.questions = questions;
        this.faculties = faculties;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) Long facultyId, Model model) {
        List<Faculty> allFaculties = faculties.findAll(Sort.by(Sort.Direction.ASC, "name"));
        Faculty selectedFaculty = facultyId == null ? null : faculties.findById(facultyId).orElse(null);
        List<Question> publicQuestions = selectedFaculty == null
                ? questions.findByIsPublicTrueOrderByCreatedAtDesc()
                : questions.findByFacultyIdAndIsPublicTrueOrderByCreatedAtDesc(selectedFaculty.getId());
        model.addAttribute("faculties", allFaculties);
        model.addAttribute("selectedFaculty", selectedFaculty);
        model.addAttribute("publicQuestions", publicQuestions);
        return "home";
    }
}

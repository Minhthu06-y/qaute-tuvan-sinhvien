package vn.iotstar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "faculty_topics", uniqueConstraints = @UniqueConstraint(columnNames = {"faculty_id", "name"}))
public class FacultyTopic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(nullable = false, length = 240)
    private String name;

    protected FacultyTopic() {}

    public FacultyTopic(Faculty faculty, String name) {
        this.faculty = faculty;
        this.name = name;
    }

    public Long getId() { return id; }
    public Faculty getFaculty() { return faculty; }
    public String getName() { return name; }
}

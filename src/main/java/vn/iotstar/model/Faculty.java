package vn.iotstar.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "faculties")
public class Faculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String shortName;

    protected Faculty() {}
    public Faculty(String name, String shortName) { this.name = name; this.shortName = shortName; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public void setName(String name) { this.name = name; }
    public void setShortName(String shortName) { this.shortName = shortName; }
}

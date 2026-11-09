package ru.mirea.platform.project;


import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 80) private String softwareVersion;
    @Column(nullable = false, length = 500) private String artifactReference;
    @Column(nullable = false) private Instant createdAt;
    protected Project() {}
    public Project(String name, String softwareVersion, String artifactReference) {
        this.name = name; this.softwareVersion = softwareVersion;
        this.artifactReference = artifactReference; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSoftwareVersion() { return softwareVersion; }
    public String getArtifactReference() { return artifactReference; }
    public Instant getCreatedAt() { return createdAt; }
}

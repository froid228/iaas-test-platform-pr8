package ru.mirea.platform.environment;


import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "environment_templates")
public class EnvironmentTemplate {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 120) private String operatingSystem;
    @Column(nullable = false, length = 500) private String imageReference;
    @Column(nullable = false) private int cpu;
    @Column(nullable = false) private int memoryMb;
    protected EnvironmentTemplate() {}
    public EnvironmentTemplate(EnvironmentRequest r) {
        name = r.name(); operatingSystem = r.operatingSystem();
        imageReference = r.imageReference(); cpu = r.cpu(); memoryMb = r.memoryMb();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getOperatingSystem() { return operatingSystem; }
    public String getImageReference() { return imageReference; }
    public int getCpu() { return cpu; }
    public int getMemoryMb() { return memoryMb; }
}

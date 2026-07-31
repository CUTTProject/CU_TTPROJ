package com.university.timetable_scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Soft-delete marker. Kept as a wrapper (not primitive) so Lombok keeps generating
     * {@code getIsDeleted()}/{@code setIsDeleted()}, and defaulted here as well as in
     * {@link #onCreate()} so an entity is never null-flagged even if persisted outside JPA.
     */
    private Boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        isDeleted = false;
    }
}

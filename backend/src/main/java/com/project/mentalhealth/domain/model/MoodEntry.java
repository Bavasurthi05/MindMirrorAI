package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "mood_entries")
public class MoodEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "mood_score", nullable = false)
    private int moodScore;

    @Column(name = "mood_label")
    private String moodLabel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}

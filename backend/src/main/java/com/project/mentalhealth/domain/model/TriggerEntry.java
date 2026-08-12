package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "trigger_entries")
public class TriggerEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int intensity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** {@code USER_LOGGED} when entered by hand, {@code DERIVED} when detected from text. */
    @Column(nullable = false, length = 24)
    private String source = EntrySource.USER_LOGGED;

    /** Review state for detected triggers; user-logged ones need no review. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TriggerConfirmation confirmation = TriggerConfirmation.NOT_REQUIRED;

    /** The analysis that surfaced this trigger, when it was detected rather than logged. */
    @Column(name = "analysis_result_id")
    private Long analysisResultId;
}

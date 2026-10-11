package rw.afriteck.wms.operations.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.operations.enums.TaskEventType;
import rw.afriteck.wms.operations.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_event", indexes =
@Index(name = "ix_task_event_task", columnList = "task_id, occurred_at"))
@Getter
@Setter
@NoArgsConstructor
public class TaskEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20) private
    TaskStatus toStatus;

    @Column(name = "actor_user_id")
    private UUID actorUserId;    // null when done by a job

    @Column(name = "assignee_id")
    private UUID assigneeId;     // snapshot, for productivity

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(length = 255)
    private String note;
}

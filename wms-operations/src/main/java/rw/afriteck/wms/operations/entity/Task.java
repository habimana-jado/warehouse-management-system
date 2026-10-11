package rw.afriteck.wms.operations.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "task", indexes = {
        @Index(name = "ix_task_board", columnList = "warehouse_id, status, type, priority"),
        @Index(name = "ix_task_assignee", columnList = "assignee_id, status"),
        @Index(name = "ix_task_reference", columnList = "reference_type, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Task extends BaseEntity {

    @Column(name = "task_number", nullable = false, unique = true, length = 30)
    private String taskNumber;                 // e.g. TSK-000123, from a DB sequence

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TaskType type;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.OPEN;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.NORMAL;

    @Formula("case priority when 'URGENT' then 4 when 'HIGH' then 3 when 'NORMAL' then 2 else 1 end")
    @Setter(AccessLevel.NONE)
    private int priorityRank;          // read-only: no setter, never written

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "zone_location_id")
    private UUID zoneLocationId;               // optional

    // what the task is about (plain strings: operations must not know other modules' enums)
    @Column(name = "reference_type", nullable = false, length = 40)
    private String referenceType;              // "PURCHASE_ORDER", "PICK_LIST", ...
    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;
    @Column(name = "reference_number", length = 40)
    private String referenceNumber;            // human label for the board

    @Column(name = "assignee_id")
    private UUID assigneeId;                   // user id
    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant dueAt;

    @Column(length = 255) private String cancelReason;
    @Column(length = 500) private String notes;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<TaskLine> lines = new ArrayList<>();

    public void addLine(TaskLine l) { l.setTask(this); lines.add(l); }
}

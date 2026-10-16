package rw.afriteck.wms.outbound.entity;

import rw.afriteck.wms.outbound.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class Shipment extends BaseEntity {

    @Column(name = "shipment_number", nullable = false, length = 20)
    private String shipmentNumber;                    // DN-000123 (delivery note number)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;                          // copied from the order

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.DISPATCHED;

    @Column(name = "shipped_at", nullable = false)
    private Instant shippedAt;

    @Column(name = "ship_to_address", length = 500)
    private String shipToAddress;                     // defaults to the customer address, editable at dispatch

    @Column(length = 100)
    private String carrier;

    @Column(name = "tracking_reference", length = 100)
    private String trackingReference;

    @Column(name = "package_count")
    private Integer packageCount;                     // light packing step: no carton contents

    @Column(length = 1000)
    private String notes;

    // proof of delivery (optional)
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "received_by_name", length = 120)
    private String receivedByName;

    @Column(name = "pod_notes", length = 500)
    private String podNotes;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<ShipmentLine> lines = new ArrayList<>();

    public void addLine(ShipmentLine line) {
        line.setShipment(this);
        lines.add(line);
    }
}


package rw.afriteck.wms.foundation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

@MappedSuperclass
@Getter
@Setter
public abstract class BusinessPartner extends BaseEntity {
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String addressLine;
    private String city;
    private String country;
    @Column(nullable = false)
    private boolean active = true;
}

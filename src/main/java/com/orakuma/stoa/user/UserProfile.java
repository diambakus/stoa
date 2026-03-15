package com.orakuma.stoa.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Accessors(chain = true)
@Data
public class UserProfile {
    @Id
    private String id;
    private String displayName;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "bigint[]")
    private Set<Long> unitsId = new HashSet<>();
    private LocalDateTime lastLogin;
    private LocalDateTime created;
    private String role;

    public void addUnit(Long unitId) {
        if (unitsId == null) {
            unitsId = new HashSet<>();
        }
        unitsId.add(unitId);
    }

    public void removeUnit(Long unitId) {
        unitsId.remove(unitId);
    }
}
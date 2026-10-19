package com.coldchain.dashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;

@Entity
@Table(name = "cold_chain_events")
public class ColdChainEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String unitId;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @NotNull
    @DecimalMin(value = "-100.0")
    @DecimalMax(value = "100.0")
    @Column(nullable = false)
    private Double temperatureC;

    @NotNull
    @Column(nullable = false)
    private Double minTempC;

    @NotNull
    @Column(nullable = false)
    private Double maxTempC;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(length = 500)
    private String notes;

    public ColdChainEvent() {}

    @PrePersist
    public void beforeInsert() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }

    @Transient
    public String getStatus() {
        if (temperatureC == null || minTempC == null || maxTempC == null) return "UNKNOWN";
        if (temperatureC < minTempC) return "LOW";
        if (temperatureC > maxTempC) return "HIGH";
        return "NORMAL";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }
    public Double getMinTempC() { return minTempC; }
    public void setMinTempC(Double minTempC) { this.minTempC = minTempC; }
    public Double getMaxTempC() { return maxTempC; }
    public void setMaxTempC(Double maxTempC) { this.maxTempC = maxTempC; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

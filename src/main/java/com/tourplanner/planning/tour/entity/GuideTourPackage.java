package com.tourplanner.planning.tour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "guide_tour_package")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideTourPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "package_id", updatable = false, nullable = false)
    private UUID packageId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false, unique = true)
    private Tour tour;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "max_slots")
    private Integer maxSlots;

    @Column(name = "available_slots")
    private Integer availableSlots;

    @Column(name = "price_per_slot")
    private BigDecimal pricePerSlot;

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PackageStatus status = PackageStatus.DRAFT;
}

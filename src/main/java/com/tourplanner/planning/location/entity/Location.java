package com.tourplanner.planning.location.entity;

import com.tourplanner.planning.stop.entity.Stop;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "location_id", updatable = false, nullable = false)
    private UUID locationId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Stop> stops = new ArrayList<>();
}

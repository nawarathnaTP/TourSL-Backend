package com.tourplanner.planning.route.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transport_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportOption {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transport_id", updatable = false, nullable = false)
    private UUID transportId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String label;

    @OneToMany(mappedBy = "transportOption", cascade = CascadeType.ALL)
    @Builder.Default
    private List<RouteOption> routeOptions = new ArrayList<>();
}

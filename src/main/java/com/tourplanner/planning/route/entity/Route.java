package com.tourplanner.planning.route.entity;

import com.tourplanner.planning.stop.entity.Stop;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "route", uniqueConstraints = {
        @UniqueConstraint(name = "uq_route_start_end", columnNames = {"start_stop_id", "end_stop_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "route_id", updatable = false, nullable = false)
    private UUID routeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_stop_id", nullable = false)
    private Stop startStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_stop_id", nullable = false)
    private Stop endStop;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RouteOption> routeOptions = new ArrayList<>();
}

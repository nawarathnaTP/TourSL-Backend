package com.tourplanner.planning.stop.entity;

import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.route.entity.Route;
import com.tourplanner.planning.tour.entity.Day;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stops", uniqueConstraints = {
        @UniqueConstraint(name = "uq_stops_day_order", columnNames = {"day_id", "stop_order"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "stop_id", updatable = false, nullable = false)
    private UUID stopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private Day day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(nullable = false)
    private Integer duration;

    @OneToMany(mappedBy = "stop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Activity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "startStop")
    @Builder.Default
    private List<Route> routesFrom = new ArrayList<>();

    @OneToMany(mappedBy = "endStop")
    @Builder.Default
    private List<Route> routesTo = new ArrayList<>();
}

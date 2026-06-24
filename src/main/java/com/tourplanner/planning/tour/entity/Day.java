package com.tourplanner.planning.tour.entity;

import com.tourplanner.planning.stop.entity.Stop;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "days")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Day {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "day_id", updatable = false, nullable = false)
    private UUID dayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "day_no", nullable = false)
    private Integer dayNo;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "lodging_id")
    private UUID lodgingId;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Stop> stops = new ArrayList<>();
}

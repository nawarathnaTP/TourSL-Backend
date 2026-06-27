package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.tour.dto.DayRequest;
import com.tourplanner.planning.tour.dto.DayResponse;

import java.util.List;
import java.util.UUID;

public interface DayService {

    DayResponse getDayById(UUID dayId);

    List<DayResponse> getDaysByTourId(UUID tourId);

    DayResponse updateDay(UUID dayId, DayRequest request);

    DayResponse clearDay(UUID dayID);
}

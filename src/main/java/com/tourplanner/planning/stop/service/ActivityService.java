package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.stop.dto.ActivityRequest;
import com.tourplanner.planning.stop.dto.ActivityResponse;

import java.util.List;
import java.util.UUID;

public interface ActivityService {

    ActivityResponse addActivity(ActivityRequest request);

    ActivityResponse getActivityById(UUID activityId);

    List<ActivityResponse> getActivitiesByStopId(UUID stopId);

    ActivityResponse updateActivity(UUID activityId, ActivityRequest request);

    void deleteActivity(UUID activityId);
}

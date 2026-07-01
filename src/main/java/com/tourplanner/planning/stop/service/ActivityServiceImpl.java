package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.stop.dto.ActivityRequest;
import com.tourplanner.planning.stop.dto.ActivityResponse;
import com.tourplanner.planning.stop.entity.Activity;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.ActivityRepository;
import com.tourplanner.planning.stop.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final StopRepository stopRepository;

    @Override
    @Transactional
    public ActivityResponse addActivity(ActivityRequest request) {
        Stop stop = stopRepository.findById(request.getStopId())
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + request.getStopId()));

        Activity activity = Activity.builder()
                .stop(stop)
                .duration(request.getDuration())
                .description(request.getDescription())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        return mapToResponse(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(UUID activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));
        return mapToResponse(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivitiesByStopId(UUID stopId) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));

        return stop.getActivities().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ActivityResponse updateActivity(UUID activityId, ActivityRequest request) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));

        if (request.getDuration() != null) {
            activity.setDuration(request.getDuration());
        }

        if (request.getDescription() != null) {
            activity.setDescription(request.getDescription());
        }

        Activity savedActivity = activityRepository.save(activity);
        return mapToResponse(savedActivity);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));
        activityRepository.delete(activity);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        return ActivityResponse.builder()
                .activityId(activity.getActivityId())
                .stopId(activity.getStop().getStopId())
                .duration(activity.getDuration())
                .description(activity.getDescription())
                .bookingId(activity.getBookingId())
                .build();
    }
}

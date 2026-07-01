package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.stop.dto.ActivityRequest;
import com.tourplanner.planning.stop.dto.ActivityResponse;
import com.tourplanner.planning.stop.entity.Activity;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.ActivityRepository;
import com.tourplanner.planning.stop.repository.StopRepository;
import com.tourplanner.planning.tour.entity.Day;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private StopRepository stopRepository;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private UUID activityId;
    private UUID stopId;
    private Stop testStop;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        activityId = UUID.randomUUID();
        stopId = UUID.randomUUID();

        Day testDay = Day.builder()
                .dayId(UUID.randomUUID())
                .dayNo(1)
                .build();

        testStop = Stop.builder()
                .stopId(stopId)
                .day(testDay)
                .stopOrder(1)
                .duration(120)
                .activities(new ArrayList<>())
                .build();

        testActivity = Activity.builder()
                .activityId(activityId)
                .stop(testStop)
                .duration(60)
                .description("Visit temple")
                .bookingId(null)
                .build();
    }

    // Verifies that an activity is created with the correct stop, duration, and description
    @Test
    void addActivity_validRequest_returnsActivityResponse() {
        ActivityRequest request = ActivityRequest.builder()
                .stopId(stopId)
                .duration(60)
                .description("Visit temple")
                .build();

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setActivityId(activityId);
            return activity;
        });

        ActivityResponse response = activityService.addActivity(request);

        assertThat(response.getActivityId()).isEqualTo(activityId);
        assertThat(response.getStopId()).isEqualTo(stopId);
        assertThat(response.getDuration()).isEqualTo(60);
        assertThat(response.getDescription()).isEqualTo("Visit temple");

        verify(activityRepository).save(any(Activity.class));
    }

    // Verifies that adding an activity with a non-existent stop throws an exception
    @Test
    void addActivity_stopNotFound_throwsException() {
        UUID fakeStopId = UUID.randomUUID();
        ActivityRequest request = ActivityRequest.builder()
                .stopId(fakeStopId)
                .duration(60)
                .description("Test")
                .build();

        when(stopRepository.findById(fakeStopId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.addActivity(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that fetching an activity by ID returns the correct response
    @Test
    void getActivityById_existingActivity_returnsActivityResponse() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));

        ActivityResponse response = activityService.getActivityById(activityId);

        assertThat(response.getActivityId()).isEqualTo(activityId);
        assertThat(response.getStopId()).isEqualTo(stopId);
        assertThat(response.getDuration()).isEqualTo(60);
        assertThat(response.getDescription()).isEqualTo("Visit temple");
    }

    // Verifies that fetching a non-existent activity throws a RuntimeException
    @Test
    void getActivityById_nonExistingActivity_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(activityRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.getActivityById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found with id:");
    }

    // Verifies that all activities for a stop are returned
    @Test
    void getActivitiesByStopId_returnsActivityList() {
        Activity activity2 = Activity.builder()
                .activityId(UUID.randomUUID())
                .stop(testStop)
                .duration(30)
                .description("Take photos")
                .build();

        testStop.setActivities(List.of(testActivity, activity2));

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));

        List<ActivityResponse> responses = activityService.getActivitiesByStopId(stopId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescription()).isEqualTo("Visit temple");
        assertThat(responses.get(1).getDescription()).isEqualTo("Take photos");
    }

    // Verifies that an empty list is returned when a stop has no activities
    @Test
    void getActivitiesByStopId_noActivities_returnsEmptyList() {
        testStop.setActivities(new ArrayList<>());
        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));

        List<ActivityResponse> responses = activityService.getActivitiesByStopId(stopId);

        assertThat(responses).isEmpty();
    }

    // Verifies that querying activities for a non-existent stop throws an exception
    @Test
    void getActivitiesByStopId_stopNotFound_throwsException() {
        UUID fakeStopId = UUID.randomUUID();
        when(stopRepository.findById(fakeStopId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.getActivitiesByStopId(fakeStopId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that updating an activity's duration only changes the duration
    @Test
    void updateActivity_durationOnly_updatesDuration() {
        ActivityRequest request = ActivityRequest.builder()
                .duration(90)
                .build();

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityResponse response = activityService.updateActivity(activityId, request);

        assertThat(response.getDuration()).isEqualTo(90);
        assertThat(response.getDescription()).isEqualTo("Visit temple");
    }

    // Verifies that updating an activity's description only changes the description
    @Test
    void updateActivity_descriptionOnly_updatesDescription() {
        ActivityRequest request = ActivityRequest.builder()
                .description("Explore ruins")
                .build();

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityResponse response = activityService.updateActivity(activityId, request);

        assertThat(response.getDescription()).isEqualTo("Explore ruins");
        assertThat(response.getDuration()).isEqualTo(60);
    }

    // Verifies that null fields in the update request preserve the existing values
    @Test
    void updateActivity_nullFields_preservesExistingValues() {
        ActivityRequest request = ActivityRequest.builder().build();

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityResponse response = activityService.updateActivity(activityId, request);

        assertThat(response.getDuration()).isEqualTo(60);
        assertThat(response.getDescription()).isEqualTo("Visit temple");
    }

    // Verifies that updating a non-existent activity throws a RuntimeException
    @Test
    void updateActivity_nonExistingActivity_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        ActivityRequest request = ActivityRequest.builder().duration(30).build();

        when(activityRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.updateActivity(nonExistentId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found with id:");
    }

    // Verifies that an existing activity is deleted via the repository
    @Test
    void deleteActivity_existingActivity_deletesSuccessfully() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));

        activityService.deleteActivity(activityId);

        verify(activityRepository).delete(testActivity);
    }

    // Verifies that deleting a non-existent activity throws a RuntimeException
    @Test
    void deleteActivity_nonExistingActivity_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(activityRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.deleteActivity(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found with id:");
    }

    // Verifies that the bookingId is correctly mapped in the response
    @Test
    void getActivityById_withBookingId_mapsBookingId() {
        UUID bookingId = UUID.randomUUID();
        testActivity.setBookingId(bookingId);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));

        ActivityResponse response = activityService.getActivityById(activityId);

        assertThat(response.getBookingId()).isEqualTo(bookingId);
    }
}

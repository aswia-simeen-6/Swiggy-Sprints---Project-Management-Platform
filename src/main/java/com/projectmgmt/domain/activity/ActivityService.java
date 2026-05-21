package com.projectmgmt.domain.activity;

import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.domain.activity.dto.ActivityResponse;
import com.projectmgmt.domain.project.ProjectService;
import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<ActivityResponse> getProjectActivity(UUID projectId, UUID userId, ActivityType filterAction,
                                                      int page, int size) {
        projectService.validateMembership(projectId, userId);

        Slice<ActivityLog> activities;
        if (filterAction != null) {
            activities = activityRepository.findByProjectIdAndAction(projectId, filterAction, PageRequest.of(page, size));
        } else {
            activities = activityRepository.findByProjectId(projectId, PageRequest.of(page, size));
        }

        Set<UUID> userIds = new HashSet<>();
        activities.forEach(a -> userIds.add(a.getUserId()));
        Map<UUID, String> userNames = buildUserNameMap(userIds);

        return activities.stream()
                .map(a -> new ActivityResponse(
                        a.getId(), a.getProjectId(), a.getIssueId(), a.getUserId(),
                        userNames.get(a.getUserId()),
                        a.getAction(), a.getChanges(), a.getMetadata(), a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getIssueActivity(UUID issueId, UUID userId, int page, int size) {
        Slice<ActivityLog> activities = activityRepository.findByIssueId(issueId, PageRequest.of(page, size));

        Set<UUID> userIds = new HashSet<>();
        activities.forEach(a -> userIds.add(a.getUserId()));
        Map<UUID, String> userNames = buildUserNameMap(userIds);

        return activities.stream()
                .map(a -> new ActivityResponse(
                        a.getId(), a.getProjectId(), a.getIssueId(), a.getUserId(),
                        userNames.get(a.getUserId()),
                        a.getAction(), a.getChanges(), a.getMetadata(), a.getCreatedAt()))
                .toList();
    }

    private Map<UUID, String> buildUserNameMap(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<UUID, String> map = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> map.put(u.getId(), u.getDisplayName()));
        return map;
    }
}

package com.projectmgmt.domain.project;

import com.projectmgmt.common.exception.DuplicateResourceException;
import com.projectmgmt.common.exception.ForbiddenException;
import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.domain.project.dto.*;
import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import com.projectmgmt.domain.workflow.StatusCategory;
import com.projectmgmt.domain.workflow.WorkflowStatus;
import com.projectmgmt.domain.workflow.WorkflowStatusRepository;
import com.projectmgmt.domain.workflow.WorkflowTransition;
import com.projectmgmt.domain.workflow.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkflowStatusRepository statusRepository;
    private final WorkflowTransitionRepository transitionRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {
        if (projectRepository.existsByKeyAndDeletedAtIsNull(request.key())) {
            throw new DuplicateResourceException("Project", "key", request.key());
        }

        Project project = Project.builder()
                .name(request.name().trim())
                .key(request.key().toUpperCase().trim())
                .description(request.description())
                .ownerId(ownerId)
                .build();

        Project saved = projectRepository.save(project);

        // Add owner as ADMIN member
        ProjectMember ownerMember = ProjectMember.builder()
                .project(saved)
                .userId(ownerId)
                .role("ADMIN")
                .build();
        memberRepository.save(ownerMember);

        // Create default workflow (To Do → In Progress → In Review → Done)
        createDefaultWorkflow(saved.getId());

        log.info("Project created: {} ({}) by user {}", saved.getName(), saved.getKey(), ownerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        validateMembership(projectId, userId);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(UUID userId) {
        return projectRepository.findAllByMemberUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void addMember(UUID projectId, UUID userId, String role, UUID requesterId) {
        validateMembership(projectId, requesterId);
        if (memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new DuplicateResourceException("ProjectMember", "userId", userId);
        }
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .userId(userId)
                .role(role != null ? role : "MEMBER")
                .build();
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<MemberDetailResponse> getProjectMembers(UUID projectId, UUID requesterId) {
        validateMembership(projectId, requesterId);
        List<ProjectMember> members = memberRepository.findAllByProjectId(projectId);
        List<UUID> userIds = members.stream().map(ProjectMember::getUserId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream()
                .map(m -> {
                    User u = usersById.get(m.getUserId());
                    return new MemberDetailResponse(
                            m.getUserId(),
                            u != null ? u.getDisplayName() : "Unknown",
                            u != null ? u.getAvatarUrl() : null,
                            m.getRole());
                })
                .toList();
    }

    public void validateMembership(UUID projectId, UUID userId) {
        if (!memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ForbiddenException("You are not a member of this project");
        }
    }

    private void createDefaultWorkflow(UUID projectId) {
        WorkflowStatus todo = WorkflowStatus.builder().projectId(projectId).name("To Do").category(StatusCategory.TODO).position(0).color("#6B7280").build();
        WorkflowStatus inProgress = WorkflowStatus.builder().projectId(projectId).name("In Progress").category(StatusCategory.IN_PROGRESS).position(1).color("#3B82F6").build();
        WorkflowStatus inReview = WorkflowStatus.builder().projectId(projectId).name("In Review").category(StatusCategory.IN_PROGRESS).position(2).color("#F59E0B").build();
        WorkflowStatus done = WorkflowStatus.builder().projectId(projectId).name("Done").category(StatusCategory.DONE).position(3).color("#10B981").build();

        statusRepository.saveAll(List.of(todo, inProgress, inReview, done));

        transitionRepository.saveAll(List.of(
                WorkflowTransition.builder().projectId(projectId).fromStatusId(todo.getId()).toStatusId(inProgress.getId()).name("Start Work").build(),
                WorkflowTransition.builder().projectId(projectId).fromStatusId(inProgress.getId()).toStatusId(inReview.getId()).name("Submit for Review").build(),
                WorkflowTransition.builder().projectId(projectId).fromStatusId(inReview.getId()).toStatusId(done.getId()).name("Approve").build(),
                WorkflowTransition.builder().projectId(projectId).fromStatusId(inReview.getId()).toStatusId(inProgress.getId()).name("Request Changes").build(),
                WorkflowTransition.builder().projectId(projectId).fromStatusId(inProgress.getId()).toStatusId(todo.getId()).name("Move to Backlog").build()
        ));
    }

    private ProjectResponse toResponse(Project project) {
        List<ProjectMemberResponse> members = project.getMembers() != null
                ? project.getMembers().stream()
                    .map(m -> new ProjectMemberResponse(m.getId(), m.getUserId(), m.getRole(), m.getCreatedAt()))
                    .toList()
                : List.of();

        return new ProjectResponse(
                project.getId(), project.getName(), project.getKey(), project.getDescription(),
                project.getOwnerId(), project.getIssueCounter(), members,
                project.getCreatedAt(), project.getUpdatedAt(), project.getVersion());
    }
}
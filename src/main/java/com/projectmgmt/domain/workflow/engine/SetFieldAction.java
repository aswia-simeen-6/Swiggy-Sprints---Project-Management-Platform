package com.projectmgmt.domain.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.domain.issue.Issue;
import com.projectmgmt.domain.issue.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetFieldAction implements TransitionActionExecutor {

    private final IssueRepository issueRepository;

    @Override
    public String getType() {
        return "SET_FIELD";
    }

    @Override
    public void execute(Issue issue, JsonNode config, UUID triggeredBy) {
        String field = config.path("field").asText("");
        String value = config.path("value").asText("");

        // Handle special value "$trigger_user"
        if ("$trigger_user".equals(value)) {
            value = triggeredBy.toString();
        }

        switch (field) {
            case "assignee_id", "assigneeId" -> {
                issue.setAssigneeId(UUID.fromString(value));
                issueRepository.save(issue);
                log.debug("Set {} to {} on issue {}", field, value, issue.getIssueKey());
            }
            default -> log.warn("Unsupported field for SET_FIELD action: {}", field);
        }
    }
}

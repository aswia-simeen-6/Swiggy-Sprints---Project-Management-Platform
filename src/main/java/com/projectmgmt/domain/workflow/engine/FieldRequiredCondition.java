package com.projectmgmt.domain.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.common.exception.ValidationException;
import com.projectmgmt.domain.issue.Issue;
import org.springframework.stereotype.Component;

@Component
public class FieldRequiredCondition implements ConditionEvaluator {

    @Override
    public String getType() {
        return "FIELD_REQUIRED";
    }

    @Override
    public void evaluate(Issue issue, JsonNode config) {
        String field = config.path("field").asText("");
        String customMessage = config.path("message").asText(null);

        Object value = resolveFieldValue(issue, field);

        if (value == null || (value instanceof String s && s.isBlank())) {
            String message = customMessage != null
                    ? customMessage
                    : "Field '%s' is required for this transition".formatted(field);
            throw new ValidationException(message);
        }
    }

    private Object resolveFieldValue(Issue issue, String field) {
        return switch (field) {
            case "assignee_id", "assigneeId" -> issue.getAssigneeId();
            case "title" -> issue.getTitle();
            case "description" -> issue.getDescription();
            case "story_points", "storyPoints" -> issue.getStoryPoints();
            case "sprint_id", "sprintId" -> issue.getSprintId();
            case "priority" -> issue.getPriority();
            case "labels" -> issue.getLabels() != null && issue.getLabels().length > 0 ? issue.getLabels() : null;
            default -> {
                // Check custom fields
                if (issue.getCustomFields() != null && issue.getCustomFields().containsKey(field)) {
                    yield issue.getCustomFields().get(field);
                }
                yield null;
            }
        };
    }
}

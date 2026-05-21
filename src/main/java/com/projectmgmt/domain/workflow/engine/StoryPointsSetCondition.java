package com.projectmgmt.domain.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.common.exception.ValidationException;
import com.projectmgmt.domain.issue.Issue;
import org.springframework.stereotype.Component;

@Component
public class StoryPointsSetCondition implements ConditionEvaluator {

    @Override
    public String getType() {
        return "STORY_POINTS_SET";
    }

    @Override
    public void evaluate(Issue issue, JsonNode config) {
        if (issue.getStoryPoints() == null || issue.getStoryPoints() <= 0) {
            throw new ValidationException("Story points must be set and greater than 0 for this transition");
        }
    }
}

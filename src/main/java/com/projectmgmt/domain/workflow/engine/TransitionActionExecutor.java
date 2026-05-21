package com.projectmgmt.domain.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.domain.issue.Issue;

import java.util.UUID;

/**
 * Strategy interface for executing post-transition actions.
 * Each action type has a corresponding executor registered in ActionRegistry.
 */
public interface TransitionActionExecutor {

    String getType();

    void execute(Issue issue, JsonNode config, UUID triggeredBy);
}

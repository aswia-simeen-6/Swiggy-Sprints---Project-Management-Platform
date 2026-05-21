package com.projectmgmt.domain.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.domain.issue.Issue;

/**
 * Strategy interface for evaluating pre-transition conditions.
 * Each condition type has a corresponding evaluator registered in ConditionRegistry.
 * Adding a new condition = implementing this interface + registering in the registry.
 * No code changes to the WorkflowEngine itself.
 */
public interface ConditionEvaluator {

    /**
     * @return The condition type string that this evaluator handles (e.g., "FIELD_REQUIRED")
     */
    String getType();

    /**
     * Evaluate whether the condition is met for the given issue.
     * @param issue The issue being transitioned
     * @param config The condition configuration from the database (JSONB)
     * @throws com.projectmgmt.common.exception.ValidationException if the condition is not met
     */
    void evaluate(Issue issue, JsonNode config);
}

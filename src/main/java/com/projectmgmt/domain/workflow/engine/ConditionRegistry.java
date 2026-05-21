package com.projectmgmt.domain.workflow.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry that maps condition type strings to their evaluator implementations.
 * New condition types are auto-discovered via Spring's component scanning.
 */
@Slf4j
@Component
public class ConditionRegistry {

    private final Map<String, ConditionEvaluator> evaluators = new HashMap<>();

    public ConditionRegistry(List<ConditionEvaluator> conditionEvaluators) {
        for (ConditionEvaluator evaluator : conditionEvaluators) {
            evaluators.put(evaluator.getType(), evaluator);
            log.info("Registered workflow condition evaluator: {}", evaluator.getType());
        }
    }

    public ConditionEvaluator get(String type) {
        ConditionEvaluator evaluator = evaluators.get(type);
        if (evaluator == null) {
            log.warn("No condition evaluator registered for type: {}. Skipping.", type);
            return null;
        }
        return evaluator;
    }

    public boolean hasEvaluator(String type) {
        return evaluators.containsKey(type);
    }
}

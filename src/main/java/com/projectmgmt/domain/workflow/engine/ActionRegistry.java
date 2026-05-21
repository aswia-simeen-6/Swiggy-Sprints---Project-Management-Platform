package com.projectmgmt.domain.workflow.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ActionRegistry {

    private final Map<String, TransitionActionExecutor> executors = new HashMap<>();

    public ActionRegistry(List<TransitionActionExecutor> actionExecutors) {
        for (TransitionActionExecutor executor : actionExecutors) {
            executors.put(executor.getType(), executor);
            log.info("Registered workflow action executor: {}", executor.getType());
        }
    }

    public TransitionActionExecutor get(String type) {
        TransitionActionExecutor executor = executors.get(type);
        if (executor == null) {
            log.warn("No action executor registered for type: {}. Skipping.", type);
        }
        return executor;
    }
}

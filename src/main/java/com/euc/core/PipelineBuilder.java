package com.euc.core;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.AgentState;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Runs the EUC's execution requirements in declared order, resolving each
 * one to its implementation through an ExecutionFilterRegistry.
 *
 * There is no orchestration logic specific to any use case here: the EUC
 * says what must happen and in what order, the registry says what code
 * carries each step out, and this class does nothing but compile that list
 * into a graph and walk it. The engine is a LangGraph4j StateGraph — one node
 * per execution requirement, connected by conditional edges — but the graph
 * itself is built generically from euc.getExecutionRequirements(), so no
 * requirement id or business term appears here.
 *
 * A requirement whose outcome is FAILED and whose onFailure policy is HALT
 * routes straight to the graph's END instead of the next requirement. That is
 * the business contract being honoured — "strong alignment cannot overcome a
 * failed mandatory requirement" is enforced here because the EUC says so, not
 * because application code happens to be written that way.
 *
 * The graph's own state carries no business data: each ExecutionFilter still
 * reads and writes the PipelineContext instance passed into run(), exactly as
 * before. The graph only needs one routing signal (PASSED/FAILED) per node to
 * decide where to go next.
 */
public class PipelineBuilder {

    private static final String PIPELINE_CONTEXT = "pipelineContext";
    private static final String ROUTE = "route";
    private static final String CONTINUE = "continue";
    private static final String HALT = "halt";

    /** Carries only the routing signal between nodes; business data lives in PipelineContext. */
    private static final class RouteState extends AgentState {
        RouteState(Map<String, Object> initData) {
            super(initData);
        }
    }

    /**
     * LangGraph4j clones the state before every node call (and again for the
     * final result) — by default via a full Java-serialization round trip,
     * which would force PipelineContext, Organization, GrantOpportunity and
     * everything else a filter ever puts in the context to implement
     * Serializable for a graph library's benefit, and would hand each node a
     * *deserialized copy* rather than the original PipelineContext instance —
     * breaking the very mutation-by-reference semantics ExecutionFilter
     * implementations rely on. Overriding cloneObject() to do a shallow copy
     * of the state map avoids both problems: it's cheap, and a shallow copy
     * still holds the same PipelineContext reference, not a clone of it. The
     * writeData/readData pair below only exists to satisfy the abstract
     * contract; nothing in this class's usage (no checkpointing configured)
     * ever calls them.
     */
    private static final class RouteStateSerializer extends StateSerializer<RouteState> {
        RouteStateSerializer() {
            super(RouteState::new);
        }

        @Override
        public RouteState cloneObject(RouteState object) {
            return new RouteState(object.data());
        }

        @Override
        public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
            out.writeObject(data.get(ROUTE));
        }

        @Override
        public Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
            Object route = in.readObject();
            return route == null ? Map.of() : Map.of(ROUTE, route);
        }
    }

    private final CompiledGraph<RouteState> graph;

    public PipelineBuilder(EucDefinition euc, ExecutionFilterRegistry registry) {
        this.graph = buildGraph(euc, registry);
    }

    /** Carries out every execution requirement against the given context, in declared order. */
    public void run(PipelineContext context) {
        graph.invoke(Map.of(PIPELINE_CONTEXT, context))
                .orElseThrow(() -> new IllegalStateException("EUC pipeline graph produced no result"));
    }

    private static CompiledGraph<RouteState> buildGraph(EucDefinition euc, ExecutionFilterRegistry registry) {
        List<ExecutionRequirement> requirements = euc.getExecutionRequirements();
        try {
            StateGraph<RouteState> graph = new StateGraph<>(Map.of(), new RouteStateSerializer());
            graph.addEdge(START, requirements.get(0).getId());

            for (int i = 0; i < requirements.size(); i++) {
                ExecutionRequirement requirement = requirements.get(i);
                String nodeId = requirement.getId();
                String nextId = (i + 1 < requirements.size()) ? requirements.get(i + 1).getId() : END;

                graph.addNode(nodeId, node_async(state -> {
                    PipelineContext context = state.<PipelineContext>value(PIPELINE_CONTEXT)
                            .orElseThrow(() -> new IllegalStateException(
                                    "PipelineContext missing from graph state"));
                    ExecutionFilter filter = registry.get(requirement.getId());
                    ExecutionFilter.Outcome outcome = filter.execute(context, requirement);
                    return Map.of(ROUTE, outcome.name());
                }));

                graph.addConditionalEdges(nodeId, edge_async(state -> {
                    boolean shouldHalt = "FAILED".equals(state.<String>value(ROUTE).orElse("PASSED"))
                            && requirement.getOnFailure() == ExecutionRequirement.OnFailure.HALT;
                    return shouldHalt ? HALT : CONTINUE;
                }), Map.of(CONTINUE, nextId, HALT, END));
            }

            return graph.compile();
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to compile EUC '" + euc.getId() + "' into a pipeline graph", e);
        }
    }
}

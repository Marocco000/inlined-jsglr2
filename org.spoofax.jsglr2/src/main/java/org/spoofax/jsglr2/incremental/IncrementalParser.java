package org.spoofax.jsglr2.incremental;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.metaborg.util.iterators.Iterables2.stream;
import static org.spoofax.jsglr2.parser.observing.IParserObserver.BreakdownReason.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.actions.ActionType;
import org.metaborg.parsetable.actions.IAction;
import org.metaborg.parsetable.actions.IReduce;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.incremental.actions.GotoShift;
import org.spoofax.jsglr2.incremental.diff.IStringDiff;
import org.spoofax.jsglr2.incremental.diff.JGitHistogramDiff;
import org.spoofax.jsglr2.incremental.diff.ProcessUpdates;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForestManager;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IncrementalInputStackFactory;
import org.spoofax.jsglr2.parseforest.Disambiguator;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parseforest.ParseForestManagerFactory;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseReporterFactory;
import org.spoofax.jsglr2.parser.ParseStateFactory;
import org.spoofax.jsglr2.parser.Parser;
import org.spoofax.jsglr2.parser.failure.ParseFailureHandlerFactory;
import org.spoofax.jsglr2.reducing.ReduceManagerFactory;
import org.spoofax.jsglr2.stack.AbstractStackManager;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.StackManagerFactory;

public class IncrementalParser
// @formatter:off
   <StackNode     extends IStackNode,
    ParseState    extends AbstractParseState<IIncrementalInputStack, StackNode> & IIncrementalParseState,
    StackManager  extends AbstractStackManager<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState>,
    ReduceManager extends org.spoofax.jsglr2.reducing.ReduceManager<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, IIncrementalInputStack, ParseState>>
// @formatter:on
    extends
    Parser<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, IIncrementalInputStack, ParseState, StackManager, ReduceManager> {

    private final IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory;
    private final IStringDiff diff;
    private final ProcessUpdates<StackNode, ParseState> processUpdates;

    public IncrementalParser(IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory,
        ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack, StackNode, ParseState> parseStateFactory,
        IParseTable parseTable,
        StackManagerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState, StackManager> stackManagerFactory,
        ParseForestManagerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState> parseForestManagerFactory,
        Disambiguator<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState> disambiguator,
        ReduceManagerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, IIncrementalInputStack, ParseState, StackManager, ReduceManager> reduceManagerFactory,
        ParseFailureHandlerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState> failureHandlerFactory,
        ParseReporterFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, IIncrementalInputStack, ParseState> reporterFactory) {

        super(null, parseStateFactory, parseTable, stackManagerFactory, parseForestManagerFactory, disambiguator,
            reduceManagerFactory, failureHandlerFactory, reporterFactory);

        this.incrementalInputStackFactory = incrementalInputStackFactory;
        // TODO parametrize parser on diff algorithm for benchmarking
        this.diff = new JGitHistogramDiff();
        this.processUpdates =
            new ProcessUpdates<>((IncrementalParseForestManager<StackNode, ParseState>) parseForestManager);
    }

    @Override protected ParseState getParseState(JSGLR2Request request, String previousInput,
        IncrementalParseForest previousResult) {
        IncrementalParseForest updatedTree = previousInput != null && previousResult != null
            ? processUpdates.processUpdates(previousInput, previousResult, diff.diff(previousInput, request.input))
            : processUpdates.getParseNodeFromString(request.input);
        return parseStateFactory.get(request, incrementalInputStackFactory.get(updatedTree, request.input), observing);
    }

    // TODO this optimization does not work at the moment, as the `parseLoop` doesn't get passed the previousResult.
    // Replacing this check with `isReusable` is not reliable enough.
    // @Override protected void parseLoop(ParseState parse) {
    // // Optimization: if the first tree on the lookahead stack is exactly the same as the previous tree:
    // IncrementalParseForest rootNode = parse.inputStack.getNode();
    // if(rootNode.width() == parse.inputStack.length() && rootNode.isReusable()) {
    // StackNode stack = parse.activeStacks.getSingle();
    //
    // // Shift this previous tree
    // addForShifter(parse, stack, parseTable.getState(
    // stack.state().getGotoId(((IncrementalParseNode) rootNode).production().id())));
    // shifter(parse);
    // parse.inputStack.next();
    //
    // // Accept
    // actor(parse.activeStacks.getSingle(), parse, Accept.SINGLETON);
    // } else
    // super.parseLoop(parse);
    // }

    @Override protected void actor(StackNode stack, ParseState parseState) {
        Iterable<IAction> actions = getActions(stack, parseState);
        // Break down the lookahead in either of the following scenarios:
        // - The lookahead is not reusable (terminal nodes are always reusable).
        // - The lookahead is a non-terminal parse node AND there are no actions for it.
        // In the second case, do not break down if we already have something to shift.
        // This node that we can shift should not be broken down anymore:
        // - if we would, it would cause different shifts to be desynchronised;
        // - if a break-down of this node would cause different actions, it would already have been broken down because
        // that would mean that this node was created when the parser was in multiple states.
        while(!parseState.inputStack.getNode().isReusable()
            || !parseState.inputStack.getNode().isTerminal() && isEmpty(actions) && parseState.forShifter.isEmpty()) {
            observing.notify(observer -> {
                IncrementalParseForest node = parseState.inputStack.getNode();
                observer.breakDown(parseState.inputStack,
                    node instanceof IParseNode && ((IParseNode<?, ?>) node).production() == null ? TEMPORARY
                        : node.isReusable() ? node.isReusable(stack.state()) ? NO_ACTIONS : WRONG_STATE
                            : IRREUSABLE);
            });
            parseState.inputStack.breakDown();
            observing.notify(observer -> observer.parseRound(parseState, parseState.activeStacks));
            actions = getActions(stack, parseState);
        }

        if(size(actions) > 1)
            parseState.setMultipleStates(true);

        Iterable<IAction> finalActions = actions;
        observing.notify(observer -> observer.actor(stack, parseState, finalActions));

        for(IAction action : actions)
            actor(stack, parseState, action);
    }

    // Inside this method, we can assume that the lookahead is a valid and complete subtree of the previous parse.
    // Else, the loop in `actor` will have broken it down
    private Iterable<IAction> getActions(StackNode stack, ParseState parseState) {
        // Get actions based on the lookahead terminal that `parse` will calculate in actionQueryCharacter
        Iterable<IAction> actions = stack.state().getApplicableActions(parseState.inputStack, parseState.mode);

        IncrementalParseForest lookahead = parseState.inputStack.getNode();
        if(lookahead.isTerminal()) {
            return actions;
        } else {
            // Split in shift and reduce actions
            List<IAction> shiftActions =
                stream(actions).filter(a -> a.actionType() == ActionType.SHIFT).collect(Collectors.toList());

            // By default, only the reduce actions are returned
            List<IAction> result = stream(actions)
                .filter(a -> a.actionType() == ActionType.REDUCE || a.actionType() == ActionType.REDUCE_LOOKAHEAD)
                .collect(Collectors.toList());

            IncrementalParseNode lookaheadNode = (IncrementalParseNode) lookahead;

            // Only allow shifting the subtree if the saved state matches the current state
            boolean reusable = lookaheadNode.isReusable(stack.state());
            if(reusable) {
                // Reusable nodes have only one derivation, by definition
                result
                    .add(new GotoShift(stack.state().getGotoId(lookaheadNode.getFirstDerivation().production().id())));
            }

            // If we don't have a GotoShift action, but do have regular shift actions, we should break down further
            if(!reusable && !shiftActions.isEmpty()) {
                return Collections.emptyList(); // Return no actions, to trigger breakdown
            }

            // If lookahead has null yield and the production of lookahead matches the state of the GotoShift,
            // there is a duplicate action that can be removed (this is an optimization to avoid multipleStates == true)
            if(lookaheadNode.width() == 0 && result.size() == 2 && reusable
                && nullReduceMatchesGotoShift(stack, (IReduce) result.get(0), (GotoShift) result.get(1))) {
                result.remove(0); // Removes the unnecessary reduce action
            }

            return result;
        }
    }

    // If the lookahead has null yield, there are always at least two valid actions:
    // Either reduce a production with arity 0, or shift the already-existing null-yield subtree.
    // This method returns whether the Goto state of the Reduce action matches the state of the GotoShift action.
    private boolean nullReduceMatchesGotoShift(StackNode stack, IReduce reduceAction, GotoShift gotoShiftAction) {
        return stack.state().getGotoId(reduceAction.production().id()) == gotoShiftAction.shiftStateId();
    }

    @Override protected IncrementalParseForest getNodeToShift(ParseState parseState) {
        return parseState.inputStack.getNode();
    }
}

package org.spoofax.jsglr2.incremental;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.metaborg.util.iterators.Iterables2.stream;
import static org.spoofax.jsglr2.parser.observing.IParserObserver.BreakdownReason.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.actions.ActionType;
import org.metaborg.parsetable.actions.IAction;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.parsetable.actions.IShift;
import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.incremental.actions.GotoShift;
import org.spoofax.jsglr2.incremental.diff.IStringDiff;
import org.spoofax.jsglr2.incremental.diff.JGitHistogramDiff;
import org.spoofax.jsglr2.incremental.diff.ProcessUpdates;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForestManager;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.InputStackFactory;
import org.spoofax.jsglr2.inputstack.incremental.EagerIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parseforest.*;
import org.spoofax.jsglr2.parser.*;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseFailureCause;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.reducing.ReduceManagerFactory;
import org.spoofax.jsglr2.stack.AbstractStackManager;
import org.spoofax.jsglr2.stack.StackManagerFactory;
import org.spoofax.jsglr2.stack.collections.*;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode;

public class IncrementalParser2
// @formatter:off
        <
//                StackNode     extends IStackNode,
//                ParseState    extends AbstractParseState<IIncrementalInputStack, HybridStackNode<IncrementalParseForest>> & IIncrementalParseState,
                StackManager extends AbstractStackManager<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>>,
                ReduceManager extends org.spoofax.jsglr2.reducing.ReduceManager<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IIncrementalInputStack, IncrementalParseState<HybridStackNode<IncrementalParseForest>>>>
// @formatter:on
        implements IObservableParser<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> {
    //    public final IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory;
    public final IStringDiff diff;
    public final ProcessUpdates<HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> processUpdates;
    public final ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing;
    public final InputStackFactory<IIncrementalInputStack> inputStackFactory;
    //    public final ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack, HybridStackNode<IncrementalParseForest>, ParseState> parseStateFactory;
    public final ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> parseStateFactory;
    public final IParseTable parseTable;
    public final StackManager stackManager;
    public final ParseForestManager<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> parseForestManager;
    public final ReduceManager reduceManager;
//    public final IParseFailureHandler<IncrementalParseForest, StackNode, ParseState> failureHandler;
//    public final IParseReporter<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, IIncrementalInputStack, ParseState> reporter;


    public IncrementalParser2(
//                             ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack, HybridStackNode<IncrementalParseForest>, ParseState> parseStateFactory,
            IParseTable parseTable,
            StackManagerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>, StackManager> stackManagerFactory,
            ParseForestManagerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> parseForestManagerFactory,
//                             Disambiguator<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState> disambiguator,
            ReduceManagerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IIncrementalInputStack, IncrementalParseState<HybridStackNode<IncrementalParseForest>>, StackManager, ReduceManager> reduceManagerFactory
//                             ParseFailureHandlerFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, ParseState> failureHandlerFactory,
//                             ParseReporterFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode, IIncrementalInputStack, ParseState> reporterFactory
    ) {

        this.inputStackFactory = null;
        this.observing = new ParserObserving<>();
//        this.parseStateFactory = parseStateFactory;
        this.parseStateFactory = IncrementalParseState.factory(new ActiveStacksFactory(ActiveStacksRepresentation.ArrayList), new ForActorStacksFactory(ForActorStacksRepresentation.ArrayDeque));
        this.parseTable = parseTable;
        this.stackManager = stackManagerFactory.get(observing);
//        this.stackManager = new HybridStackManager<>(observing); TODO ??
        this.parseForestManager = parseForestManagerFactory.get(observing, null);
        this.reduceManager = reduceManagerFactory.get(parseTable, stackManager, parseForestManager);
//        this.failureHandler = failureHandlerFactory.get(observing);
//        this.failureHandler = new DefaultParseFailureHandler(observing);
//        this.reporter = reporterFactory.get(parseForestManager);

//        this.incrementalInputStackFactory = incrementalInputStackFactory;
        // TODO parametrize parser on diff algorithm for benchmarking
        this.diff = new JGitHistogramDiff();
        this.processUpdates =
                new ProcessUpdates<>((IncrementalParseForestManager<HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>>) parseForestManager);
    }

    public ParseResult<IncrementalParseForest> parse(JSGLR2Request request, String previousInput,
                                                     IncrementalParseForest previousResult) {

        // Get parse state
        IncrementalParseForest updatedTree = previousInput != null && previousResult != null
                ? processUpdates.processUpdates(previousInput, previousResult, diff.diff(previousInput, request.input))
                : processUpdates.getParseNodeFromString(request.input);

        IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState = new IncrementalParseState<>(request,
                new EagerIncrementalInputStack(updatedTree, request.input),
                new ActiveStacksArrayList<>(observing),
                new ForActorStacksArrayDeque<>(observing));



        observing.notify(observer -> observer.parseStart(parseState));

        HybridStackNode<IncrementalParseForest> initialStackNode = stackManager.createInitialStackNode(parseTable.getStartState());

        parseState.activeStacks.add(initialStackNode);

        boolean recover;

        try {
            do {
                parseLoop(parseState);

                if (parseState.acceptingStack == null)
                    recover = false;
                else
                    recover = false;
            } while (recover);

            if (parseState.acceptingStack != null) {
                IncrementalParseForest parseForest =
                        stackManager.findDirectLink(parseState.acceptingStack, initialStackNode).parseForest;

                IncrementalParseForest parseForestWithStartSymbol = request.startSymbol != null
                        ? parseForestManager.filterStartSymbol(parseForest, request.startSymbol, parseState) : parseForest;

                if (parseForest != null && parseForestWithStartSymbol == null)
                    return failure(parseState, new ParseFailureCause(ParseFailureCause.Type.InvalidStartSymbol));
                else
                    return complete(parseState, parseForestWithStartSymbol);
            } else {
                Position position = parseState.inputStack.safePosition();
                if (parseState.inputStack.offset() < parseState.inputStack.length())
                    return failure(parseState, new ParseFailureCause(ParseFailureCause.Type.UnexpectedInput, position));
                else
                    return failure(parseState, new ParseFailureCause(ParseFailureCause.Type.UnexpectedEOF, position));
            }
        } catch (ParseException e) {
            return failure(parseState, e.cause);
        }
    }

    @Override
    public void visit(ParseSuccess<?> success, ParseNodeVisitor<?, ?, ?> visitor) {
        parseForestManager.visit(success.parseState.request, (IncrementalParseForest) success.parseResult,
                (ParseNodeVisitor<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode>) visitor);
    }

//    public IncrementalParseState<HybridStackNode<IncrementalParseForest>> getParseState(JSGLR2Request request, String previousInput,
//                                                                                        IncrementalParseForest previousResult) {
//        IncrementalParseForest updatedTree = previousInput != null && previousResult != null
//                ? processUpdates.processUpdates(previousInput, previousResult, diff.diff(previousInput, request.input))
//                : processUpdates.getParseNodeFromString(request.input);
//        return new IncrementalParseState<>(request,
//                new EagerIncrementalInputStack(updatedTree, request.input),
//                new ActiveStacksArrayList<>(observing),
//                new ForActorStacksArrayDeque<>(observing));
//    }

    public ParseResult<IncrementalParseForest> complete(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState, IncrementalParseForest parseForest) {
        List<Message> messages = new ArrayList<>();
        CycleDetector<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode> cycleDetector = new CycleDetector<>(messages);

        parseForestManager.visit(parseState.request, parseForest, cycleDetector);

        if (cycleDetector.cycleDetected()) {
            return failure(new ParseFailure<>(parseState, cycleDetector.failureCause));
        } else {
            //reporter.report(parseState, parseForest, messages);

            // Generate errors for non-assoc or non-nested productions that are used associatively
            parseForestManager.visit(parseState.request, parseForest, new NonAssocDetector<>(messages));

            if (parseState.request.reportAmbiguities) {
                // Generate warnings for ambiguous parse nodes
                parseForestManager.visit(parseState.request, parseForest,
                        new AmbiguityDetector<>(parseState.inputStack.inputString(), messages));
            }

            ParseSuccess<IncrementalParseForest> success = new ParseSuccess<>(parseState, parseForest, messages);

            observing.notify(observer -> observer.success(success));

            return success;
        }
    }

    public ParseFailure<IncrementalParseForest> failure(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState, ParseFailureCause failureCause) {
        return failure(new ParseFailure<>(parseState, failureCause));
    }

    public ParseFailure<IncrementalParseForest> failure(ParseFailure<IncrementalParseForest> failure) {
        observing.notify(observer -> observer.failure(failure));

        return failure;
    }

    public void parseLoop(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) throws ParseException {
        while (parseState.inputStack.hasNext() && !parseState.activeStacks.isEmpty()) {
            parseCharacter(parseState);
            parseState.inputStack.consumed();

            if (!parseState.activeStacks.isEmpty())
                parseState.inputStack.next();
        }
    }

    public void parseCharacter(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) throws ParseException {
        parseState.nextParseRound(observing);

        parseState.activeStacks.addAllTo(parseState.forActorStacks);

        observing.notify(observer -> observer.forActorStacks(parseState.forActorStacks));

        processForActorStacks(parseState);

        shifter(parseState);
    }

    public void processForActorStacks(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) {
        while (parseState.forActorStacks.nonEmpty()) {
            HybridStackNode<IncrementalParseForest> stack = parseState.forActorStacks.remove();

            observing.notify(observer -> observer.handleForActorStack(stack, parseState.forActorStacks));

            if (!stack.allLinksRejected())
                actor(stack, parseState);
            else
                observing.notify(observer -> observer.skipRejectedStack(stack));
        }
    }


    public void actor(HybridStackNode<IncrementalParseForest> stack, IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) {
        Iterable<IAction> actions = getActions(stack, parseState);
        // Break down the lookahead in either of the following scenarios:
        // - The lookahead is not reusable (terminal nodes are always reusable).
        // - The lookahead is a non-terminal parse node AND there are no actions for it.
        // In the second case, do not break down if we already have something to shift.
        // This node that we can shift should not be broken down anymore:
        // - if we would, it would cause different shifts to be desynchronised;
        // - if a break-down of this node would cause different actions, it would already have been broken down because
        // that would mean that this node was created when the parser was in multiple states.
        while (!parseState.inputStack.getNode().isReusable()
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

        if (size(actions) > 1)
            parseState.setMultipleStates(true);

        Iterable<IAction> finalActions = actions;
        observing.notify(observer -> observer.actor(stack, parseState, finalActions));

        for (IAction action : actions)
            actor(stack, parseState, action);
    }

    // Inside this method, we can assume that the lookahead is a valid and complete subtree of the previous parse.
    // Else, the loop in `actor` will have broken it down
    private Iterable<IAction> getActions(HybridStackNode<IncrementalParseForest> stack, IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) {
        // Get actions based on the lookahead terminal that `parse` will calculate in actionQueryCharacter
        Iterable<IAction> actions = stack.state().getApplicableActions(parseState.inputStack, parseState.mode);

        IncrementalParseForest lookahead = parseState.inputStack.getNode();
        if (lookahead.isTerminal()) {
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
            if (reusable) {
                // Reusable nodes have only one derivation, by definition
                result
                        .add(new GotoShift(stack.state().getGotoId(lookaheadNode.getFirstDerivation().production().id())));
            }

            // If we don't have a GotoShift action, but do have regular shift actions, we should break down further
            if (!reusable && !shiftActions.isEmpty()) {
                return Collections.emptyList(); // Return no actions, to trigger breakdown
            }

            // If lookahead has null yield and the production of lookahead matches the state of the GotoShift,
            // there is a duplicate action that can be removed (this is an optimization to avoid multipleStates == true)
            if (lookaheadNode.width() == 0 && result.size() == 2 && reusable
                    && nullReduceMatchesGotoShift(stack, (IReduce) result.get(0), (GotoShift) result.get(1))) {
                result.remove(0); // Removes the unnecessary reduce action
            }

            return result;
        }
    }

    public void actor(HybridStackNode<IncrementalParseForest> stack, IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState, IAction action) {
        switch (action.actionType()) {
            case SHIFT:
                IShift shiftAction = (IShift) action;
                IState shiftState = parseTable.getState(shiftAction.shiftStateId());

                addForShifter(parseState, stack, shiftState);

                break;
            case REDUCE:
            case REDUCE_LOOKAHEAD: // Lookahead is checked while retrieving applicable actions from the state
                IReduce reduceAction = (IReduce) action;

                reduceManager.doReductions(observing, parseState, stack, reduceAction);

                break;
            case ACCEPT:
                parseState.acceptingStack = stack;

                observing.notify(observer -> observer.accept(stack));

                break;
        }
    }

    public void shifter(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) {
        parseState.activeStacks.clear();

        IncrementalParseForest characterNode = getNodeToShift(parseState);

        observing.notify(observer -> observer.shifter(characterNode, parseState.forShifter));

        for (ForShifterElement<HybridStackNode<IncrementalParseForest>> forShifterElement : parseState.forShifter) {
            HybridStackNode<IncrementalParseForest> gotoStack = parseState.activeStacks.findWithState(forShifterElement.state);

            if (gotoStack != null) {
                stackManager.createStackLink(parseState, gotoStack, forShifterElement.stack, characterNode);
            } else {
                gotoStack = stackManager.createStackNode(forShifterElement.state);

                stackManager.createStackLink(parseState, gotoStack, forShifterElement.stack, characterNode);

                parseState.activeStacks.add(gotoStack);
            }

            HybridStackNode<IncrementalParseForest> finalGotoStack = gotoStack;
            observing.notify(observer -> observer.shift(parseState, forShifterElement.stack, finalGotoStack));
        }

        parseState.forShifter.clear();
    }

    // If the lookahead has null yield, there are always at least two valid actions:
    // Either reduce a production with arity 0, or shift the already-existing null-yield subtree.
    // This method returns whether the Goto state of the Reduce action matches the state of the GotoShift action.
    private boolean nullReduceMatchesGotoShift(HybridStackNode<IncrementalParseForest> stack, IReduce reduceAction, GotoShift gotoShiftAction) {
        return stack.state().getGotoId(reduceAction.production().id()) == gotoShiftAction.shiftStateId();
    }

    public IncrementalParseForest getNodeToShift(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) {
        return parseState.inputStack.getNode();
    }

    public void addForShifter(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState, HybridStackNode<IncrementalParseForest> stack, IState shiftState) {
        ForShifterElement<HybridStackNode<IncrementalParseForest>> forShifterElement = new ForShifterElement<>(stack, shiftState);

        observing.notify(observer -> observer.addForShifter(forShifterElement));

        parseState.forShifter.add(forShifterElement);
    }

    @Override
    public ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing() {
        return observing;
    }
}

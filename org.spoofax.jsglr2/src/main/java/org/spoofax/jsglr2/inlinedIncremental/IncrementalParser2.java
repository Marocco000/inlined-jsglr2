package org.spoofax.jsglr2.inlinedIncremental;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.metaborg.util.iterators.Iterables2.stream;
import static org.spoofax.jsglr2.incremental.EditorUpdate.Type.INSERTION;
import static org.spoofax.jsglr2.incremental.EditorUpdate.Type.REPLACEMENT;
import static org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode.NO_STATE;

import java.util.*;
import java.util.stream.Collectors;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.actions.ActionType;
import org.metaborg.parsetable.actions.IAction;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.parsetable.actions.IShift;
import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.incremental.EditorUpdate;
import org.spoofax.jsglr2.incremental.actions.GotoShift;
import org.spoofax.jsglr2.incremental.diff.IStringDiff;
import org.spoofax.jsglr2.incremental.diff.JGitHistogramDiff;
import org.spoofax.jsglr2.incremental.parseforest.*;
import org.spoofax.jsglr2.inputstack.incremental.EagerIncrementalInputStack;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parseforest.*;
import org.spoofax.jsglr2.parser.*;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseFailureCause;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.stack.IStackNode;

public class IncrementalParser2 implements IParser<IncrementalParseForest> {

    public final IParseTable parseTable;

    public final IStringDiff diff;

    public IncrementalParser2(IParseTable parseTable) {
        this.parseTable = parseTable;

        // TODO parametrize parser on diff algorithm for benchmarking
        this.diff = new JGitHistogramDiff();
 }

    public ParseResult<IncrementalParseForest> parse(JSGLR2Request request, String previousInput,
                                                     IncrementalParseForest previousResult) {

        // Get parse state
        IncrementalParseForest updatedTree = previousInput != null && previousResult != null
                ? processUpdates(previousInput, previousResult, diff.diff(previousInput, request.input))
                : getParseNodeFromString(request.input);

        IncrementalParseState2 parseState = new IncrementalParseState2(request,
                new EagerIncrementalInputStack(updatedTree, request.input),
//                new ActiveStacksArrayList2(observers),
//                new ForActorStacksArrayDeque2(observers));
                new ActiveStacksArrayList2(),
                new ForActorStacksArrayDeque2());


//        notify(observer -> observer.parseStart(parseState));

        // create stack node
        HybridStackNode2 newStackNode = new HybridStackNode2(parseTable.getStartState());
        // notify new stack node
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createStackNode(newStackNode)).notify(observer1);
//        }

        HybridStackNode2 initialStackNode = newStackNode;

        parseState.activeStacks.add(initialStackNode);

        boolean recover;

        try {
            do {
//                parseLoop(parseState);
                while (parseState.inputStack.hasNext() && !parseState.activeStacks.isEmpty()) {
                    // parse character
                    parseState.nextParseRound();
                    parseState.activeStacks.addAllTo(parseState.forActorStacks);
//        notify(observer -> observer.forActorStacks(parseState.forActorStacks));
                    processForActorStacks(parseState);
                    shifter(parseState);

                    parseState.inputStack.consumed();

                    if (!parseState.activeStacks.isEmpty())
                        parseState.inputStack.next();
                }

                if (parseState.acceptingStack == null)
                    recover = false;
                else
                    recover = false;
            } while (recover);

            if (parseState.acceptingStack != null) {
                StackLink2 result = null;
                // find direct link (loop on stackLinksOut)
                for (StackLink2 link : parseState.acceptingStack.getLinks()) {
                    if (link.to == initialStackNode) {
                        result = link;
                        break;
                    }
                }

                IncrementalParseForest parseForest =
                        result.parseForest;

                IncrementalParseForest parseForestWithStartSymbol;
                if ( request.startSymbol != null) {
                    // Filter start symbol
                    IncrementalParseNode topNode = (IncrementalParseNode) parseForest;
                    List<IncrementalDerivation> derivationsWithStartSymbol = new ArrayList<>();

                    for (IncrementalDerivation derivation : topNode.getDerivations()) {
                        String derivationStartSymbol = derivation.production().startSymbolSort();

                        if (derivationStartSymbol != null && derivationStartSymbol.equals(request.startSymbol))
                            derivationsWithStartSymbol.add(derivation);
                    }

                    if (derivationsWithStartSymbol.isEmpty())
                        parseForestWithStartSymbol = null;
                    else {
                        IncrementalParseNode topParseNode =
                                new IncrementalParseNode(topNode.production(), derivationsWithStartSymbol.get(0), NO_STATE);

                        for (int i = 1; i < derivationsWithStartSymbol.size(); i++)
                            topParseNode.addDerivation(derivationsWithStartSymbol.get(i));

                        parseForestWithStartSymbol = topParseNode;
                    }
                } else {
                    parseForestWithStartSymbol = parseForest;
                }


                if (parseForest != null && parseForestWithStartSymbol == null)
                    return new ParseFailure<>(parseState, new ParseFailureCause(ParseFailureCause.Type.InvalidStartSymbol));
                else
                    return complete(parseState, parseForestWithStartSymbol);
            } else {
                Position position = parseState.inputStack.safePosition();
                if (parseState.inputStack.offset() < parseState.inputStack.length())
                    return new ParseFailure<>(parseState, new ParseFailureCause(ParseFailureCause.Type.UnexpectedInput, position));
                else
                    return new ParseFailure<>(parseState, new ParseFailureCause(ParseFailureCause.Type.UnexpectedEOF, position));
            }
        } catch (ParseException e) {
            return new ParseFailure<>(parseState, e.cause);
        }
    }

    @Override
    public void visit(ParseSuccess<?> success, ParseNodeVisitor<?, ?, ?> visitor) {
        visit(success.parseState.request, (IncrementalParseForest) success.parseResult,
                (ParseNodeVisitor<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode>) visitor);
    }

    public ParseResult<IncrementalParseForest> complete(IncrementalParseState2 parseState, IncrementalParseForest parseForest) {
        List<Message> messages = new ArrayList<>();
        CycleDetector<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode> cycleDetector = new CycleDetector<>(messages);

        visit(parseState.request, parseForest, cycleDetector);

        if (cycleDetector.cycleDetected()) {
            return new ParseFailure<>(parseState, cycleDetector.failureCause);
        } else {
            //reporter.report(parseState, parseForest, messages);

            // Generate errors for non-assoc or non-nested productions that are used associatively
            visit(parseState.request, parseForest, new NonAssocDetector<>(messages));

            if (parseState.request.reportAmbiguities) {
                // Generate warnings for ambiguous parse nodes
                visit(parseState.request, parseForest,
                        new AmbiguityDetector<>(parseState.inputStack.inputString(), messages));
            }

            ParseSuccess<IncrementalParseForest> success = new ParseSuccess<>(parseState, parseForest, messages);

//            notify(observer -> observer.success(success));

            return success;
        }
    }

//    public ParseFailure<IncrementalParseForest> failure(IncrementalParseState2 parseState, ParseFailureCause failureCause) {
//        return new ParseFailure<>(parseState, failureCause);
//    }

//    public ParseFailure<IncrementalParseForest> failure(ParseFailure<IncrementalParseForest> failure) {
////        notify(observer -> observer.failure(failure));
//
//        return failure;
//    }

//    public void parseLoop(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) throws ParseException {
//        while (parseState.inputStack.hasNext() && !parseState.activeStacks.isEmpty()) {
//            parseCharacter(parseState);
//            parseState.inputStack.consumed();
//
//            if (!parseState.activeStacks.isEmpty())
//                parseState.inputStack.next();
//        }
//    }

//    public void parseCharacter(IncrementalParseState2 parseState) throws ParseException {
//        // parse character
//        parseState.nextParseRound();
//
//        parseState.activeStacks.addAllTo(parseState.forActorStacks);
//
////        notify(observer -> observer.forActorStacks(parseState.forActorStacks));
//
//        processForActorStacks(parseState);
//
//        shifter(parseState);
//    }

    public void processForActorStacks(IncrementalParseState2 parseState) {
        while (parseState.forActorStacks.nonEmpty()) {
            HybridStackNode2 stack = parseState.forActorStacks.remove();

//            notify(observer -> observer.handleForActorStack(stack, parseState.forActorStacks));

            if (!stack.allLinksRejected())
                actor(stack, parseState);
//            else
//                notify(observer -> observer.skipRejectedStack(stack));
        }
    }


    public void actor(HybridStackNode2 stack, IncrementalParseState2 parseState) {
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
//            notify(observer -> {
//                IncrementalParseForest node = parseState.inputStack.getNode();
//                observer.breakDown(parseState.inputStack,
//                        node instanceof IParseNode && ((IParseNode<?, ?>) node).production() == null ? TEMPORARY
//                                : node.isReusable() ? node.isReusable(stack.state()) ? NO_ACTIONS : WRONG_STATE
//                                : IRREUSABLE);
//            });
            parseState.inputStack.breakDown();
//            notify(observer -> observer.parseRound(parseState, parseState.activeStacks));
            actions = getActions(stack, parseState);
        }

        if (size(actions) > 1)
            parseState.setMultipleStates(true);

        Iterable<IAction> finalActions = actions;
//        notify(observer -> observer.actor(stack, parseState, finalActions));

        for (IAction action : actions)
            actor(stack, parseState, action);
    }

    // Inside this method, we can assume that the lookahead is a valid and complete subtree of the previous parse.
    // Else, the loop in `actor` will have broken it down
    private Iterable<IAction> getActions(HybridStackNode2 stack, IncrementalParseState2 parseState) {
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
//                    && nullReduceMatchesGotoShift(stack, (IReduce) result.get(0), (GotoShift) result.get(1))
                    && stack.state().getGotoId(((IReduce) result.get(0)).production().id()) == ((GotoShift) result.get(1)).shiftStateId()) {
                result.remove(0); // Removes the unnecessary reduce action
            }

            return result;
        }
    }

    public void actor(HybridStackNode2 stack, IncrementalParseState2 parseState, IAction action) {
        switch (action.actionType()) {
            case SHIFT:
                IShift shiftAction = (IShift) action;
                IState shiftState = parseTable.getState(shiftAction.shiftStateId());

                // add for shifter
                ForShifterElement<HybridStackNode2> forShifterElement = new ForShifterElement<>(stack, shiftState);

//                notify(observer1 -> observer1.addForShifter(forShifterElement));

                parseState.forShifter.add(forShifterElement);

                break;
            case REDUCE:
            case REDUCE_LOOKAHEAD: // Lookahead is checked while retrieving applicable actions from the state
                IReduce reduceAction = (IReduce) action;
                // do reduction
//                reduceManager.doReductions(observing, parseState, stack, reduceAction);

                if (!(reduceAction.production().isRecovery() || reduceAction.production().isCompletion())) {

//                    notify(observer -> observer.doReductions(parseState, stack, reduceAction));

//                    reduceManager.doReductionsHelper(observing, parseState, stack, reduceAction, null);
                    // find all paths of a given length
                    List<StackPath2> paths = new ArrayList<>();
                    StackPath2 pathsOrigin = new EmptyStackPath2(stack);

                    findAllPathsOfLength(pathsOrigin, reduceAction.arity(), paths);


                    if (paths.size() > 1)
                        parseState.setMultipleStates(true);

                    for (StackPath2 path : paths) {
                        HybridStackNode2 originStack = path.head();
                        // get parse forests
                        IncrementalParseForest[] parseNodes = new IncrementalParseForest[path.length];

                        if (parseNodes != null) {
                            StackPath2 path1 = path;

                            for (int i = 0; i < path.length; i++) {
                                NonEmptyStackPath2 nonEmptyPath =
                                        (NonEmptyStackPath2) path1;

                                parseNodes[i] = nonEmptyPath.link.parseForest;

                                path1 = nonEmptyPath.tail;
                            }
                        }

//                        reducer(observing, parseState, stack, originStack, reduceAction, parseNodes);
                        reducer( parseState, stack, originStack, reduceAction, parseNodes);
                    }

                }
                break;
            case ACCEPT:
                parseState.acceptingStack = stack;

//                notify(observer -> observer.accept(stack));

                break;
        }

    }

    /**
     * Perform a reduction for the given reduce action and parse forests. The reduce action contains which production
     * will be reduced and the parse forests represent the right hand side of this production. The reduced derivation
     * will end up on a stack link from the given stack to a stack with the goto state. The latter can already exist or
     * not and if such an active stack already exists, the link to it can also already exist. Based on the existence of
     * the stack with the goto state and the link to it, different actions are performed.
     */
    // keep as is
    public void reducer(
//            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing,
            IncrementalParseState2 parseState,
            HybridStackNode2 activeStack,
            HybridStackNode2 originStack, IReduce reduce,
            IncrementalParseForest[] parseForests) {
        int gotoId = originStack.state().getGotoId(reduce.production().id());
        IState gotoState = parseTable.getState(gotoId);

        HybridStackNode2 gotoStack = parseState.activeStacks.findWithState(gotoState);

        if (gotoStack != null) {
            StackLink2 directLink = null;
            // find direct link (loop on stackLinksOut)
            for (StackLink2 link1 : gotoStack.getLinks()) {
                if (link1.to == originStack) {
                    directLink = link1;
                    break;
                }
            }

            StackLink2 finalDirectLink = directLink;
//            notify(observer -> observer.directLinkFound(parseState, finalDirectLink));

            if (directLink != null) {
                // reducer existing stack with direct link
                @SuppressWarnings("unchecked") IncrementalParseNode parseNode =
                        (IncrementalParseNode) directLink.parseForest;

                if (reduce.isRejectProduction()) {
                    // reject link
                    directLink.reject();

                    // notify reject link
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers) {
//                            StackLink2 finalDirectLink1 = directLink;
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.rejectStackLink(finalDirectLink1)).notify(observer1);
//                        }
//                    }
                }
                else if (!directLink.isRejected()
                        && !reduce.production().isSkippableInParseForest()) {
                    IProduction production = reduce.production();

                    // create derivation (from parseForestManager)
                    IncrementalDerivation derivation = new IncrementalDerivation(production, reduce.productionType(), parseForests);

                    // notify observers on created derivation
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createDerivation(derivation, production, derivation.parseForests)).notify(observer1);
//                    }

                    // notify observers on added derivation
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode, derivation)).notify(observer1);
//                    }

                    // add derivation
                    parseNode.addDerivation(derivation);

                }
            } else {
                // reducer Existing Stack without direct link
                StackLink2 newDirectLinkToActiveStateWithGoto;

                if (reduce.isRejectProduction()) {

                    // create stack link (with skipped node)
                    newDirectLinkToActiveStateWithGoto = new StackLink2(gotoStack, originStack, new IncrementalSkippedNode(reduce.production(), parseForests));
                    gotoStack.addLink(newDirectLinkToActiveStateWithGoto); // ad link

                    // notify created stack link
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer11 : observers)
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer2 -> observer2.createStackLink(newDirectLinkToActiveStateWithGoto)).notify(observer11);
//                    }

                    // reject link
                    newDirectLinkToActiveStateWithGoto.reject();

                    // notify reject link
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.rejectStackLink(newDirectLinkToActiveStateWithGoto)).notify(observer1);
//                    }
                } else {
                    // get parse node
                    IncrementalParseNode parseNode;

                    // if skip parse node creation?
                    if (reduce.production().isSkippableInParseForest())
                        parseNode = new IncrementalSkippedNode(reduce.production(), parseForests);
                    else {
                        IProduction production1 = reduce.production();

                        // create derivation (from parseForestManager)
                        IncrementalDerivation derivation = new IncrementalDerivation(production1, reduce.productionType(), parseForests);

                        // notify observers on created derivation
//                        if(!observers.isEmpty()) {
//                            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer11 : observers)
//                                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer2 -> observer2.createDerivation(derivation, production1, derivation.parseForests)).notify(observer11);
//                        }

                        IProduction production = reduce.production();

                        // create parse node
                        IState state = parseState.newParseNodesAreReusable() ? ((IStackNode) originStack).state() : NO_STATE;
                        IncrementalParseNode parseNode1 = new IncrementalParseNode(production, derivation, state);

                        // notify parse node creation
//                        if(!observers.isEmpty())
//                            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers) {
//                                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createParseNode(parseNode1, production)).notify(observer1);
//                                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode1, derivation)).notify(observer1);
//                            }

                        parseNode = parseNode1;
                    }

                    // create stack link
                    newDirectLinkToActiveStateWithGoto = new StackLink2(gotoStack, originStack, parseNode);
                    gotoStack.addLink(newDirectLinkToActiveStateWithGoto); // ad link

                    // notify created stack link
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createStackLink(newDirectLinkToActiveStateWithGoto)).notify(observer1);
//                    }
                }

                for (HybridStackNode2 activeStackForLimitedReductions : parseState.activeStacks
                        .forLimitedReductions(parseState.forActorStacks)) {
                    for (IReduce reduceAction : activeStackForLimitedReductions.state()
                            .getApplicableReduceActions(parseState.inputStack, parseState.mode)) {
//                        reduceManager.doLimitedReductions(observing, parseState, activeStackForLimitedReductions, reduceAction, link);

                        if (!(reduceAction.production().isRecovery() || reduceAction.production().isCompletion())) {
                            // if ! ignore reduce action
//                            notify(observer -> observer.doLimitedReductions(parseState, activeStackForLimitedReductions, reduceAction, newDirectLinkToActiveStateWithGoto));

                            // do reductionshelper
//                        reduceManager.doReductionsHelper(observing, parseState, activeStackForLimitedReductions, reduceAction, link);

                            // find all paths of a given length
                            List<StackPath2> paths = new ArrayList<>();
                            StackPath2 pathsOrigin = new EmptyStackPath2(activeStackForLimitedReductions);

                            findAllPathsOfLength(pathsOrigin, reduceAction.arity(), paths);


                            if (newDirectLinkToActiveStateWithGoto != null)
                                paths = paths.stream().filter(path -> path.contains(newDirectLinkToActiveStateWithGoto)).collect(Collectors.toList());

                            if (paths.size() > 1)
                                parseState.setMultipleStates(true);

                            for (StackPath2 path : paths) {
                                HybridStackNode2 originStack2 = path.head();
                                IncrementalParseForest[] parseNodes = null;
                                // get parse forests
                                IncrementalParseForest[] res = new IncrementalParseForest[path.length];

                                if (res != null) {
                                    StackPath2 path1 = path;

                                    for (int i = 0; i < path.length; i++) {
                                        NonEmptyStackPath2 nonEmptyPath =
                                                (NonEmptyStackPath2) path1;

                                        res[i] = nonEmptyPath.link.parseForest;

                                        path1 = nonEmptyPath.tail;
                                    }

                                    parseNodes = res;
                                }

//                            if(!reduceManager.ignoreReducePath(originStack2, reduceAction, parseNodes))
//                                reducer(observing, parseState, activeStackForLimitedReductions, originStack2, reduceAction, parseNodes);
                                reducer(parseState, activeStackForLimitedReductions, originStack2, reduceAction, parseNodes);
                            }
                            // end do reductionshelper

                        }
                    }
                }
            }
        } else {
            // reducer no existing stack

            // create stack node
            HybridStackNode2 newStackWithGotoState = new HybridStackNode2(gotoState);
            // notify new stack node
//            if(!observers.isEmpty()) {
//                for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer11 : observers)
//                    ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer2 -> observer2.createStackNode(newStackWithGotoState)).notify(observer11);
//            }

            StackLink2 link;
            if (reduce.isRejectProduction()) {

                // create stack link (with skipped node)
                link = new StackLink2(newStackWithGotoState, originStack, new IncrementalSkippedNode(reduce.production(), parseForests));
                newStackWithGotoState.addLink(link); // ad link

                // notify created stack link
//                if(!observers.isEmpty()) {
//                    for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer11 : observers)
//                        ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer2 -> observer2.createStackLink(link)).notify(observer11);
//                }

                // reject link
                link.reject();

                // notify reject link
//                if(!observers.isEmpty()) {
//                    for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                        ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.rejectStackLink(link)).notify(observer1);
//                }
            } else {
                // get parse node
                IncrementalParseNode parseNode;

                // if skip parse node creation?
                if (reduce.production().isSkippableInParseForest())
                    parseNode = new IncrementalSkippedNode(reduce.production(), parseForests);
                else {
                    IProduction production1 = reduce.production();

                    // create derivation (from parseForestManager)
                    IncrementalDerivation derivation = new IncrementalDerivation(production1, reduce.productionType(), parseForests);

                    // notify observers on created derivation
//                    if(!observers.isEmpty()) {
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer11 : observers)
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer2 -> observer2.createDerivation(derivation, production1, derivation.parseForests)).notify(observer11);
//                    }

                    IProduction production = reduce.production();

                    // create parse node
                    IState state = parseState.newParseNodesAreReusable() ? ((IStackNode) originStack).state() : NO_STATE;
                    IncrementalParseNode parseNode1 = new IncrementalParseNode(production, derivation, state);

                    // notify parse node creation
//                    if(!observers.isEmpty())
//                        for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers) {
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createParseNode(parseNode1, production)).notify(observer1);
//                            ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode1, derivation)).notify(observer1);
//                        }

                    parseNode = parseNode1;
                }

                // create stack link
                StackLink2 link1 = new StackLink2(newStackWithGotoState, originStack, parseNode);
                newStackWithGotoState.addLink(link1); // ad link

                // notify created stack link
//                if(!observers.isEmpty()) {
//                    for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                        ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createStackLink(link1)).notify(observer1);
//                }

            }

            gotoStack = newStackWithGotoState;

            parseState.activeStacks.add(gotoStack);
            parseState.forActorStacks.add(gotoStack);
        }

        HybridStackNode2 finalGotoStack = gotoStack;
//        notify(
//                observer -> observer.reducer(parseState, activeStack, originStack, reduce, parseForests, finalGotoStack));
    }

    public void shifter(IncrementalParseState2 parseState) {
        parseState.activeStacks.clear();

        // get node to shift
        IncrementalParseForest characterNode = parseState.inputStack.getNode();//getNodeToShift(parseState);

//        notify(observer -> observer.shifter(characterNode, parseState.forShifter));

        for (ForShifterElement<HybridStackNode2> forShifterElement : parseState.forShifter) {
            HybridStackNode2 gotoStack = parseState.activeStacks.findWithState(forShifterElement.state);

            if (gotoStack != null) {

                // create stack link
                StackLink2 link = new StackLink2(gotoStack, forShifterElement.stack, characterNode);
                gotoStack.addLink(link); // ad link

                // notify created stack link
//                if(!observers.isEmpty()) {
//                    for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                        ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createStackLink(link)).notify(observer1);
//                }

            } else {
                // create stack node
                HybridStackNode2 newStackNode = new HybridStackNode2(forShifterElement.state);
                // notify new stack node
//                if(!observers.isEmpty()) {
//                    for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                        ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createStackNode(newStackNode)).notify(observer1);
//                }
                gotoStack = newStackNode; // TODO (Mara) use less attributions

                // create stack link
                StackLink2 link = new StackLink2(gotoStack, forShifterElement.stack, characterNode);
                gotoStack.addLink(link); // ad link

                // notify created stack link
//                if(!observers.isEmpty()) {
//                    for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                        ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createStackLink(link)).notify(observer1);
//                }

                parseState.activeStacks.add(gotoStack);
            }

            HybridStackNode2 finalGotoStack = gotoStack;
//            notify(observer -> observer.shift(parseState, forShifterElement.stack, finalGotoStack));
        }

        parseState.forShifter.clear();
    }

    // If the lookahead has null yield, there are always at least two valid actions:
    // Either reduce a production with arity 0, or shift the already-existing null-yield subtree.
    // This method returns whether the Goto state of the Reduce action matches the state of the GotoShift action.
//    private boolean nullReduceMatchesGotoShift(HybridStackNode<IncrementalParseForest> stack, IReduce reduceAction, GotoShift gotoShiftAction) {
//        return stack.state().getGotoId(reduceAction.production().id()) == gotoShiftAction.shiftStateId();
//    }

//    public IncrementalParseForest getNodeToShift(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState) {
//        return parseState.inputStack.getNode();
//    }

//    public void addForShifter(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState, HybridStackNode<IncrementalParseForest> stack, IState shiftState) {
//        ForShifterElement<HybridStackNode<IncrementalParseForest>> forShifterElement = new ForShifterElement<>(stack, shiftState);
//
//        observing.notify(observer -> observer.addForShifter(forShifterElement));
//
//        parseState.forShifter.add(forShifterElement);
//    }

//    public ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing() {
//        return observing;
//    }


    //REDUCER OPTIMIZED METHODS Incremental
    // TODO inline methods, remove param passing if not necessary
//    public void reducerExistingStackWithDirectLink(
////            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing,
//            IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState,
//            IReduce reduce, StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> existingDirectLinkToActiveStateWithGoto,
//            IncrementalParseForest[] parseForests) {
//        @SuppressWarnings("unchecked") IncrementalParseNode parseNode =
//                (IncrementalParseNode) existingDirectLinkToActiveStateWithGoto.parseForest;
//
//        if (reduce.isRejectProduction())
//            stackManager.rejectStackLink(existingDirectLinkToActiveStateWithGoto);
//        else if (!existingDirectLinkToActiveStateWithGoto.isRejected()
//                && !reduce.production().isSkippableInParseForest()) {
//            IncrementalDerivation derivation = parseForestManager.createDerivation(parseState,
//                    existingDirectLinkToActiveStateWithGoto.to, reduce.production(), reduce.productionType(), parseForests);
//            parseForestManager.addDerivation(parseState, parseNode, derivation);
//        }
//    }

//    public StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> reducerExistingStackWithoutDirectLink(
////            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing,
//            IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState,
//            IReduce reduce, HybridStackNode<IncrementalParseForest> existingActiveStackWithGotoState,
//            HybridStackNode<IncrementalParseForest> stack,
//            IncrementalParseForest[] parseForests) {
//        StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> newDirectLinkToActiveStateWithGoto;
//
//        if (reduce.isRejectProduction()) {
//            newDirectLinkToActiveStateWithGoto =
//                    stackManager.createStackLink(parseState, existingActiveStackWithGotoState, stack,
//                            parseForestManager.createSkippedNode(parseState, reduce.production(), parseForests));
//
//            stackManager.rejectStackLink(newDirectLinkToActiveStateWithGoto);
//        } else {
//            // get parse node
//            IncrementalParseNode parseNode;
//
//            // if skip parse node creation?
//            if (reduce.production().isSkippableInParseForest())
//                parseNode = parseForestManager.createSkippedNode(parseState, reduce.production(), parseForests);
//            else {
//                IncrementalDerivation derivation = parseForestManager.createDerivation(parseState, stack, reduce.production(),
//                        reduce.productionType(), parseForests);
//                parseNode = parseForestManager.createParseNode(parseState, stack, reduce.production(), derivation);
//            }
//
//            newDirectLinkToActiveStateWithGoto =
//                    stackManager.createStackLink(parseState, existingActiveStackWithGotoState, stack, parseNode);
//        }
//
//        return newDirectLinkToActiveStateWithGoto;
//    }

//    public HybridStackNode<IncrementalParseForest> reducerNoExistingStack(
////            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing,
//            IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState,
//            IReduce reduce, HybridStackNode<IncrementalParseForest> stack, IState gotoState, IncrementalParseForest[] parseForests) {
//        HybridStackNode<IncrementalParseForest> newStackWithGotoState = stackManager.createStackNode(gotoState);
//
//        StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> link;
//
//        if (reduce.isRejectProduction()) {
//            link = stackManager.createStackLink(parseState, newStackWithGotoState, stack,
//                    parseForestManager.createSkippedNode(parseState, reduce.production(), parseForests));
//
//            stackManager.rejectStackLink(link);
//        } else {
//            // get parse node
//            IncrementalParseNode parseNode;
//
//            // if skip parse node creation?
//            if (reduce.production().isSkippableInParseForest())
//                parseNode = parseForestManager.createSkippedNode(parseState, reduce.production(), parseForests);
//            else {
//                IncrementalDerivation derivation = parseForestManager.createDerivation(parseState, stack, reduce.production(),
//                        reduce.productionType(), parseForests);
//                parseNode = parseForestManager.createParseNode(parseState, stack, reduce.production(), derivation);
//            }
//
//            stackManager.createStackLink(parseState, newStackWithGotoState, stack, parseNode);
//        }
//
//        return newStackWithGotoState;
//    }

//    private IncrementalParseForest getParseNode(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState, IReduce reduce, HybridStackNode<IncrementalParseForest> stack,
//                                                IncrementalParseForest[] parseForests) {
//        IncrementalParseNode parseNode;
//
//        // if skip parse node creation?
//        if (reduce.production().isSkippableInParseForest())
//            parseNode = parseForestManager.createSkippedNode(parseState, reduce.production(), parseForests);
//        else {
//            IncrementalDerivation derivation = parseForestManager.createDerivation(parseState, stack, reduce.production(),
//                    reduce.productionType(), parseForests);
//            parseNode = parseForestManager.createParseNode(parseState, stack, reduce.production(), derivation);
//        }
//
//        return parseNode;
//    }
    // END REDUCER METHODS

    // PROCESS UPDATES METHODS
    public IncrementalParseForest processUpdates(String previousInput, IncrementalParseForest previous,
                                                 EditorUpdate... editorUpdates) {
        return processUpdates(previousInput, previous, Arrays.asList(editorUpdates));
    }

    /**
     * Recursively processes the tree until the update site has been found. General strategy:
     *
     * <ul>
     * <li>If the update is a replacement (a, b, new) with a != b: replace character a with the new string and delete
     * characters [a+1, b>
     * <li>If the update is an insertion (a, a, new), and
     * <ul>
     * <li>the insertion is at the first character: replace first character with (new, first)
     * <li>the insertion is anywhere else: replace character a with (a, new)
     * </ul>
     * <li>If the update is a deletion (a, b, ""): just delete everything in range [a, b>
     * </ul>
     * <p>
     * The slightly reasonable assumption has been made that any two consecutive updates never "touch" each other
     * (meaning that first.end == second.start). This method will still work for two consecutive replacements, but no
     * guarantees are made for a consecutive insertion/deletion.
     */
    public IncrementalParseForest processUpdates(String previousInput, IncrementalParseForest previous,
                                                 List<EditorUpdate> editorUpdates) {
        // Optimization: if there are no changes: then just return the old tree
        if (editorUpdates.size() == 0)
            return previous;

        // Optimization: if everything is deleted/replaced: then return a tree created from the inserted string
        if (editorUpdates.size() == 1) {
            EditorUpdate editorUpdate = editorUpdates.get(0);
            if (editorUpdate.deletedStart == 0 && editorUpdate.deletedEnd == previous.width()) {
                return getParseNodeFromString(editorUpdate.inserted);
            }
        }

        LinkedList<EditorUpdate> linkedUpdates = new LinkedList<>(editorUpdates);
        return processUpdates(previousInput, previous, 0, linkedUpdates);
    }

    private IncrementalParseForest processUpdates(String previousInput, IncrementalParseForest currentForest,
                                                  int currentOffset, LinkedList<EditorUpdate> updates) {
        if (currentForest.isTerminal()) {
            if (currentForest instanceof IncrementalSkippedNode) {
                // First explicitly instantiate all skipped character nodes before applying updates
                return processUpdates(previousInput,
                        getParseNodeFromString(
                                previousInput.substring(currentOffset, currentOffset + currentForest.width())),
                        currentOffset, updates);
            }
            EditorUpdate update = updates.getFirst();
            int deletedStartOffset = update.deletedStart;
            int deletedEndOffset = update.deletedEnd;
            String inserted = update.inserted;
            EditorUpdate.Type type = update.getType();

            // If it is an insertion (there is nothing to delete, deletedStart == deletedEnd)
            if (type == INSERTION) {
                // If insert position is begin of string: prepend to first character
                if (deletedStartOffset == 0 && currentOffset == deletedEndOffset) {
                    updates.removeFirst();
                    return newParseNodeFromChildren(getParseNodeFromString(inserted), currentForest);
                }
                // If insert position is NOT begin of string: append to current character
                if (deletedStartOffset != 0 && currentOffset == deletedStartOffset - currentForest.width()) {
                    updates.removeFirst();
                    return newParseNodeFromChildren(currentForest, getParseNodeFromString(inserted));
                }
                // If none of the cases applies: just return original character node
                return currentForest;
            }
            // Replace first deleted character with the inserted string (if any)
            if (type == REPLACEMENT && currentOffset == deletedStartOffset) {
                if (currentOffset == deletedEndOffset - currentForest.width())
                    updates.removeFirst();
                return getParseNodeFromString(inserted);
            }
            // Else: delete all characters within deletion range
            if (deletedStartOffset <= currentOffset && currentOffset < deletedEndOffset) {
                if (currentOffset == deletedEndOffset - currentForest.width())
                    updates.removeFirst();
                return null;
            }
            // If none of the cases applies: just return original character node
            return currentForest;
        }
        // Use a shallow copy of the current children, else the old children array will be modified
        IncrementalParseForest[] parseForests =
                ((IncrementalParseNode) currentForest).getFirstDerivation().parseForests().clone();
        for (int i = 0; i < parseForests.length; i++) {
            if (updates.isEmpty())
                break;
            // If the current subtree is after the previous to-be-deleted range: move to next update
            if (currentOffset >= updates.getFirst().deletedEnd && currentOffset > 0)
                updates.removeFirst();
            if (updates.isEmpty())
                break;

            EditorUpdate update = updates.getFirst();
            int deletedStartOffset = update.deletedStart;
            int deletedEndOffset = update.deletedEnd;
            String inserted = update.inserted;
            EditorUpdate.Type type = update.getType();

            IncrementalParseForest parseForest = parseForests[i];
            int nextOffset = currentOffset + parseForest.width(); // == start offset of right sibling subtree

            // Optimization: if current subtree starts exactly at deletedStart and it spans the subtree: replace it
            if (type == REPLACEMENT && deletedStartOffset == currentOffset && nextOffset <= deletedEndOffset
                    // (also, it must be at least one character wide, else empty subtrees at the same position get replaced)
                    && currentOffset < nextOffset)
                parseForests[i] = getParseNodeFromString(inserted);
                // Optimization: if current subtree is a subrange within [deletedStart, deletedEnd]: delete it
            else if (type == REPLACEMENT && deletedStartOffset <= currentOffset && nextOffset <= deletedEndOffset)
                parseForests[i] = null;
                // If current subtree (partially) overlaps with the to-be-deleted range: recurse
            else if (deletedStartOffset <= nextOffset && currentOffset <= deletedEndOffset)
                parseForests[i] = processUpdates(previousInput, parseForest, currentOffset, updates);

            currentOffset = nextOffset;
        }
        return newParseNodeFromChildren(parseForests);
    }

    private IncrementalParseNode newParseNodeFromChildren(IncrementalParseForest... newChildren) {
        IncrementalParseForest[] filtered =
                Arrays.stream(newChildren).filter(Objects::nonNull).toArray(IncrementalParseForest[]::new);
        if (filtered.length == 0)
            return null;
        // create changed parse node
        IncrementalParseNode parseNode = new IncrementalParseNode(filtered);

        // notify observers on new changed parse node creation
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer3 : observers) {
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createDerivation(parseNode.getFirstDerivation(), null, filtered)).notify(observer3);
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer2 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createParseNode(parseNode, null)).notify(observer2);
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode, parseNode.getFirstDerivation())).notify(observer1);
//            }
//        }

        return parseNode;
    }

    public IncrementalParseNode getParseNodeFromString(String inputString) {
        int[] chars = inputString.codePoints().toArray();

        IncrementalParseForest[] parseForests = new IncrementalParseForest[chars.length];

        for (int i = 0; i < chars.length; i++) {
            // create character node
            // notify observers on character node creation
//            if(!observers.isEmpty()) {
//                for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                    ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createCharacterNode(characterNode, characterNode.character)).notify(observer1);
//            }

            parseForests[i] = new IncrementalCharacterNode(chars[i]);
        }
        // create changed parse node
        IncrementalParseNode parseNode = new IncrementalParseNode(parseForests);

        // notify observers on new changed parse node creation
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer3 : observers) {
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createDerivation(parseNode.getFirstDerivation(), null, parseForests)).notify(observer3);
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer2 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createParseNode(parseNode, null)).notify(observer2);
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode, parseNode.getFirstDerivation())).notify(observer1);
//            }
//        }

        return parseNode;
    }
    // END PROCESS UPDATES METHODS

    //observing
//    public void notify(IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> notification) {
////        if(observers.isEmpty())
////            return;
////
////        for(IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer : observers)
////            notification.notify(observer);
//    }


    // From HybridStackManager (not inlined because its recursive)
    public void findAllPathsOfLength(StackPath2 path, int length,
                                     List<StackPath2> paths) {
        if (length == 0)
            paths.add(path);
        else {
            HybridStackNode2 lastStackNode = path.head();

            // loop on stackLinksOut
            for (StackLink2 linkOut : lastStackNode.getLinks()) {
                if (!linkOut.isRejected()) {
                    StackPath2 extendedPath = new NonEmptyStackPath2(linkOut, path);

                    findAllPathsOfLength(extendedPath, length - 1, paths);
                }
            }
        }
    }

    // From IncrementalParseForestManager2 (not inlined because it has 4 uses and its a large method)
    public void visit(JSGLR2Request request, IncrementalParseForest root,
                      ParseNodeVisitor<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode> visitor) {
        Stack<Position> positionStack = new Stack<>(); // Start positions of parse nodes
        Stack<Object> inputStack = new Stack<>(); // Pending parse nodes and derivations
        Stack<IncrementalVisit> outputStack = new Stack<>(); // Parse node and derivations with remaining children

        Position pivotPosition = Position.START_POSITION;
        positionStack.push(Position.START_POSITION);
        inputStack.push(root);
        outputStack.push(new IncrementalVisit());

        while (!inputStack.isEmpty() || !outputStack.isEmpty()) {
            if (!outputStack.isEmpty() && outputStack.peek().done()) { // Finish derivation
                outputStack.pop();

                if (outputStack.isEmpty())
                    break;

                outputStack.peek().remainingChildren--;

                if (!outputStack.isEmpty() && outputStack.peek().done()) { // Visit parse node
                    IncrementalParseNode parseNode = outputStack.pop().parseNode;

                    visitor.postVisit(parseNode, positionStack.pop(), pivotPosition);

                    outputStack.peek().remainingChildren--;
                }
            } else if (inputStack.peek() instanceof IDerivation) { // Consume derivation
                IncrementalDerivation derivation = (IncrementalDerivation) inputStack.pop();

                outputStack.push(new IncrementalVisit(derivation));

                IncrementalParseForest[] children = derivation.parseForests();

                for (int i = children.length - 1; i >= 0; i--)
                    inputStack.push(children[i]);

                pivotPosition = positionStack.peek();
            } else if (inputStack.peek() instanceof ICharacterNode) { // Consume character node
                pivotPosition = pivotPosition.step(request.input, ((ICharacterNode) inputStack.pop()).width());

                outputStack.peek().remainingChildren--;
            } else if (inputStack.peek() instanceof IParseNode) { // Consume (skipped) parse node
                IncrementalParseNode parseNode = (IncrementalParseNode) inputStack.pop();
                positionStack.push(pivotPosition);

                boolean visitChildren = visitor.preVisit(parseNode, pivotPosition);

                if (visitChildren && parseNode.hasDerivations()) { // Parse node with derivation(s)
                    int derivations = 0;

                    for (IncrementalDerivation derivation : parseNode.getDerivations()) {
                        inputStack.push(derivation);
                        derivations++;

                        if (derivations >= 1 && !visitor.visitAmbiguousDerivations())
                            break;
                    }

                    outputStack.push(new IncrementalVisit(derivations, parseNode));
                } else { // Skipped parse node (without derivations)
                    pivotPosition = pivotPosition.step(request.input, parseNode.width());

                    visitor.postVisit(parseNode, positionStack.pop(), pivotPosition);

                    outputStack.peek().remainingChildren--;
                }
            }
        }
    }

    class IncrementalVisit {
        int remainingChildren;
        IncrementalParseNode parseNode;

        IncrementalVisit() {
            this.remainingChildren = 1;
            this.parseNode = null;
        }

        IncrementalVisit(IDerivation<?> derivation) {
            this.remainingChildren = derivation.parseForests().length;
            this.parseNode = null;
        }

        IncrementalVisit(int remainingChildren, IncrementalParseNode parseNode) {
            this.remainingChildren = remainingChildren;
            this.parseNode = parseNode;
        }

        boolean done() {
            return remainingChildren == 0;
        }
    }
}

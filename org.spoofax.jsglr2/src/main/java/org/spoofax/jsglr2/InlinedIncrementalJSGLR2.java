package org.spoofax.jsglr2;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.actions.ActionType;
import org.metaborg.parsetable.actions.IAction;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.parsetable.actions.IShift;
import org.metaborg.parsetable.states.IState;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.imploder.*;
import org.spoofax.jsglr2.imploder.incremental.IncrementalStrategoTermImploder;
import org.spoofax.jsglr2.imploder.incremental.IncrementalTreeImploder;
import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.incremental.IncrementalParser;
import org.spoofax.jsglr2.incremental.IncrementalParser2;
import org.spoofax.jsglr2.incremental.IncrementalReduceManager;
import org.spoofax.jsglr2.incremental.actions.GotoShift;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForestManager;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.IInputStack;
import org.spoofax.jsglr2.inputstack.incremental.EagerIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IncrementalInputStackFactory;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.*;
import org.spoofax.jsglr2.parser.failure.DefaultParseFailureHandler;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseFailureCause;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.reducing.ReduceActionFilter;
import org.spoofax.jsglr2.reducing.ReducerOptimized;
import org.spoofax.jsglr2.stack.AbstractStackNode;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.*;
import org.spoofax.jsglr2.stack.hybrid.HybridStackManager;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeShapedTokenizer;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.metaborg.util.iterators.Iterables2.stream;
import static org.spoofax.jsglr2.parser.observing.IParserObserver.BreakdownReason.*;
import static org.spoofax.jsglr2.parser.observing.IParserObserver.BreakdownReason.IRREUSABLE;

public class InlinedIncrementalJSGLR2
// @formatter:off
//        <
//        ParseForest extends IParseForest,
//        IntermediateResult,
//        ImploderCache,
//        AbstractSyntaxTree,
//        ImplodeResult extends IImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm>
//        TreeTokens extends ITokens
//        >
// @formatter:on
        implements JSGLR2<IStrategoTerm> {

    IParseTable parseTable;
    public final IncrementalParser2 parser;
    IncrementalStrategoTermImploder imploder;
    ITokenizer<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeTokens> tokenizer;

    IActiveStacksFactory activeStacksFactory;
    IForActorStacksFactory forActorStacksFactory;

    InlinedIncrementalJSGLR2 (IParseTable parseTable){
        this.parseTable = parseTable;

         this.activeStacksFactory = new ActiveStacksFactory(ActiveStacksRepresentation.ArrayList);
         this.forActorStacksFactory =  new ForActorStacksFactory(ForActorStacksRepresentation.ArrayDeque);

        IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory =
                EagerIncrementalInputStack::new; // TODO switch between Eager, Lazy, and Linked?

        IncrementalParser2 parser =
                new IncrementalParser2<>(
//                        incrementalInputStackFactory,
                        IncrementalParseState.factory(activeStacksFactory, forActorStacksFactory),
                        parseTable,
                        HybridStackManager.factory(),
                        IncrementalParseForestManager::new,
//                        null,
                        IncrementalReduceManager.factoryIncremental(ReducerOptimized::new)
//                       (parseTable1, stackManager, parseForestManager) -> new IncrementalReduceManager<>(parseTable1,
//                                stackManager, parseForestManager, ReducerOptimized::new),

//                        DefaultParseFailureHandler::new,
//                        EmptyParseReporter.factory()
                                        );

        parser.reduceManager.addFilter(ReduceActionFilter.ignoreRecoveryAndCompletion());

        this.parser = parser;
        this.imploder = new IncrementalStrategoTermImploder();
        this.tokenizer =  new IncrementalTreeShapedTokenizer();
    }

    @Override public IParser parser() {
        return parser;
    }

    @Override public void attachObserver(IParserObserver observer) {
        parser.observing().attachObserver(observer);
    }

    public final HashMap<JSGLR2Request.CachingKey, String> inputCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalParseForest> parseForestCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>> imploderCacheCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalTreeTokens> tokensCache = new HashMap<>();

    @Override public JSGLR2Result<IStrategoTerm> parseResult(JSGLR2Request request) {
        JSGLR2Request.CachingKey cachingKey = request.isCacheable() ? request.cachingKey() : null;
        // The "previous" values will be `null` if `cachingKey == null`
        String previousInput = inputCache.get(cachingKey);
        IncrementalParseForest previousParseForest = parseForestCache.get(cachingKey);
        IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> previousImploderCache = imploderCacheCache.get(cachingKey);
        IncrementalTreeTokens previousTokens = tokensCache.get(cachingKey);

        // Parse
        ParseResult<IncrementalParseForest> parseResult = parse(request, previousInput, (IncrementalParseForest) previousParseForest);

        if(parseResult.isSuccess()) {
            IncrementalParseForest parseForest = ((ParseSuccess<IncrementalParseForest>) parseResult).parseResult;

            IImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm> implodeResult =  imploder.implode(request, parseForest, (IncrementalTreeImploder.ResultCache) previousImploderCache);

            IncrementalTreeTokens tokens =
                    tokenizer.tokenize(request, implodeResult.intermediateResult(), previousTokens).tokens;

            parseResult.postProcessMessages(tokens);

            if(cachingKey != null) {
                inputCache.put(cachingKey, request.input);
                parseForestCache.put(cachingKey, parseForest);
                imploderCacheCache.put(cachingKey, implodeResult.resultCache());
                tokensCache.put(cachingKey, tokens);
            }

            return new JSGLR2Success<>(request, implodeResult.ast(), tokens, implodeResult.isAmbiguous(),
                    parseResult.messages);
        } else {
            ParseFailure<IncrementalParseForest> failure = (ParseFailure<IncrementalParseForest>) parseResult;

            return new JSGLR2Failure<>(request, failure, parseResult.messages);
        }
    }

    // FROM PARSER
    public ParseResult<IncrementalParseForest> parse(JSGLR2Request request, String previousInput,
                                                     IncrementalParseForest previousResult) {
        IncrementalParseState parseState = (IncrementalParseState)  getParseState(request, previousInput, previousResult);

        parser.observing.notify(observer -> observer.parseStart(parseState));

        // TODO StackNode
        IStackNode initialStackNode = parser.stackManager.createInitialStackNode(parseTable.getStartState());

        parseState.activeStacks.add(initialStackNode);

        boolean recover;

        try {
            do {
                parseLoop(parseState);
                // TODO: optimization remove if condition
                if(parseState.acceptingStack == null)
                    recover = false;
                else
                    recover = false;
            } while(recover);

            if(parseState.acceptingStack != null) {
                IncrementalParseForest parseForest =
                        (IncrementalParseForest) parser.stackManager.findDirectLink(parseState.acceptingStack, initialStackNode).parseForest;

                IncrementalParseForest parseForestWithStartSymbol = request.startSymbol != null
                        ? (IncrementalParseForest) parser.parseForestManager.filterStartSymbol(parseForest, request.startSymbol, parseState) : parseForest;

                if(parseForest != null && parseForestWithStartSymbol == null)
                    return parser.failure(parseState, new ParseFailureCause(ParseFailureCause.Type.InvalidStartSymbol));
                else
                    return parser.complete(parseState, parseForestWithStartSymbol);
            } else{
                Position position = parseState.inputStack.safePosition();
                if(parseState.inputStack.offset() < parseState.inputStack.length())
                    return parser.failure(parseState, new ParseFailureCause(ParseFailureCause.Type.UnexpectedInput, position));
                else
                    return parser.failure(parseState, new ParseFailureCause(ParseFailureCause.Type.UnexpectedEOF, position));
            }
        } catch(ParseException e) {
            return parser.failure(parseState, e.cause);
        }
   }

    protected IncrementalParseState getParseState(JSGLR2Request request, String previousInput,
                                       IncrementalParseForest previousResult) {
        IncrementalParseForest updatedTree = previousInput != null && previousResult != null
                ? parser.processUpdates.processUpdates(previousInput, previousResult, parser.diff.diff(previousInput, request.input))
                : parser.processUpdates.getParseNodeFromString(request.input);

//        return (IncrementalParseState) parser.parseStateFactory.get(request, parser.incrementalInputStackFactory.get(updatedTree, request.input), parser.observing);
//        return (IncrementalParseState) parser.parseStateFactory.get(request, new EagerIncrementalInputStack(updatedTree, request.input), parser.observing);
        IActiveStacks<IStackNode> activeStacks = activeStacksFactory.get(parser.observing);
        IForActorStacks<IStackNode> forActorStacks = forActorStacksFactory.get(parser.observing);
        return new IncrementalParseState<>(request,
                new EagerIncrementalInputStack(updatedTree, request.input),
                activeStacks,
                forActorStacks);
    }

    protected void parseLoop(IncrementalParseState parseState) throws ParseException {
        while(parseState.inputStack.hasNext() && !parseState.activeStacks.isEmpty()) {
            parseCharacter(parseState);
            parseState.inputStack.consumed();

            if(!parseState.activeStacks.isEmpty())
                parseState.inputStack.next();
        }
    }

    protected void parseCharacter(IncrementalParseState parseState) throws ParseException {
        parseState.nextParseRound(parser.observing);

        parseState.activeStacks.addAllTo(parseState.forActorStacks);

        parser.observing.notify(observer -> observer.forActorStacks(parseState.forActorStacks));

        processForActorStacks(parseState);

        shifter(parseState);
    }

    protected void processForActorStacks(IncrementalParseState parseState) {
        while(parseState.forActorStacks.nonEmpty()) {
            //TODO STackNode
            IStackNode stack = parseState.forActorStacks.remove();

            parser.observing.notify(observer -> observer.handleForActorStack(stack, parseState.forActorStacks));

            if(!stack.allLinksRejected())
                actor(stack, parseState);
            else
                parser.observing.notify(observer -> observer.skipRejectedStack(stack));
        }
    }

    protected void shifter(IncrementalParseState parseState) {
        parseState.activeStacks.clear();

        IncrementalParseForest characterNode = parser.getNodeToShift(parseState);

        parser.observing.notify(observer -> observer.shifter(characterNode, parseState.forShifter));

//        for(ForShifterElement<StackNode> forShifterElement : parseState.forShifter) {
        for(Object forShifterElement : parseState.forShifter) {
                IStackNode gotoStack = parseState.activeStacks.findWithState(((ForShifterElement<IStackNode>)forShifterElement).state);

            if(gotoStack != null) {
                parser.stackManager.createStackLink(parseState, gotoStack, ((ForShifterElement<IStackNode>)forShifterElement).stack, characterNode);
            } else {
                gotoStack = parser.stackManager.createStackNode(((ForShifterElement<IStackNode>)forShifterElement).state);

                parser.stackManager.createStackLink(parseState, gotoStack, ((ForShifterElement<IStackNode>)forShifterElement).stack, characterNode);

                parseState.activeStacks.add(gotoStack);
            }

            IStackNode finalGotoStack = gotoStack;
            parser.observing.notify(observer -> observer.shift(parseState, ((ForShifterElement<IStackNode>)forShifterElement).stack, finalGotoStack));
        }

        parseState.forShifter.clear();
    }

    protected void actor(IStackNode stack, IncrementalParseState parseState) {
        Iterable<IAction> actions = getActions(stack, parseState);
        // Break down the lookahead in either of the following scenarios:
        // - The lookahead is not reusable (terminal nodes are always reusable).
        // - The lookahead is a non-terminal parse node AND there are no actions for it.
        // In the second case, do not break down if we already have something to shift.
        // This node that we can shift should not be broken down anymore:
        // - if we would, it would cause different shifts to be desynchronised;
        // - if a break-down of this node would cause different actions, it would already have been broken down because
        // that would mean that this node was created when the parser was in multiple states.
        while(!((IIncrementalInputStack)parseState.inputStack).getNode().isReusable()
                || !((IIncrementalInputStack)parseState.inputStack).getNode().isTerminal() && isEmpty(actions) && parseState.forShifter.isEmpty()) {
            parser.observing.notify(observer -> {
                IncrementalParseForest node = ((IIncrementalInputStack)parseState.inputStack).getNode();
                observer.breakDown(((IIncrementalInputStack)parseState.inputStack),
                        node instanceof IParseNode && ((IParseNode<?, ?>) node).production() == null ? TEMPORARY
                                : node.isReusable() ? node.isReusable(stack.state()) ? NO_ACTIONS : WRONG_STATE
                                : IRREUSABLE);
            });
            ((IIncrementalInputStack)parseState.inputStack).breakDown();
            parser.observing.notify(observer -> observer.parseRound(parseState, parseState.activeStacks));
            actions = getActions(stack, parseState);
        }

        if(size(actions) > 1)
            parseState.setMultipleStates(true);

        Iterable<IAction> finalActions = actions;
        parser.observing.notify(observer -> observer.actor(stack, parseState, finalActions));

        for(IAction action : actions)
            actor(stack, parseState, action);
    }

    protected void actor(IStackNode stack, IncrementalParseState parseState, IAction action) {
        switch(action.actionType()) {
            case SHIFT:
                IShift shiftAction = (IShift) action;
                IState shiftState = parseTable.getState(shiftAction.shiftStateId());

                addForShifter(parseState, stack, shiftState);

                break;
            case REDUCE:
            case REDUCE_LOOKAHEAD: // Lookahead is checked while retrieving applicable actions from the state
                IReduce reduceAction = (IReduce) action;

                parser.reduceManager.doReductions(parser.observing, parseState, stack, reduceAction);

                break;
            case ACCEPT:
                parseState.acceptingStack = stack;

                parser.observing.notify(observer -> observer.accept(stack));

                break;
        }
    }

    // Inside this method, we can assume that the lookahead is a valid and complete subtree of the previous parse.
    // Else, the loop in `actor` will have broken it down
    private Iterable<IAction> getActions(IStackNode stack, IncrementalParseState parseState) {
        // Get actions based on the lookahead terminal that `parse` will calculate in actionQueryCharacter
        Iterable<IAction> actions = stack.state().getApplicableActions(parseState.inputStack, parseState.mode);

        IncrementalParseForest lookahead = ((IIncrementalInputStack)parseState.inputStack).getNode();
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
    private boolean nullReduceMatchesGotoShift(IStackNode stack, IReduce reduceAction, GotoShift gotoShiftAction) {
        return stack.state().getGotoId(reduceAction.production().id()) == gotoShiftAction.shiftStateId();
    }

    protected IncrementalParseForest getNodeToShift(IncrementalParseState parseState) {
        return ((IIncrementalInputStack)parseState.inputStack).getNode();
    }

    protected void addForShifter(IncrementalParseState parseState, IStackNode stack, IState shiftState) {
        ForShifterElement<IStackNode> forShifterElement = new ForShifterElement<>(stack, shiftState);

        parser.observing.notify(observer -> observer.addForShifter(forShifterElement));

        parseState.forShifter.add(forShifterElement);
    }


    public ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IStackNode, AbstractParseState<IIncrementalInputStack, IStackNode>> observing() {
        return parser.observing;
    }

    protected ParseResult<IncrementalParseForest> complete(IncrementalParseState parseState, IncrementalParseForest parseForest) {
        List<Message> messages = new ArrayList<>();
        CycleDetector<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode> cycleDetector = new CycleDetector<>(messages);

        parser.parseForestManager.visit(parseState.request, parseForest, cycleDetector);

        if(cycleDetector.cycleDetected()) {
            return failure(new ParseFailure<>(parseState, cycleDetector.failureCause));
        } else {
            //todo: For recovery variant use recoveryParseReporter
//            parser.reporter.report(parseState, parseForest, messages);

            // Generate errors for non-assoc or non-nested productions that are used associatively
            parser.parseForestManager.visit(parseState.request, parseForest, new NonAssocDetector<>(messages));

            if(parseState.request.reportAmbiguities) {
                // Generate warnings for ambiguous parse nodes
                parser.parseForestManager.visit(parseState.request, parseForest,
                        new AmbiguityDetector<>(parseState.inputStack.inputString(), messages));
            }

            ParseSuccess<IncrementalParseForest> success = new ParseSuccess<>(parseState, parseForest, messages);

            parser.observing.notify(observer -> observer.success(success));

            return success;
        }
    }

    protected ParseFailure<IncrementalParseForest> failure(IncrementalParseState parseState, ParseFailureCause failureCause) {
        return failure(new ParseFailure<>(parseState, failureCause));
    }

    protected ParseFailure<IncrementalParseForest> failure(ParseFailure<IncrementalParseForest> failure) {
        parser.observing.notify(observer -> observer.failure(failure));

        return failure;
    }
}

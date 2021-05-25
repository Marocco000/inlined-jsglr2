package org.spoofax.jsglr2;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.actions.ActionType;
import org.metaborg.parsetable.actions.IAction;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.parsetable.actions.IShift;
import org.metaborg.parsetable.states.IState;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.*;
import org.spoofax.jsglr2.imploder.incremental.IncrementalStrategoTermImploder;
import org.spoofax.jsglr2.imploder.incremental.IncrementalTreeImploder;
import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.incremental.IncrementalParser2;
import org.spoofax.jsglr2.incremental.IncrementalReduceManager;
import org.spoofax.jsglr2.incremental.actions.GotoShift;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.incremental.EagerIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.*;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseFailureCause;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.reducing.ReduceActionFilter;
import org.spoofax.jsglr2.reducing.ReducerOptimized;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.*;
import org.spoofax.jsglr2.stack.hybrid.HybridStackManager;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeShapedTokenizer;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeShapedTokenizer2;
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

public class InlinedIncrementalJSGLR2 implements JSGLR2<IStrategoTerm> {

    IParseTable parseTable;
    public final IncrementalParser2 parser;
    IncrementalStrategoTermImploder imploder;
//    ITokenizer<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeTokens> tokenizer;
    IncrementalTreeShapedTokenizer2 tokenizer;

    IActiveStacksFactory activeStacksFactory;
    IForActorStacksFactory forActorStacksFactory;

    InlinedIncrementalJSGLR2(IParseTable parseTable) {
        this.parseTable = parseTable;

        this.activeStacksFactory = new ActiveStacksFactory(ActiveStacksRepresentation.ArrayList);
        this.forActorStacksFactory = new ForActorStacksFactory(ForActorStacksRepresentation.ArrayDeque);

//        IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory =
//                EagerIncrementalInputStack::new; // TODO switch between Eager, Lazy, and Linked?

        IncrementalParser2 parser =
                new IncrementalParser2(
//                        EagerIncrementalInputStack::new,
//                        IncrementalParseState.factory(activeStacksFactory, forActorStacksFactory),
                        parseTable
//                        HybridStackManager.factory(),
//                        IncrementalParseForestManager::new,
//                        null,
//                        IncrementalReduceManager.factoryIncremental(ReducerOptimized::new)
//                       (parseTable1, stackManager, parseForestManager) -> new IncrementalReduceManager<>(parseTable1,
//                                stackManager, parseForestManager, ReducerOptimized::new),

//                        DefaultParseFailureHandler::new,
//                        EmptyParseReporter.factory()
                );
//        parser.reduceManager.addFilter(ReduceActionFilter.ignoreRecoveryAndCompletion());

        this.parser = parser;
        this.imploder = new IncrementalStrategoTermImploder();
        this.tokenizer = new IncrementalTreeShapedTokenizer2();
    }

    @Override
    public IParser parser() {
        return parser;
    }

    @Override
    public void attachObserver(IParserObserver observer) {
        parser.observing().attachObserver(observer);
    }

    public final HashMap<JSGLR2Request.CachingKey, String> inputCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalParseForest> parseForestCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>> imploderCacheCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalTreeTokens> tokensCache = new HashMap<>();

    @Override
    public JSGLR2Result<IStrategoTerm> parseResult(JSGLR2Request request) {
        JSGLR2Request.CachingKey cachingKey = request.isCacheable() ? request.cachingKey() : null;
        // The "previous" values will be `null` if `cachingKey == null`
        String previousInput = inputCache.get(cachingKey);
        IncrementalParseForest previousParseForest = parseForestCache.get(cachingKey);
        IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> previousImploderCache = imploderCacheCache.get(cachingKey);
        IncrementalTreeTokens previousTokens = tokensCache.get(cachingKey);

        // Parse
        ParseResult<IncrementalParseForest> parseResult = parser.parse(request, previousInput, (IncrementalParseForest) previousParseForest);

        if (parseResult.isSuccess()) {
            IncrementalParseForest parseForest = ((ParseSuccess<IncrementalParseForest>) parseResult).parseResult;

            IImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm> implodeResult = imploder.implode(request, parseForest, (IncrementalTreeImploder.ResultCache) previousImploderCache);

            IncrementalTreeTokens tokens =
                    tokenizer.tokenize(request, implodeResult.intermediateResult(), previousTokens).tokens;

            parseResult.postProcessMessages(tokens);

            if (cachingKey != null) {
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

}

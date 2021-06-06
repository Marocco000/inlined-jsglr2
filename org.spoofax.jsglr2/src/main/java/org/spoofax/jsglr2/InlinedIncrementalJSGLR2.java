package org.spoofax.jsglr2;

import org.metaborg.parsetable.IParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.*;
import org.spoofax.jsglr2.inlinedIncremental.IncrementalStrategoTermImploder2;
import org.spoofax.jsglr2.inlinedIncremental.IncrementalParser2;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.inlinedIncremental.ResultCache2;
import org.spoofax.jsglr2.parser.*;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeShapedTokenizer2;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeTokens;

import java.util.HashMap;

public class InlinedIncrementalJSGLR2 implements JSGLR2<IStrategoTerm> {

    IParseTable parseTable;

    public final IncrementalParser2 parser;
    IncrementalStrategoTermImploder2 imploder;
    IncrementalTreeShapedTokenizer2 tokenizer;

    InlinedIncrementalJSGLR2(IParseTable parseTable) {
        this.parseTable = parseTable;

//        IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory =
//                EagerIncrementalInputStack::new; // TODO switch between Eager, Lazy, and Linked?

        IncrementalParser2 parser =
                new IncrementalParser2(parseTable);

        this.parser = parser;
        this.imploder = new IncrementalStrategoTermImploder2();
        this.tokenizer = new IncrementalTreeShapedTokenizer2();
    }

    @Override
    public IParser parser() {
        return parser;
    }

    @Override
    public void attachObserver(IParserObserver observer) {
        parser.observers.add(observer);
    }

    public final HashMap<JSGLR2Request.CachingKey, String> inputCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalParseForest> parseForestCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, ResultCache2> imploderCacheCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, IncrementalTreeTokens> tokensCache = new HashMap<>();

    @Override
    public JSGLR2Result<IStrategoTerm> parseResult(JSGLR2Request request) {
        JSGLR2Request.CachingKey cachingKey = request.isCacheable() ? request.cachingKey() : null;
        // The "previous" values will be `null` if `cachingKey == null`
        String previousInput = inputCache.get(cachingKey);
        IncrementalParseForest previousParseForest = parseForestCache.get(cachingKey);
        ResultCache2 previousImploderCache = imploderCacheCache.get(cachingKey);
        IncrementalTreeTokens previousTokens = tokensCache.get(cachingKey);

        // Parse
        ParseResult<IncrementalParseForest> parseResult = parser.parse(request, previousInput, previousParseForest);

        if (parseResult.isSuccess()) {
            IncrementalParseForest parseForest = ((ParseSuccess<IncrementalParseForest>) parseResult).parseResult;

            IImplodeResult<TreeImploder.SubTree<IStrategoTerm>, ResultCache2, IStrategoTerm> implodeResult = imploder.implode(request, parseForest, previousImploderCache);

            IncrementalTreeTokens tokens =
                    tokenizer.tokenize(request, implodeResult.intermediateResult(), previousTokens).tokens;

            parseResult.postProcessMessages(tokens);

            if (cachingKey != null) {
                // TODO (MARA) optimize: bundle up the data and use one .put instead of 4 ?
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
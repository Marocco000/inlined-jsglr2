package org.spoofax.jsglr2;

import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.imploder.*;
import org.spoofax.jsglr2.imploder.incremental.IncrementalStrategoTermImploder;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.IParser;
import org.spoofax.jsglr2.parser.Parser;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeShapedTokenizer;

import java.util.HashMap;

public class InlinedIncrementalJSGLR2
        // @formatter:off
        <ParseForest extends IParseForest,
                IntermediateResult,
                ImploderCache,
                AbstractSyntaxTree,
                ImplodeResult extends IImplodeResult<IntermediateResult, ImploderCache, AbstractSyntaxTree>,
                TokensResult extends ITokens>
// @formatter:on
        implements JSGLR2<AbstractSyntaxTree> {

//    public final IncrementalParser< ?, ?, ?, ?> parser;
    public final Parser parser;
//    public final IncrementalStrategoTermImploder<?, ?, ?> imploder;
//    public final IncrementalTreeShapedTokenizer tokenizer;


    IImploder<ParseForest, IntermediateResult, ImploderCache, AbstractSyntaxTree, ImplodeResult> imploder;
    ITokenizer<IntermediateResult, TokensResult> tokenizer;
//    ITokenizer<TreeImploder.SubTree<IStrategoTerm>, ?> tokenizer;

    InlinedIncrementalJSGLR2 (Parser parser){
        this.parser = parser;
        this.imploder = new IncrementalStrategoTermImploder();
        this.tokenizer = (ITokenizer<IntermediateResult, TokensResult>) new IncrementalTreeShapedTokenizer();
    }

    @Override public IParser parser() {
        return parser;
    }

    @Override public void attachObserver(IParserObserver observer) {
        parser.observing().attachObserver(observer);
    }

    public final HashMap<JSGLR2Request.CachingKey, String> inputCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, ParseForest> parseForestCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, ImploderCache> imploderCacheCache = new HashMap<>();
    public final HashMap<JSGLR2Request.CachingKey, TokensResult> tokensCache = new HashMap<>();

    @Override public JSGLR2Result<AbstractSyntaxTree> parseResult(JSGLR2Request request) {
        JSGLR2Request.CachingKey cachingKey = request.isCacheable() ? request.cachingKey() : null;
        // The "previous" values will be `null` if `cachingKey == null`
        String previousInput = inputCache.get(cachingKey);
        ParseForest previousParseForest = parseForestCache.get(cachingKey);
        ImploderCache previousImploderCache = imploderCacheCache.get(cachingKey);
        TokensResult previousTokens = tokensCache.get(cachingKey);

        ParseResult<ParseForest> parseResult = parser.parse(request, previousInput, previousParseForest);

        if(parseResult.isSuccess()) {
            ParseForest parseForest = ((ParseSuccess<ParseForest>) parseResult).parseResult;

            ImplodeResult implodeResult = (ImplodeResult) imploder.implode(request, parseForest, previousImploderCache);

            TokensResult tokens =
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
            ParseFailure<ParseForest> failure = (ParseFailure<ParseForest>) parseResult;

            return new JSGLR2Failure<>(request, failure, parseResult.messages);
        }
    }
}

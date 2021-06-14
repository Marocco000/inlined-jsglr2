package org.spoofax.jsglr2.benchmark.jsglr2.util;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.InlinedIncrementalJSGLR2;
import org.spoofax.jsglr2.JSGLR2ImplementationWithCache;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.imploder.IImplodeResult;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.inlinedIncremental.ResultCache2;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.tokens.incremental.IncrementalTreeTokens;

/**
 * This wrapper for JSGLR2 reuses the same cache for every call to parse. Useful for benchmarking the parsing of one
 * specific version of the input while reusing the cache for the previous version every time.
 */
public class JSGLR2PersistentCache
// @formatter:off
   <ParseForest extends IParseForest,
    IntermediateResult,
    ImploderCache,
    AbstractSyntaxTree,
    ImplodeResult extends IImplodeResult<IntermediateResult, ImploderCache, AbstractSyntaxTree>,
    TokensResult extends ITokens>
// @formatter:on
{
    public final JSGLR2ImplementationWithCache<ParseForest, IntermediateResult, ImploderCache, AbstractSyntaxTree, ImplodeResult, TokensResult> jsglr2Implementation;
    public final InlinedIncrementalJSGLR2 jsglr2Implementation2;

    public final String previousInput;
    public final ParseForest previousParseForest;
    public final ImploderCache previousImploderCache;
    public final TokensResult previousTokensResult;

    public final IncrementalParseForest previousParseForest2;
    public final ResultCache2 previousImploderCache2;
    public final IncrementalTreeTokens previousTokensResult2;

    private final String fileName;
    private final JSGLR2Request.CachingKey cachingKey;

    public JSGLR2PersistentCache(
        JSGLR2ImplementationWithCache<ParseForest, IntermediateResult, ImploderCache, AbstractSyntaxTree, ImplodeResult, TokensResult> jsglr2Implementation,
        String input) throws ParseException {
//        new JSGLR2PersistentCache(jsglr2Implementation, null, input);

        this.jsglr2Implementation = jsglr2Implementation;

        this.jsglr2Implementation2 = null;
        this.previousParseForest2 = null;
        this.previousImploderCache2 = null;
        this.previousTokensResult2 = null;

        fileName = "" + System.nanoTime();
        JSGLR2Request request = new JSGLR2Request(input, fileName);
        cachingKey = request.cachingKey();
        jsglr2Implementation.parseUnsafe(request);

        previousInput = jsglr2Implementation.inputCache.get(cachingKey);
        previousParseForest = jsglr2Implementation.parseForestCache.get(cachingKey);
        previousImploderCache = jsglr2Implementation.imploderCacheCache.get(cachingKey);
        previousTokensResult = jsglr2Implementation.tokensCache.get(cachingKey);
    }

    public JSGLR2PersistentCache(
            JSGLR2ImplementationWithCache<ParseForest, IntermediateResult, ImploderCache, AbstractSyntaxTree, ImplodeResult, TokensResult> jsglr2Implementation,
            InlinedIncrementalJSGLR2 jsglr2Implementation2,
            String input) throws ParseException {
        this.jsglr2Implementation = null;
        this.previousParseForest = null;
        this.previousImploderCache = null;
        this.previousTokensResult = null;

        this.jsglr2Implementation2 = jsglr2Implementation2;

        fileName = "" + System.nanoTime();
        JSGLR2Request request = new JSGLR2Request(input, fileName);
        cachingKey = request.cachingKey();
        jsglr2Implementation2.parseUnsafe(request);

        previousInput = jsglr2Implementation2.inputCache.get(cachingKey);
        previousParseForest2 = jsglr2Implementation2.parseForestCache.get(cachingKey);
        previousImploderCache2 = jsglr2Implementation2.imploderCacheCache.get(cachingKey);
        previousTokensResult2 = jsglr2Implementation2.tokensCache.get(cachingKey);
    }

    public AbstractSyntaxTree parse(String input) throws ParseException {
        if(input == null)
            return null;
        if (jsglr2Implementation != null) {
            JSGLR2Request request = new JSGLR2Request(input, fileName);
            AbstractSyntaxTree res = jsglr2Implementation.parseUnsafe(request);

            jsglr2Implementation.inputCache.put(cachingKey, previousInput);
            jsglr2Implementation.parseForestCache.put(cachingKey, previousParseForest);
            jsglr2Implementation.imploderCacheCache.put(cachingKey, previousImploderCache);
            jsglr2Implementation.tokensCache.put(cachingKey, previousTokensResult);

            return res;
        } else {
            JSGLR2Request request = new JSGLR2Request(input, fileName);
            IStrategoTerm res = jsglr2Implementation2.parseUnsafe(request);

            jsglr2Implementation2.inputCache.put(cachingKey, previousInput);
            jsglr2Implementation2.parseForestCache.put(cachingKey, previousParseForest2);
            jsglr2Implementation2.imploderCacheCache.put(cachingKey, previousImploderCache2);
            jsglr2Implementation2.tokensCache.put(cachingKey, previousTokensResult2);

            return (AbstractSyntaxTree) res;
        }

    }

}

package org.spoofax.jsglr2.inlinedIncremental;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.incremental.IncrementalTreeImploder;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;

public class IncrementalImplodeInputInlined {

    public final String inputString;
    public final IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> resultCache;

    public IncrementalImplodeInputInlined(String inputString, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> resultCache) {
        this.resultCache = resultCache;
        this.inputString = inputString;
    }

}
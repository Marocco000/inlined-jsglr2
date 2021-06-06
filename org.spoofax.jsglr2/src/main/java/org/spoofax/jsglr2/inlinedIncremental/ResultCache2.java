package org.spoofax.jsglr2.inlinedIncremental;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;

import java.util.WeakHashMap;

public class ResultCache2 {
    public final WeakHashMap<IncrementalParseForest, TreeImploder.SubTree<IStrategoTerm>> cache = new WeakHashMap<>();

}

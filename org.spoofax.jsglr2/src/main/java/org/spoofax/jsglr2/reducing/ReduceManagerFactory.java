package org.spoofax.jsglr2.reducing;

import org.metaborg.parsetable.IParseTable;
import org.spoofax.jsglr2.datadependent.DataDependentReduceManager;
import org.spoofax.jsglr2.elkhound.AbstractElkhoundStackNode;
import org.spoofax.jsglr2.elkhound.ElkhoundReduceManager;
import org.spoofax.jsglr2.elkhound.ElkhoundStackManager;
import org.spoofax.jsglr2.layoutsensitive.LayoutSensitiveParseForest;
import org.spoofax.jsglr2.layoutsensitive.LayoutSensitiveReduceManager;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestManager;
import org.spoofax.jsglr2.parser.AbstractParse;
import org.spoofax.jsglr2.stack.AbstractStackManager;
import org.spoofax.jsglr2.stack.IStackNode;

public interface ReduceManagerFactory
//@formatter:off
   <ParseForest   extends IParseForest,
    ParseNode     extends ParseForest,
    Derivation    extends IDerivation<ParseForest>,
    StackNode     extends IStackNode,
    Parse         extends AbstractParse<ParseForest, StackNode>,
    StackManager  extends AbstractStackManager<ParseForest, StackNode, Parse>,
    ReduceManager extends org.spoofax.jsglr2.reducing.ReduceManager<ParseForest, ParseNode, Derivation, StackNode, Parse>>
//@formatter:on
{
    ReduceManager get(IParseTable parseTable, StackManager stackManager,
        ParseForestManager<ParseForest, ParseNode, Derivation> parseForestManager,
        ParseForestConstruction parseForestConstruction);

    static
    //@formatter:off
       <ParseForest  extends IParseForest,
        ParseNode    extends ParseForest,
        Derivation   extends IDerivation<ParseForest>,
        StackNode    extends IStackNode,
        StackManager extends AbstractStackManager<ParseForest, StackNode, Parse>,
        Parse        extends AbstractParse<ParseForest, StackNode>>
    //@formatter:on
    ReduceManagerFactory<ParseForest, ParseNode, Derivation, StackNode, Parse, StackManager, org.spoofax.jsglr2.reducing.ReduceManager<ParseForest, ParseNode, Derivation, StackNode, Parse>>
        reduceManagerFactory() {
        return org.spoofax.jsglr2.reducing.ReduceManager::new;
    }

    static
    //@formatter:off
       <ParseForest  extends IParseForest,
        ParseNode    extends ParseForest,
        Derivation   extends IDerivation<ParseForest>,
        StackNode    extends AbstractElkhoundStackNode<ParseForest>,
        StackManager extends ElkhoundStackManager<ParseForest, StackNode, Parse>,
        Parse        extends AbstractParse<ParseForest, StackNode>>
    //@formatter:on
    ReduceManagerFactory<ParseForest, ParseNode, Derivation, StackNode, Parse, StackManager, ElkhoundReduceManager<ParseForest, ParseNode, Derivation, StackNode, Parse>>
        elkhoundReduceManagerFactory() {
        return ElkhoundReduceManager::new;
    }

    static
    //@formatter:off
       <ParseForest  extends IParseForest,
        ParseNode    extends ParseForest,
        Derivation   extends IDerivation<ParseForest>,
        StackNode    extends IStackNode,
        StackManager extends AbstractStackManager<ParseForest, StackNode, Parse>,
        Parse        extends AbstractParse<ParseForest, StackNode>>
    //@formatter:on
    ReduceManagerFactory<ParseForest, ParseNode, Derivation, StackNode, Parse, StackManager, DataDependentReduceManager<ParseForest, ParseNode, Derivation, StackNode, Parse>>
        dataDependentReduceManagerFactory() {
        return DataDependentReduceManager::new;
    }

    static
    //@formatter:off
       <ParseForest  extends LayoutSensitiveParseForest,
        ParseNode    extends ParseForest,
        Derivation   extends IDerivation<ParseForest>,
        StackNode    extends IStackNode,
        StackManager extends AbstractStackManager<ParseForest, StackNode, Parse>,
        Parse        extends AbstractParse<ParseForest, StackNode>>
    //@formatter:on
    ReduceManagerFactory<ParseForest, ParseNode, Derivation, StackNode, Parse, StackManager, LayoutSensitiveReduceManager<ParseForest, ParseNode, Derivation, StackNode, Parse>>
        layoutSensitiveReduceManagerFactory() {
        return LayoutSensitiveReduceManager::new;
    }

}

package org.spoofax.jsglr2.datadependent;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.parsetable.IProduction;
import org.spoofax.jsglr2.parseforest.basic.IBasicSymbolNode;
import org.spoofax.jsglr2.parseforest.basic.BasicParseForest;
import org.spoofax.jsglr2.parser.AbstractParse;
import org.spoofax.jsglr2.parser.Position;

public class DataDependentSymbolNode extends BasicParseForest implements IBasicSymbolNode<BasicParseForest, DataDependentRuleNode> {

    public final IProduction production;
    private final List<DataDependentRuleNode> derivations = new ArrayList<>();

    public DataDependentSymbolNode(int nodeNumber, AbstractParse<?, ?> parse, Position startPosition, Position endPosition,
                                   IProduction production) {
        super(nodeNumber, parse, startPosition, endPosition);
        this.production = production;
    }

    @Override
    public String descriptor() {
        return production.descriptor();
    }

    @Override
    public List<DataDependentRuleNode> getDerivations() {
        return derivations;
    }
}

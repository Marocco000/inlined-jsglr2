/*
 * Created on 06.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk near strategoxt.org>
 *
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr.client;

import java.io.Serializable;

import org.spoofax.jsglr.shared.terms.ATermAppl;

public class Label implements Serializable {

    private static final long serialVersionUID = -4080621639747161438L;

    /*package*/ final int labelNumber;
    /*package*/ final ATermAppl prod;
    private final ProductionAttributes productionAttributes;
    private final boolean injection;
    private Object tree;

    public Label(int labelNumber, ATermAppl prod, ProductionAttributes productionAttributes, boolean injection) {
        this.labelNumber = labelNumber;
        this.prod = prod;
        this.productionAttributes = productionAttributes;
        this.injection = injection;

        if(productionAttributes == null)
            throw new RuntimeException();
    }

    public boolean isLeftAssociative() {
        return productionAttributes.getType() == ProductionType.LEFT_ASSOCIATIVE;
    }

    public boolean isRightAssociative() {
        return productionAttributes.getType() == ProductionType.RIGHT_ASSOCIATIVE;
    }

    public boolean isRecoverProduction() {
        return productionAttributes.isRecoverProduction();
    }

    public boolean isMoreEager(Label rightProd) {
        return productionAttributes.isMoreEager(rightProd.productionAttributes);
    }

    public ProductionAttributes getAttributes() {
        return productionAttributes;
    }

    public boolean isInjection() {
        return injection;
    }

    public ATermAppl getProduction() {
        return prod;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Label && ((Label) obj).labelNumber == labelNumber);
    }

    @Override
    public int hashCode() {
        return labelNumber;
    }

	public void setTree(Object tree) {
		this.tree = tree;
	}

	public Object getTree() {
		return tree;
	}
    
}

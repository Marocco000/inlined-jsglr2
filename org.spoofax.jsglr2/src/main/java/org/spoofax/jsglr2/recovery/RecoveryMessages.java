package org.spoofax.jsglr2.recovery;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.symbols.ILiteralSymbol;
import org.spoofax.jsglr2.messages.Category;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.messages.SourceRegion;

public class RecoveryMessages {

    public static Message get(IProduction production, SourceRegion region) {
        String message;

        if("WATER".equals(production.constructor()))
            message = "Not expected";
        else if("INSERTION".equals(production.constructor())) {
            String insertion;

            if(production.isLiteral())
                insertion = ((ILiteralSymbol) production.lhs()).literal();
            else
                insertion = "Token";

            message = insertion + " expected";
        } else
            message = "Invalid syntax";

        return new Message(message, Category.RECOVERY, region);
    }

}

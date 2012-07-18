package edu.iastate.cycmodeler.model;

import java.util.ArrayList;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Exchange reactions are those reactions which exchange metabolites from what is defined as internal to the system to what is defined as external to the
 * system.  The assumption is that external metabolites are externally buffered, and thus treated differently.  In particular, external reactions
 * can create or destroy metabolites, and are expected not to be balanced.  All exchange reactions are reversible.  All exchange reactions have only
 * one metabolite, which is defined as a reactant with a coefficient of 1, which moves to the boundary compartment.
 * 
 * @author Jesse Walsh
 */
public class ExchangeReactionInstance extends ReactionInstance {

	public ExchangeReactionInstance(String reactionName, Frame metabolite, String compartment) {
		super(null, null, reactionName, true, null, null, null, false);
		ArrayList<MetaboliteInstance> reactants = new ArrayList<MetaboliteInstance>();
		reactants.add(new MetaboliteInstance(metabolite, compartment, 1));
		ArrayList<MetaboliteInstance> products = new ArrayList<MetaboliteInstance>();
		products.add(new MetaboliteInstance(metabolite, CycModeler.BoundaryCompartmentName, 1));
		super.reactants_ = reactants;
		super.products_ = products;		
	}
	
	/**
	 * The convention in the iAF1260 model is to add the suffix "_LPAREN_e_RPAREN_" to the end of reaction IDs for exchange reactions.
	 */
	public String generateReactionID() {
		String baseID = "";
		if (thisReactionFrame_ != null) baseID = thisReactionFrame_.getLocalID();
		else if (parentReaction_ != null) baseID = parentReaction_.getLocalID();
		else baseID = name_;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "" + baseID + "_LPAREN_e_RPAREN_");
		else return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "_" + baseID + "_LPAREN_e_RPAREN_");
	}
	
	/**
	 * By definition, exchange reactions do not have gene associations.
	 */
	public String reactionGeneRule(boolean asBNumber) throws PtoolsErrorException {
		return "";
	}
}

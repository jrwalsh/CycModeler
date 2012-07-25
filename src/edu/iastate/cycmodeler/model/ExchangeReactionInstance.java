package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashSet;


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
public class ExchangeReactionInstance extends AbstractReactionInstance {

	public ExchangeReactionInstance(String reactionName, Frame metabolite, String compartment) {
		this.Name = reactionName;
		this.Reversible = true;
		this.ReactionLocation = compartment;
		
		HashSet<MetaboliteInstance> reactants = new HashSet<MetaboliteInstance>();
		reactants.add(new MetaboliteInstance(metabolite, compartment, 1));
		HashSet<MetaboliteInstance> products = new HashSet<MetaboliteInstance>();
		products.add(new MetaboliteInstance(metabolite, CycModeler.BoundaryCompartmentName, 1));
		this.Reactants = reactants;
		this.Products = products;
		
//		super(null, null, reactionName, true, null, null, null, false);
//		HashSet<MetaboliteInstance> reactants = new HashSet<MetaboliteInstance>();
//		reactants.add(new MetaboliteInstance(metabolite, compartment, 1));
//		HashSet<MetaboliteInstance> products = new HashSet<MetaboliteInstance>();
//		products.add(new MetaboliteInstance(metabolite, CycModeler.BoundaryCompartmentName, 1));
//		super.Reactants = reactants;
//		super.Products = products;
	}
	
	/**
	 * The convention in the iAF1260 model is to add the suffix "_LPAREN_e_RPAREN_" to the end of reaction IDs for exchange reactions.
	 */
	public String generateReactionID() {
		String baseID = Name;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "" + baseID + "_LPAREN_e_RPAREN_");
		else return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "_" + baseID + "_LPAREN_e_RPAREN_");
	}
	
	/**
	 * By definition, exchange reactions do not have gene associations.
	 */
	public String reactionGeneRule(boolean asBNumber) throws PtoolsErrorException {
		return "";
	}

	@Override
	protected void addReactant(MetaboliteInstance reactant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void addProduct(MetaboliteInstance product) {
		// TODO Auto-generated method stub
		
	}
}

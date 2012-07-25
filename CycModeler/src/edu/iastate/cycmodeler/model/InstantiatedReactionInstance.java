package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashSet;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Instantiated reactions are reactions that take on the properties of their parent reaction, but have replace the generic reactants and products of the parent
 * reaction with specific instances of those reactants and products.
 * 
 * @author Jesse Walsh
 */
public class InstantiatedReactionInstance extends AbstractReactionInstance {
	protected Reaction parentReactionFrame;

	public static ArrayList<AbstractReactionInstance> getInstantiatedReactions(ReactionInstance reaction) {
		//TODO
		return null;
	}
			
	public InstantiatedReactionInstance(Reaction parentReactionFrame, String reactionName, boolean reversible, String reactionLocation, HashSet<MetaboliteInstance> reactants, HashSet<MetaboliteInstance> products) {
		this.parentReactionFrame = parentReactionFrame;
		this.Name = reactionName;
		this.Reversible = reversible;
		this.ReactionLocation = reactionLocation;
		this.Reactants = reactants;
		this.Products = products;
	}

	public String generateReactionID() {
		//FIXME need to add correct suffix to names here!!!!!
		String baseID = "";
		if (parentReactionFrame != null) baseID = parentReactionFrame.getLocalID();
		else baseID = Name;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "" + baseID + "_LPAREN_e_RPAREN_");
		else return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "_" + baseID + "_LPAREN_e_RPAREN_");
	}

	@Override
	protected void addReactant(MetaboliteInstance reactant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void addProduct(MetaboliteInstance product) {
		// TODO Auto-generated method stub
		
	}

	public String reactionGeneRule(boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

}

package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashSet;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Instantiated reactions are reactions that take on the properties of their parent reaction, but replace the generic reactants and products of the parent
 * reaction with specific instances of those reactants and products.
 * 
 * @author Jesse Walsh
 */
public class InstantiatedReactionInstance extends AbstractReactionInstance {
	protected Reaction parentReactionFrame_;

	public static ArrayList<AbstractReactionInstance> getInstantiatedReactions(ReactionInstance reaction) {
		//TODO
		return null;
	}
			
	public InstantiatedReactionInstance(Reaction parentReactionFrame, String reactionName, boolean reversible, String reactionLocation, HashSet<MetaboliteInstance> reactants, HashSet<MetaboliteInstance> products) {
		this.parentReactionFrame_ = parentReactionFrame;
		this.name_ = reactionName;
		this.reversible_ = reversible;
		this.reactionLocation_ = reactionLocation;
		this.reactants_ = reactants;
		this.products_ = products;
	}

	public String generateReactionID() {
		//FIXME need to add correct suffix to names here!!!!!
		String baseID = "";
		if (parentReactionFrame_ != null) baseID = parentReactionFrame_.getLocalID();
		else baseID = name_;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.parameters.ReactionPrefix + "" + baseID + "_LPAREN_e_RPAREN_");
		else return CycModeler.convertToSBMLSafe(CycModeler.parameters.ReactionPrefix + "_" + baseID + "_LPAREN_e_RPAREN_");
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

	@Override
	public String getGeneProteinReactionRule() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printReaction() {
		String printString = "";
		printString += "InstantiatedReactionInstance" + "\t" + this.name_ + "\t" + this.parentReactionFrame_.getLocalID() + "\t";
		for (MetaboliteInstance reactant : reactants_) {
			printString += reactant.getMetaboliteID() + "\t";
		}
		for (MetaboliteInstance product : products_) {
			printString += product.getMetaboliteID() + "\t";
		}
		return printString;
	}

}

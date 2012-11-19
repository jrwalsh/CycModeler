package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashSet;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Diffusion reactions, or more specifically, passive diffusion reactions, are represented by this class.  Since passive diffusion reactions are not included
 * in EcoCyc, these reactions must be inferred from the available information.  This means that these reactions do not have any EcoCyc Frames to refer to. Also,
 * passive diffusion reactions are always set as reversible.
 * 
 * @author Jesse Walsh
 */
public class DiffusionReactionInstance extends AbstractReactionInstance {
	public String SecondaryReactionLocation;
	
	public DiffusionReactionInstance(String reactionName, String fromCompartment, String toCompartment, HashSet<MetaboliteInstance> reactants, HashSet<MetaboliteInstance> products) {
		this.name_ = reactionName;
		this.reversible_ = true;
		this.reactionLocation_ = fromCompartment;
		this.SecondaryReactionLocation = toCompartment;
		this.reactants_ = reactants;
		this.products_ = products;
	}
	
	/**
	 * The convention in the iAF1260 model is to add the suffix "tex" to the end of reaction IDs for diffusion from extracellular to periplasm, "texi" for
	 * irreversible diffusion from extracellular to periplasm, and "tpp" for periplasm to cytoplasm.
	 */
	@Override
	public String generateReactionID() {
		//FIXME need to add correct suffix to names here!!!!!
		String baseID = name_;

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

	@Override
	public String getGeneProteinReactionRule() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printReaction() {
		String printString = "";
		printString += "DiffusionReactionsInstance" + "\t" + this.name_ + "\t" + "" + "\t";
		for (MetaboliteInstance reactant : reactants_) {
			printString += reactant.getMetaboliteID();
		}
		for (MetaboliteInstance product : products_) {
			printString += product.getMetaboliteID();
		}
		return printString;
	}

}

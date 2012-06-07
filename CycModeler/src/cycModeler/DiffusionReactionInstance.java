package cycModeler;

import java.util.ArrayList;

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
public class DiffusionReactionInstance extends ReactionInstance {

	public DiffusionReactionInstance(String reactionName, ArrayList<MetaboliteInstance> reactants, ArrayList<MetaboliteInstance> products) {
		super(null, null, reactionName, true, null, reactants, products, false);
	}
	
	/**
	 * The convention in the iAF1260 model is to add the suffix "tex" to the end of reaction IDs for diffusion from extracellular to periplasm, "texi" for
	 * irreversible diffusion from extracellular to periplasm, and "tpp" for periplasm to cytoplasm.
	 */
	protected String generateReactionID() {
		//FIXME need to add correct suffix to names here!!!!!
		String baseID = "";
		if (thisReactionFrame_ != null) baseID = thisReactionFrame_.getLocalID();
		else if (parentReaction_ != null) baseID = parentReaction_.getLocalID();
		else baseID = name_;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "" + baseID + "_LPAREN_e_RPAREN_");
		else return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "_" + baseID + "_LPAREN_e_RPAREN_");
	}

}

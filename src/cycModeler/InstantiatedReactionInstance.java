package cycModeler;

import java.util.ArrayList;

import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Instantiated reactions are reactions that take on the properties of their parent reaction, but have replace the generic reactants and products of the parent
 * reaction with specific instances of those reactants and products.
 * 
 * @author Jesse Walsh
 */
public class InstantiatedReactionInstance extends ReactionInstance {

	public InstantiatedReactionInstance(Reaction parentReactionFrame, String reactionName, boolean reversible, String reactionLocation, ArrayList<MetaboliteInstance> reactants, ArrayList<MetaboliteInstance> products) {
		super(parentReactionFrame, null, reactionName, reversible, reactionLocation, reactants, products, false);
	}

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

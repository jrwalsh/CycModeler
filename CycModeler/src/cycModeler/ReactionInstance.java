package cycModeler;

import java.util.ArrayList;
import edu.iastate.javacyco.Reaction;

/**
 * Internal class which holds all reaction information needed to generate reactions in the SBMLDocument model.
 * 
 * @author Jesse
 */
public class ReactionInstance {
	public Reaction parentReaction;
	public Reaction thisReactionFrame;
	public String name;
	public boolean reversible;
	public ArrayList<MetaboliteInstance> reactants;
	public ArrayList<MetaboliteInstance> products;
	
	public ReactionInstance(Reaction parentReactionFrame, Reaction thisReactionFrame, String name, boolean reversible, ArrayList<MetaboliteInstance> reactants, ArrayList<MetaboliteInstance> products) {
		this.parentReaction = parentReactionFrame;
		this.thisReactionFrame = thisReactionFrame;
		this.name = name;
		this.reversible = reversible;
		this.reactants = reactants;
		this.products = products;
	}
}

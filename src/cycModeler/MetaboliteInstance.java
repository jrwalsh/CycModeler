package cycModeler;

import edu.iastate.javacyco.Frame;

/**
 * Internal class which holds all metabolite information needed to generate species in the SBMLDocument model.  Certain properties imply an association
 * with a certain reaction, such as compartment and stoichiometry.
 * 
 * @author Jesse
 */
public class MetaboliteInstance {
	public Frame metabolite;
	public String compartment;
	public int stoichiometry;
	public String chemicalFormula;
	
	public MetaboliteInstance(Frame metabolite, String compartment, int stoichiometry, String chemicalFormula) {
		this.metabolite = metabolite;
		this.compartment = compartment;
		this.stoichiometry = stoichiometry;
		this.chemicalFormula = chemicalFormula;
	}
}

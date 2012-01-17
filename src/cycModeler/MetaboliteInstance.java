package cycModeler;

import java.util.ArrayList;

import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Represents metabolites in context of a specific reaction.
 * 
 * @author Jesse Walsh
 */
public class MetaboliteInstance {
	public Frame metabolite;
	public String compartment;
	public int stoichiometry;
	public String chemicalFormula;
	public String keggID;
	
	/**
	 * 
	 * @param metabolite
	 * @param compartment
	 * @param stoichiometry
	 */
	public MetaboliteInstance(Frame metabolite, String compartment, int stoichiometry) {
		this.metabolite = metabolite;
		this.compartment = compartment;
		this.stoichiometry = stoichiometry;
		this.chemicalFormula = getChemicalFormula();
	}
	
//	/**
//	 * Intended for instantiating generic reactions. Creates a Metabolite object within the context of a reaction and another metabolite.
//	 * Typical usage would be to provide a generic reaction, a class metabolite for that generic reaction, and an instance of the class
//	 * metabolite. The result would be a Metabolite object borrowing the compartment and coefficient information from the original
//	 * reaction-metabolite pair.
//	 * 
//	 * @param origReaction Original reaction, which combined with origMetabolite provides compartment and coefficient information for
//	 * the resulting MetaboliteInstance object
//	 * @param slot Slot of origReaction containing the origMetabolite (Usually either "RIGHT" or "LEFT")
//	 * @param origMetabolite Original metabolite, which combined with origReaction provides compartment and coefficient information for
//	 * the resulting Metabolite object
//	 * @param newMetabolite Metabolite frame on which the resulting MetaboliteInstance object will be based
//	 */
//	public MetaboliteInstance(JavacycConnection conn, Reaction origReaction, String slot, Frame origMetabolite, Frame newMetabolite) throws PtoolsErrorException {
//		String compartment = conn.getValueAnnot(origReaction.getLocalID(), slot, origMetabolite.getLocalID(), "COMPARTMENT");
//		if (compartment.equalsIgnoreCase("NIL")) compartment = CycModeler.defaultCompartment;
//		
//		int coeficient = 1;
//		try {
//			coeficient = Integer.parseInt(conn.getValueAnnot(origReaction.getLocalID(), slot, origMetabolite.getLocalID(), "COEFFICIENT"));
//		} catch (Exception e) {
//			coeficient = 1;
//		}
//		
//		this.metabolite = newMetabolite;
//		this.compartment = compartment;
//		this.stoichiometry = coeficient;
//		this.chemicalFormula = getChemicalFormula();
//	}
	
	/**
	 * Gets the chemical formula from EcoCyc of given compound. Intended for use as a display string, not for elemental balancing.
	 * 
	 * Note: When comparing chemical formulae for elemental balancing, naming conventions in EcoCyc can differ from standard practice.
	 * This function will translate elements into standard one or two character symbols as found on a periodic table of elements. For
	 * example, EcoCyc lists Cobalt as "COBALT", which is otherwise normally shortened to the symbol "Co". The output of this function
	 * is caps sensitive, as "CO" would stand for carbon and oxygen, rather than "Co" which stands for cobalt. Finally, elements with a 
	 * stoichiometry of 1 do not add the 1 explicitly to the formula.
	 * 
	 * @return Chemical formula of the compound. Returns empty string if no formula information is in EcoCyc.
	 */
	private String getChemicalFormula() {
		String chemicalFormula = "";
		try {
			if (!metabolite.hasSlot("CHEMICAL-FORMULA")) return "";
			for (Object o : metabolite.getSlotValues("CHEMICAL-FORMULA")) {
				String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
				String element = chemicalFormulaElement.split(",")[0];
				Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
				
				// Special Cases
				if (element.equalsIgnoreCase("ACP")) element = "ACP";
				if (element.equalsIgnoreCase("COBALT")) element = "Co";
				if (element.equalsIgnoreCase("FE")) element = "Fe";
				if (element.equalsIgnoreCase("ZN")) element = "Zn";
				if (element.equalsIgnoreCase("SE")) element = "Se";
				if (element.equalsIgnoreCase("NI")) element = "Ni";
				if (element.equalsIgnoreCase("NA")) element = "Na";
				if (element.equalsIgnoreCase("MN")) element = "Mn";
				if (element.equalsIgnoreCase("MG")) element = "Mg";
				if (element.equalsIgnoreCase("HG")) element = "Hg";
				if (element.equalsIgnoreCase("CU")) element = "Cu";
				if (element.equalsIgnoreCase("CD")) element = "Cd";
				if (element.equalsIgnoreCase("CA")) element = "Ca";
				if (element.equalsIgnoreCase("AS")) element = "As";
				if (element.equalsIgnoreCase("CL")) element = "Cl";
				if (element.equalsIgnoreCase("AG")) element = "Ag";
				
				
				if (quantity != 1) chemicalFormula += element + quantity;
				else chemicalFormula += element;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return chemicalFormula;
	}
	
	/**
	 * Gets the Kegg ID of the compound.
	 * 
	 * @return Kegg ID of compound, empty string if no Kegg ID is found in EcoCyc for this compound.
	 */
	@SuppressWarnings("unchecked")
	private String getKeggID() {
		String keggID = "";
		try {
			ArrayList dblinks = null;
			if (metabolite.hasSlot("DBLINKS")) dblinks = metabolite.getSlotValues("DBLINKS");
			for (Object dblink : dblinks) {
				ArrayList<String> dbLinkArray = ((ArrayList<String>)dblink); 
				if (dbLinkArray.get(0).contains("LIGAND-CPD")) {
					keggID += dbLinkArray.get(1).replace("\"", "") + "\t";
				}
			}
			keggID = keggID.split("\t")[0]; // Many kegg id entries are duplicated in EcoCyc v15.0, but we only need one
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return keggID;
	}
	
	/**
	 * Prepends a species prefix to the beginning of the metabolite ID, then appends compartment appreviations to the end of the SBML metabolite ID.  This 
	 * helps to identify metabolite IDs from other ID types in the finished model, as well as allows metabolites in separate compartments to be treated
	 * separately.
	 * 
	 * @param baseID
	 * @param compartment
	 * @return
	 */
	protected String generateSpeciesID() {
		String baseID = metabolite.getLocalID();
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.speciesPrefix + "" + baseID + "_" + CycModeler.compartmentAbrevs.get(compartment));
		else return CycModeler.convertToSBMLSafe(CycModeler.speciesPrefix + "_" + baseID + "_" + CycModeler.compartmentAbrevs.get(compartment));
	}
}

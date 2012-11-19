package edu.iastate.cycmodeler.model;

import java.util.ArrayList;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.PtoolsErrorException;

/**
 * Represents metabolites in context of a specific reaction.
 * 
 * @author Jesse Walsh
 */
public class MetaboliteInstance {
	private Frame metaboliteFrame_;
	public String compartment_;
	public int coefficient_;
	public String chemicalFormula_;
	public String keggID_;
	
	/**
	 * Constructor
	 * 
	 * @param metabolite
	 * @param compartment
	 * @param coefficient
	 */
	public MetaboliteInstance(Frame metabolite, String compartment, int coefficient) {
		metaboliteFrame_ = metabolite;
		compartment_ = compartment;
		coefficient_ = coefficient;
		chemicalFormula_ = fetchChemicalFormula();
		keggID_ = fetchKeggID();
	}
	
	
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
	private String fetchChemicalFormula() {
		String chemicalFormula = "";
		try {
			if (!metaboliteFrame_.hasSlot("CHEMICAL-FORMULA")) return "";
			for (Object o : metaboliteFrame_.getSlotValues("CHEMICAL-FORMULA")) {
				String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
				String element = chemicalFormulaElement.split(",")[0];
				Integer quantity = 1;
				try {
					quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					System.err.println("Error parsing chemical formula : " + o.toString());
				}
				
				// Special Cases
				element = EcoCycElementCode.convertToStandardAbbreviation(element);
				
				if (quantity != 1) chemicalFormula += element + quantity;
				else chemicalFormula += element;
			}
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
	private String fetchKeggID() {
		String keggID = "";
		try {
			ArrayList<String> dblinks = null;
			if (metaboliteFrame_.hasSlot("DBLINKS") && metaboliteFrame_.getSlotValues("DBLINKS") != null) {
				dblinks = metaboliteFrame_.getSlotValues("DBLINKS");
			
				for (Object dblink : dblinks) {
					ArrayList<String> dbLinkArray = ((ArrayList<String>)dblink); 
					if (dbLinkArray.get(0).contains("LIGAND-CPD")) {
						keggID += dbLinkArray.get(1).replace("\"", "") + "\t";
					}
				}
			}
			keggID = keggID.split("\t")[0]; // Many kegg id entries are duplicated in EcoCyc v15.0, but we only need one
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return keggID;
	}
	
	
	// Overrides
	/**
	 * Prepends a species prefix to the beginning of the metabolite ID, then appends compartment abbreviations to the end of the SBML metabolite ID.  This 
	 * helps to identify metabolite IDs from other ID types in the finished model, as well as allows metabolites in separate compartments to be treated
	 * separately.
	 * 
	 * @return
	 */
	public String generateSpeciesID() {
		String baseID = metaboliteFrame_.getLocalID();
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.parameters.SpeciesPrefix + "" + baseID + "_" + CycModeler.parameters.CompartmentAbrevs.get(compartment_));
		else return CycModeler.convertToSBMLSafe(CycModeler.parameters.SpeciesPrefix + "_" + baseID + "_" + CycModeler.parameters.CompartmentAbrevs.get(compartment_));
	}
	
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result
//				+ ((metaboliteFrame_ == null) ? 0 : metaboliteFrame_.getLocalID().hashCode());
//		return result;
//	}

//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		MetaboliteInstance other = (MetaboliteInstance) obj;
//		if (metaboliteFrame_ == null) {
//			if (other.metaboliteFrame_ != null)
//				return false;
//		} else if (!metaboliteFrame_.getLocalID().equals(other.metaboliteFrame_.getLocalID()))
//			return false;
//		return true;
//	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).
	        append(metaboliteFrame_.getLocalID()).
	        append(compartment_).
	        append(coefficient_).
	        toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;

		MetaboliteInstance other = (MetaboliteInstance) obj;
		return new EqualsBuilder().
				append(this.metaboliteFrame_.getLocalID(), other.metaboliteFrame_.getLocalID()).
				append(this.compartment_, other.compartment_).
				append(this.coefficient_, other.coefficient_).
				isEquals();
	}

	
	// Getters and Setters
	public Frame getMetaboliteFrame() {
		return metaboliteFrame_;
	}
	public String getMetaboliteID() {
		return metaboliteFrame_.getLocalID();
	}
	
	// Internal Classes
	/**
	 * Enum class which represents element codes used in EcoCyc.  Most two-letter elements are not listed with a capital first letter and lowercase second.
	 * Some elements are spelled out for ambiguity reasons. Some "elements" are actually complexes made of several elements themselves.  This emum helps
	 * to keep track of these and fix them.
	 * @author jesse
	 */
	public enum EcoCycElementCode	{
		ACP, COBALT, FE, ZN, SE, NI, NA, MN, MG, HG, CU, CD, CA, AS, CL, AG,
		NOVALUE;			
		
		public static EcoCycElementCode getCode(String elementCode) {
	        try {
	            return valueOf(elementCode.toUpperCase());
	        } catch (Exception e) {
	            return NOVALUE;
	        }
		}
		
	    public static String convertToStandardAbbreviation(String elementCode) {
	    	String element = "";
	    	switch (EcoCycElementCode.getCode(elementCode)) {
				case ACP: element = "ACP"; break;
				case COBALT: element = "Co"; break;
				case FE: element = "Fe"; break;
				case ZN: element = "Zn"; break;
				case SE: element = "Se"; break;
				case NI: element = "Ni"; break;
				case NA: element = "Na"; break;
				case MN: element = "Mn"; break;
				case MG: element = "Mg"; break;
				case HG: element = "Hg"; break;
				case CU: element = "Cu"; break;
				case CD: element = "Cd"; break;
				case CA: element = "Ca"; break;
				case AS: element = "As"; break;
				case CL: element = "Cl"; break;
				case AG: element = "Ag"; break;
				default : element = elementCode; break;
	    	}
	    	
	    	return element;
	    }   
	}
}

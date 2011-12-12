package cycModeler;

import java.util.ArrayList;

import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.Compound;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;

public class Diffusion {
	private JavacycConnection conn = null;
	
	public Diffusion (JavacycConnection connection) {
		conn = connection;
	}
	
	public void getSmallMetabolites() {
		ArrayList<Frame> compounds;
		try {
			int count = 0;
			compounds = conn.getAllGFPInstances("|Compounds|");
			for (Frame compound : compounds) {
				try {
					String weight = compound.getSlotValue("MOLECULAR-WEIGHT");
					if (Float.parseFloat(weight) <= 610.00) {
						System.out.println(compound.getCommonName() + " : " + compound.getSlotValue("MOLECULAR-WEIGHT"));
						count++;
					}
				} catch (Exception e) {
					System.err.println(compound.getCommonName() + " : " + compound.getSlotValue("MOLECULAR-WEIGHT"));
				}
			}
			System.out.println(count + "/" + compounds.size());
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
	}
}

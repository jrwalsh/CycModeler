package edu.iastate.cycmodeler.view;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.OrgStruct;
import edu.iastate.javacyco.PtoolsErrorException;

public class DefaultController {
	//Connection Params
	private JavacycConnection conn;
	private String host;
	private int port;
	private String userName;
	private String password;
	
	//Params
	public static JFrame mainJFrame;
	public static MainCardPanel mainCardPanel;
	private WindowAdapter winAdaptor;
	
    public DefaultController() {
    	conn = null;
    	host = null;
    	port = 4444;
    	userName = null;
    	password = null;
    }
    
    // Getters and Setters
    public void setMainJFrame(JFrame jframe) {
    	DefaultController.mainJFrame = jframe;
    }
    
    public void setMainCardPanel(MainCardPanel panel) {
    	DefaultController.mainCardPanel = panel;
    }

    public JavacycConnection getConnection() {
    	return conn;
    }

    
    // Actions
    public void showMainScreen() {
    	mainCardPanel.showLoginCard();
    }
    
    public void connect(String host, int port, String userName, String password) {
    	this.conn = null;
    	this.host = host;
    	this.port = port;
    	this.userName = userName;
    	this.password = password;
    	initiateConnection();
		if (testConnection()) {
			try {
				// Select the first organism as a default
				ArrayList<OrgStruct> orgs = conn.allOrgs();
				if (orgs != null && orgs.size() > 0) {
					conn.selectOrganism(conn.allOrgs().get(0).getLocalID());
				}
				mainCardPanel.showOptionPanel();
			} catch (PtoolsErrorException e) {
				e.printStackTrace();
			}
		} else {
			JOptionPane.showMessageDialog(mainJFrame, "Unable to contact server");
		}
    }
    
    // This method only operates on local information.  A test query must be made to see if the connection is successful.  Use testConnection() for this.
	private void initiateConnection() {
		if (userName == null || userName.equalsIgnoreCase("") || password == null) conn = new JavacycConnection(host, port); 
		else conn = new JavacycConnection(host, port, userName, password);
	}
	
	private boolean testConnection() {
		if (conn == null) return false;
		try {
			ArrayList<OrgStruct> orgs = conn.allOrgs();
			if (orgs != null && orgs.size() > 0) return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

//    public void selectOrganism(String organism) {
//    	dataAccess.selectOrganism(organism);
//    }
//    
//    // Queries
//    public ArrayList<OrgStruct> getAvailableOrganisms() {
//    	return dataAccess.getAvailableOrganisms();
//    }
//    
//	public String getSelectedOrganism() {
//		return dataAccess.getSelectedOrganism();
//	}
//
//	public String getOrganismCommonName(String organismID) {
//		return dataAccess.getOrganismCommonName(organismID);
//	}
}
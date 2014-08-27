package edu.iastate.cycmodeler.view;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class MainCardPanel extends JPanel {
	public final static String loginCard = "Login";
	public final static String optionCard = "Option";

	public MainCardPanel() {
		setPreferredSize(new Dimension(800, 400));
		initComponents();
        localInitialization();
    }

    public void localInitialization() {
    }
    
    private void initComponents() {
    	this.setLayout(new CardLayout(0,0));
	}
    
    public void showLoginCard() {
    	CardLayout cl = (CardLayout)(this.getLayout());
	    cl.show(this, MainCardPanel.loginCard);
    }

	public void showOptionPanel() {
		CardLayout cl = (CardLayout)(this.getLayout());
	    cl.show(this, MainCardPanel.optionCard);
	}
}

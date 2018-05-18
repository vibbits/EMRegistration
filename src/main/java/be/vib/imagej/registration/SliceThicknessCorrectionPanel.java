package be.vib.imagej.registration;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

@SuppressWarnings("serial")
class SliceThicknessCorrectionPanel extends JPanel
{
	private static double minimumThicknessNM = 0.01;
	private static double defaultThicknessNM = 5.0;   // 5 nm slice thickness is reasonably common for FIB SEM
	
	private JCheckBox thicknessCorrectionCheckbox;
	private JFormattedTextField thicknessField;
	
	private double thicknessNM; // in nanometer; only valid if thicknessCorrection==true
	private boolean thicknessCorrection;
	
	public SliceThicknessCorrectionPanel()
	{		
		buildUI();
	}

	private void buildUI()
	{
		setBorder(BorderFactory.createTitledBorder("Slice Thickness Correction"));
		
		NumberFormatter formatter = new NumberFormatter();
		formatter.setMinimum(new Double(minimumThicknessNM));

		thicknessField = new JFormattedTextField(formatter);		
		thicknessField.setValue(new Double(defaultThicknessNM));  
		thicknessField.setColumns(5);
		thicknessField.addPropertyChangeListener("value", e -> { thicknessNM = ((Number)thicknessField.getValue()).doubleValue(); });
		
		JLabel thicknessLabel = new JLabel("Slice thickness:");		
		JLabel thicknessUnits = new JLabel("nm");
		
		thicknessCorrectionCheckbox = new JCheckBox("Correct for slice thickness variation");
		thicknessCorrectionCheckbox.addPropertyChangeListener("value", e -> { thicknessCorrection = thicknessCorrectionCheckbox.isSelected(); });
		
		GroupLayout layout = new GroupLayout(this);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
		   layout.createSequentialGroup()
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
			           .addComponent(thicknessCorrectionCheckbox)
		      		   .addComponent(thicknessLabel))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false) // FIXME: alignment is wrong
		      		   .addComponent(thicknessField))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
		      		   .addComponent(thicknessUnits))
		      );
		
		layout.setVerticalGroup(
		   layout.createSequentialGroup()
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
		    		   .addComponent(thicknessCorrectionCheckbox))
 		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
		    		   .addComponent(thicknessLabel)
		    		   .addComponent(thicknessField)
		    		   .addComponent(thicknessUnits))
		);		
		
		setLayout(layout);
	}
	

	public double thicknessNM()
	{
		return thicknessNM;
	}

	public boolean thicknessCorrection()
	{
		return thicknessCorrection;
	}
	
	public void setEditable(boolean editable)
	{
		thicknessCorrectionCheckbox.setEnabled(editable);
		thicknessField.setEditable(editable);
	}
}
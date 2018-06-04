package be.vib.imagej.registration;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

@SuppressWarnings("serial")
class MaxShiftPanel extends JPanel
{
	private int maxShiftX = 25;
	private int maxShiftY = 25;
	private JFormattedTextField fieldX;
	private JFormattedTextField fieldY;
	
	public MaxShiftPanel()
	{		
		buildUI();
	}

	private void buildUI()
	{
		setBorder(BorderFactory.createTitledBorder("Maximum Image Shift"));
		
		// Only allow integer shifts from 0 to 100.
		NumberFormatter formatter = new NumberFormatter();
		formatter.setMinimum(new Integer(0));
		formatter.setMaximum(new Integer(100));
		// TODO? Warn user about (but do not forbid) large max shifts, because they will be very computationally demanding.
			
		fieldX = new JFormattedTextField(formatter);		
		fieldX.setValue(new Integer(maxShiftX));
		fieldX.setColumns(3);
		fieldX.addPropertyChangeListener("value", e -> { maxShiftX = ((Number)fieldX.getValue()).intValue(); });
		
		fieldY = new JFormattedTextField(formatter);		
		fieldY.setValue(new Integer(maxShiftY));
		fieldY.setColumns(3);
		fieldY.addPropertyChangeListener("value", e -> { maxShiftY = ((Number)fieldY.getValue()).intValue(); });
		
		JLabel labelX = new JLabel("X:");
		JLabel labelY = new JLabel("Y:");
		
		JLabel unitsX = new JLabel("pixels");
		JLabel unitsY = new JLabel("pixels");
		
		GroupLayout layout = new GroupLayout(this);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
		   layout.createSequentialGroup()
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
			           .addComponent(labelX)
		      		   .addComponent(labelY))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
			           .addComponent(fieldX)
		      		   .addComponent(fieldY))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
			           .addComponent(unitsX)
		      		   .addComponent(unitsY))
		      );
		
		layout.setVerticalGroup(
		   layout.createSequentialGroup()
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
		    		   .addComponent(labelX)
		    		   .addComponent(fieldX)
		      		   .addComponent(unitsX))
 		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
		    		   .addComponent(labelY)
		    		   .addComponent(fieldY)
		    		   .addComponent(unitsY))
		);		
		
		setLayout(layout);
	}
	
	public int getMaxShiftX()
	{
		return maxShiftX;
	}

	public int getMaxShiftY()
	{
		return maxShiftY;
	}
	
	public void setEditable(boolean editable)
	{
		fieldX.setEditable(editable);
		fieldY.setEditable(editable);
	}
}
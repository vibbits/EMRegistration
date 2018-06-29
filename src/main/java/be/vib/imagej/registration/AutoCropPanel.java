package be.vib.imagej.registration;

import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class AutoCropPanel extends JPanel
{
	private WizardModel wizardModel;
	private JCheckBox autoCropCheckbox;
	private JLabel autoCropInfo;
	private boolean autoCrop = true;
	
	public AutoCropPanel(WizardModel wizardModel)
	{		
		this.wizardModel = wizardModel;
		
		buildUI();
	}

	private void buildUI()
	{
		setBorder(BorderFactory.createTitledBorder("Auto-crop"));
		
		autoCropCheckbox = new JCheckBox("Remove black border in input images");
		autoCropCheckbox.setSelected(autoCrop);
		autoCropCheckbox.addActionListener(e -> { autoCrop = autoCropCheckbox.isSelected(); updateAutoCropControls(autoCrop); });
		
		autoCropInfo = new JLabel();
		
		updateAutoCropControls(autoCrop);
		
		GroupLayout layout = new GroupLayout(this);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
			layout.createSequentialGroup()
		    	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		    				.addComponent(autoCropCheckbox)
		    				.addComponent(autoCropInfo)));
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addComponent(autoCropCheckbox)
				.addComponent(autoCropInfo));		
		
		setLayout(layout);
	}
	
	private void updateAutoCropControls(boolean enable)
	{
		autoCropInfo.setEnabled(enable);
		updateCropInfoLabel();
	}
	
	public Rectangle getNonblackRegion()  // returns null if no auto-crop needs to be performed
	{
		return autoCrop ? wizardModel.getNonblackRegion() : null;
	}
	
	public void setEditable(boolean editable)
	{
		autoCropCheckbox.setEnabled(editable);
	}
	
	private void updateCropInfoLabel()
	{
		if (autoCrop)
		{
			Rectangle r = wizardModel.getNonblackRegion();
			if (!r.isEmpty())
			{
				autoCropInfo.setText("The input images will be cropped to a rectangle with top-left corner at (" + r.x + ", " + r.y + ") and bottom-right corner at (" + (r.x + r.width - 1) + ", " + (r.y + r.height - 1) + ").");
			}
			else
			{
				autoCropInfo.setText("It seems the input image is completely black. This is suspicious...");
			}
		}
		else
		{
			autoCropInfo.setText("The input images will not be cropped.");
		}
		
	}
}
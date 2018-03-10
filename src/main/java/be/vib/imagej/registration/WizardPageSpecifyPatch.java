package be.vib.imagej.registration;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import ij.ImagePlus;
import ij.gui.RoiListener;

@SuppressWarnings("serial")
public class WizardPageSpecifyPatch extends WizardPage implements RoiListener
{					
	private JLabel infoLabel;
	
	public WizardPageSpecifyPatch(Wizard wizard, String name)
	{
		super(wizard, name);		
		buildUI();
	}

	private void buildUI()
	{		
		infoLabel = new JLabel();
		add(infoLabel);
		handleChange();
	}

	@Override
	public void roiModified(ImagePlus img, int id) // called on the EDT
	{		
		if (img == null)
			return;
		
		if (img != wizard.getModel().referenceImage)
			return;  // We're not interested in ROI changes for an image that is not our reference image
						
		assert(SwingUtilities.isEventDispatchThread());
		handleChange();
	}

	@Override
	public void goingToNextPage() 
	{
		leavePage();
	}

	@Override
	public void goingToPreviousPage()
	{
		leavePage();
	}

	@Override
	public void arriveFromNextPage() 
	{
		enterPage();
	}
	
	@Override
	public void arriveFromPreviousPage()
	{
		enterPage();
	}	
	
	private void enterPage()
	{
		assert(SwingUtilities.isEventDispatchThread());	
		ij.gui.Roi.addRoiListener(this);
		handleChange();		
	}
	
	private void leavePage()
	{
		assert(SwingUtilities.isEventDispatchThread());
		ij.gui.Roi.removeRoiListener(this);  // stop listening to ROI changes	
	}
	
	private void handleChange()  // must be called on the EDT
	{
		if (haveReferenceImageWithRoi())
			infoLabel.setText("The selected ROI will be used for registering the images.");
		else
			infoLabel.setText("Please select a ROI that can be used for registering the images.");
		
		wizard.updateButtons();
	}
	
	private boolean haveReferenceImageWithRoi()
	{
		ImagePlus image = wizard.getModel().referenceImage;
		return image != null && image.getRoi() != null && !image.getRoi().getBounds().isEmpty();
	}
	
	@Override
	public boolean canGoToNextPage()
	{		
    	return haveReferenceImageWithRoi();
	}
}

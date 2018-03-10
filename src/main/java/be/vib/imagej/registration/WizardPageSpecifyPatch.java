package be.vib.imagej.registration;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import ij.ImagePlus;
import ij.gui.RoiListener;

public class WizardPageSpecifyPatch extends WizardPage implements RoiListener
{					
	// TODO: 
	// 1. done
	// 2. done
	// 3. maybe lock the image(s) we work on to avoid that the users closes them while we are busy?
	// 4. change the info message when the user selected a ROI
	
	public WizardPageSpecifyPatch(Wizard wizard, String name)
	{
		super(wizard, name);
		
		buildUI();
	}

	private void buildUI()
	{		
		JLabel label = new JLabel("Please select a ROI with the patch that will be used for registering the image slices.");
		add(label);
	}

	@Override
	public void roiModified(ImagePlus img, int id) // called on the EDT
	{		
		if (img == null)
			return;
		
		if (img != wizard.getModel().referenceImage)
			return;  // We're not interested in ROI changes for an image that is not our first slice image
						
		assert(SwingUtilities.isEventDispatchThread());
		handleChange(); // CHECKME: no need for invokeLater() because we're on the EDT, right?
//		SwingUtilities.invokeLater(() -> { handleChange(); }); 
	}

	@Override
	public void goingToNextPage() 
	{
		assert(SwingUtilities.isEventDispatchThread());
		ij.gui.Roi.removeRoiListener(this);  // stop listening to ROI changes		
	}

	@Override
	public void goingToPreviousPage()
	{
		assert(SwingUtilities.isEventDispatchThread());
		ij.gui.Roi.removeRoiListener(this);  // stop listening to ROI changes	
	}

	@Override
	public void arriveFromNextPage() 
	{
		setupPage();
	}
	
	@Override
	public void arriveFromPreviousPage()
	{
		setupPage();
	}	
	
	private void setupPage()
	{
		assert(SwingUtilities.isEventDispatchThread());	
		ij.gui.Roi.addRoiListener(this);
		handleChange();		
	}
	
	private void handleChange()  // must be called on the EDT
	{
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

package be.vib.imagej.registration;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import ij.ImagePlus;
import ij.gui.RoiListener;

@SuppressWarnings("serial")
public class WizardPageSpecifyPatch extends WizardPage implements RoiListener
{					
	private JLabel infoLabel;
	private JLabel roiLabel;
	
	public WizardPageSpecifyPatch(Wizard wizard, String name)
	{
		super(wizard, name);		
		buildUI();
	}
	
	private void buildUI()
	{		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		infoLabel = new JLabel();
		roiLabel = new JLabel();

		infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		roiLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		add(infoLabel);
		add(roiLabel);

		handleChange();
	}

	@Override
	public void roiModified(ImagePlus img, int id) // called on the EDT
	{		
		if (img == null)
			return;
		
		if (img != wizard.getModel().getReferenceImage())
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
		
//		/// TEST TEST TEST
//		ImagePlus image = wizard.getModel().getReferenceImage();
//		Rectangle r = AutoCropper.getNonblackRegion(image);
//		if (r != null)
//			System.out.println("Non-black: " + r.width + " x " + r.height + " pixels, top left corner at (" + r.x + ", " + r.y + ")");
//		else
//			System.out.println("Non-black: null");
//		/// END TEST TEST TEST
	}
	
	private void leavePage()
	{
		assert(SwingUtilities.isEventDispatchThread());
		ij.gui.Roi.removeRoiListener(this);  // stop listening to ROI changes	
	}
	
	private void handleChange()  // must be called on the Java Event Dispatch Thread (EDT)
	{
		if (haveReferenceImageWithRoi())
		{
			ImagePlus image = wizard.getModel().getReferenceImage();
			Rectangle r = image.getRoi().getBounds();
			
			infoLabel.setText("The selected ROI will be used as a template patch for registering the images.");
			roiLabel.setText("ROI: " + r.width + " x " + r.height + " pixels, top left corner at (" + r.x + ", " + r.y + ")");
			roiLabel.setVisible(true);
		}
		else
		{
			infoLabel.setText("Please select a ROI that can be used for registering the images.");
			roiLabel.setVisible(false);
		}
	
		wizard.updateButtons();
	}
	
	private boolean haveReferenceImageWithRoi()
	{
		ImagePlus image = wizard.getModel().getReferenceImage();
		return image != null && image.getRoi() != null && !image.getRoi().getBounds().isEmpty();
	}
	
	@Override
	public boolean canGoToNextPage()
	{		
    	return haveReferenceImageWithRoi();
	}
}

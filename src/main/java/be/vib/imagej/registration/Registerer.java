package be.vib.imagej.registration;

import java.util.concurrent.Callable;

import be.vib.bits.QFunction;   // IMPROVEME: move the quasar code to be.vib.bits.quasar (or be.vib.quasar).
import be.vib.bits.QValue;
import ij.process.ImageProcessor;


public class Registerer implements Callable<RegistrationResult>
{
//	private final static double alpha = 1.0;
	private final static int searchWindow = 25;  // in pixels, so all shifts dx and dy in -search_window to +search_window are checked exhaustively for the optimal shift
	
	private ImageProcessor referencePatch;  // IMPROVEME: make this a QValue too, since we now repeatedly convert the reference patch to a QValue cube
	private ImageProcessor image;
	private int prevX;
	private int prevY;
	
	public Registerer()
	{
	}
	
	public void setParameters(ImageProcessor image, ImageProcessor referencePatch, int prevX, int prevY)
	{
		this.image = image;
		this.referencePatch = referencePatch;
		this.prevX = prevX;
		this.prevY = prevY;
	}
	
	// Important: call() *must* be run on the Quasar thread!
	@Override
	public RegistrationResult call()
	{		
		QValue img = ImageUtils.newCubeFromImage(image);
		QValue refPatch = ImageUtils.newCubeFromImage(referencePatch);
		
		// Quasar function registration(img, ref_patch, xmin, xmax, ymin, ymax, best_pos, best_patch)
		QFunction registration = new QFunction("registration(mat,mat,int,int,int,int,mat,mat)"); 
		
		QFunction zeros = new QFunction("zeros(...)");

		QValue best_pos = zeros.apply(new QValue(1), new QValue(2));  // a 1x2 placeholder vector for returning the position [y, x] of the best matching patch
		QValue best_patch = zeros.apply(refPatch.size()); // placeholder

		int xmin = Math.max(0, prevX - searchWindow);
		int xmax = Math.min(prevX + searchWindow, image.getWidth() - referencePatch.getWidth() - 1);

		int ymin = Math.max(0, prevY - searchWindow);
		int ymax = Math.min(prevY + searchWindow, image.getHeight() - referencePatch.getHeight() - 1);

		registration.apply(img, refPatch, new QValue(xmin), new QValue(xmax), new QValue(ymin), new QValue(ymax), best_pos, best_patch);

		RegistrationResult result = new RegistrationResult();
		result.posX = best_pos.at(1).getInt();
		result.posY = best_pos.at(0).getInt();
		return result;
	}

}
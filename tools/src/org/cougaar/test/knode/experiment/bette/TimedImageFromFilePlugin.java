package org.cougaar.test.knode.experiment.bette;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class TimedImageFromFilePlugin extends TimedImageBasePlugin{
	
	protected String[] listImages(String dirName) {
		File dir = new File(dirName);   
        String[] fileList = dir.list(new ImageFilter());
        Arrays.sort(fileList);
		return fileList;
	}

	protected byte[] readImage(int imageNumber) throws Exception {
		long imageLength = 0;
		byte[] pixels = null;
		String[] imageNames=getImageNames();
		File imageFile = new File(imageDirectory +File.separator + imageNames[imageNumber]);
		imageLength = imageFile.length();
		log.shout("Image Number=" + imageNumber+ "Image Length="+imageLength);
		FileInputStream imageStream = new FileInputStream(imageFile);

		if (imageLength > Integer.MAX_VALUE) {
			imageStream.close();
			throw new RuntimeException("File too large");
		}
		pixels = new byte[(int) imageLength];
		imageStream.read(pixels);
		imageStream.close();
		return pixels;
	}
	

}
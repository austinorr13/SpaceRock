package fpga;

import java.awt.image.BufferedImage;

/**
 * Produces a boolean 2d array from an image.
 * @author James Holland
 */
class Threshold {

  /**
   * The Gaussian blur will leave a border of untouched pixels.
   * These will need to be ignored.
   */
  private static final int BORDER_SIZE = 1;

  /**
   * Convert an image to a 2d array of booleans
   * True indicates an debris object, False one does not exist.
   * @param image 8-bit grayscale image, must be square.
   * @return 2d array of booleans, true for debris.
   */
  static boolean[][] threshold(BufferedImage image){

    boolean[][] debrisMap = new boolean[image.getHeight()][image.getHeight()];

    for(int i = BORDER_SIZE; i < image.getWidth() - BORDER_SIZE; i++){
      for(int j = BORDER_SIZE; j < image.getHeight() - BORDER_SIZE; j++){
        if(image.getRGB(i, j)>= 128){
          debrisMap[i][j] = true;
        }
      }
    }
    return debrisMap;
  }

}

package org.cougaar.demo.mandelbrot.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Image output utilities.
 */
public final class ImageOutput {

   private ImageOutput() {
   }

   /**
    * Write the image data as a JPEG.
    * 
    * @see #writeImage(int,int,byte[],Palette,String,OutputStream) write the
    *      data using the default palette and "jpg" format.
    */
   public static void writeJPG(int width, int height, byte[] data, OutputStream out) {
      writeImage(width, height, data, null, "jpg", out);
   }

   /**
    * Display the image data in a popup Swing UI.
    * 
    * @see #createImage(int,int,byte[],Palette) creates the image
    * @see #displayImage(Image) creates the popup UI, calls "System.exit" when
    *      the window is closed.
    */
   public static void displayImage(int width, int height, byte[] data) {
      try {
         Image image = createImage(width, height, data, null);
         displayImage(image);
      } catch (Exception e) {
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         }
         throw new RuntimeException("Unable to display image", e);
      }
   }

   /**
    * Write image data to an output stream.
    * 
    * @param width the data width
    * @param height the data height
    * @param data an array with length at least (width * height)
    * @param palette optional color palette, defaults to the default paleete
    * @param format the image format, e.g. "png" or "jpg"
    * @param out the output stream to write the image to
    */
   public static void writeImage(int width, int height, byte[] data, Palette palette, String format, OutputStream out) {
      try {
         Image image = createImage(width, height, data, palette);
         writeImage(image, format, out);
      } catch (Exception e) {
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         }
         throw new RuntimeException("Unable to write image", e);
      }
   }

   /**
    * Create an image from the given data.
    * 
    * @return an Image
    * @see #writeImage(Image,String,OutputStream) write the image to a stream
    * @see #displayImage(Image) create a simple popup UI that displays the image
    */
   public static Image createImage(int width, int height, byte[] data, Palette palette) {
      return createImage(width, height, width, height, data, palette);
   }

   private static Image createImage(int image_width, int image_height, int data_width, int data_height, byte[] data, Palette palette) {
      BufferedImage bufferedImage = new BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_RGB);
      Graphics g = bufferedImage.createGraphics();

      paint(g, image_width, image_height, data_width, data_height, data, palette);

      g.dispose();
      return bufferedImage;
   }

   private static void paint(Graphics g, int image_width, int image_height, int data_width, int data_height, byte[] data,
                             Palette palette) {
      int height = Math.min(data_height, image_height);
      int width = Math.min(data_width, image_width);

      Palette p = palette;
      if (p == null) {
         p = new Palette();
      }

      for (int col = 0; col < height; col++) {
         int col_offset = col * data_width;
         for (int row = 0; row < width; row++) {
            int n = col_offset + row;
            byte b = data[n];
            int i = b & 0xFF;
            Color ci = p.get(i);
            g.setColor(ci);
            g.fillRect(row, col, 1, 1);
         }
      }
   }

   /**
    * Write an image to a file.
    * 
    * @param filename a name ending in either ".jpg" or ".png"
    */
   public static void writeImage(Image image, String filename)
         throws IOException {
      int sep = filename.lastIndexOf('.');
      String format = filename.substring(sep + 1).toLowerCase();
      if (!"jpg".equals(format) && !"png".equals(format)) {
         throw new IllegalArgumentException("Unknown image format: " + filename);
      }

      File file = new File(filename);
      ImageIO.write(((RenderedImage) image), format, file);
   }

   /**
    * Write an image to an output stream.
    * 
    * @param format a supported {@link ImageIO} format, such as "jpg" or "png".
    */
   public static void writeImage(Image image, String format, OutputStream out)
         throws IOException {
      ImageIO.write(((RenderedImage) image), format, out);
   }

   /**
    * Pop-up a simple frame that display the image and calls {@link System#exit}
    * when the "close" box is clicked.
    * <p>
    * This is primarily intended as example code and for debugging purposes.
    */
   public static void displayImage(final Image image) {
      Runnable runner = new Runnable() {
         public void run() {
            JComponent component = new JComponent() {
               private static final long serialVersionUID = 1L;

               @Override
               public void paint(Graphics g) {
                  g.drawImage(image, 0, 0, this);
               }
            };
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(component);
            frame.setSize(image.getWidth(null), image.getHeight(null));
            frame.setVisible(true);
         }
      };
      (new Thread(runner)).start();
   }
}

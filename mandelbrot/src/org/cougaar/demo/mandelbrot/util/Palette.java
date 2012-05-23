package org.cougaar.demo.mandelbrot.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A color palette parser.
 * <p>
 * 
 * @see default_palette.map See "default_palette.map" for the default content
 */
public class Palette
      extends AbstractList<Color> {

   private static final String DEFAULT_FILENAME = "resource:/org/cougaar/demo/mandelbrot/util/default_palette.map";
   private static final int CACHE_SIZE = 5;
   private static final Map<String, Color[]> cache = new CacheLinkedHashMap(CACHE_SIZE);

   private final Color[] colors;

   public Palette() {
      this(-1);
   }

   public Palette(int max) {
      this(null, max);
   }

   public Palette(String filename) {
      this(filename, -1);
   }

   public Palette(String filename, int max) {
      colors = cachedReadPalette(filename, max);
   }

   @Override
   public int size() {
      return colors.length;
   }

   @Override
   public Color get(int index) {
      return colors[index];
   }

   private static Color[] cachedReadPalette(String filename, int max) {
      if (cache == null) {
         return readPalette(filename, max);
      }
      synchronized (cache) {
         String key = filename + ":" + max;
         Color[] ret = cache.get(key);
         if (ret == null) {
            ret = readPalette(filename, max);
            cache.put(key, ret);
         }
         return ret;
      }
   }

   private static Color[] readPalette(String filename, int max) {
      // check for default filename
      String s = filename;
      if (s == null) {
         s = DEFAULT_FILENAME;
      }

      List<Color> l = new ArrayList<Color>();
      try {
         // open stream
         Reader reader;
         if (s.startsWith("resource:")) {
            s = s.substring("resource:".length());
            InputStream in = Palette.class.getResourceAsStream(s);
            if (in == null) {
               throw new RuntimeException("Unable to find resource");
            }
            reader = new InputStreamReader(in);
         } else {
            reader = new FileReader(s);
         }

         // read (RED GREEN BLUE) lines
         BufferedReader br = new BufferedReader(reader);
         Pattern p = Pattern.compile("^(\\d+)\\s+(\\d+)\\s+(\\d+)(\\s.*)?$");
         while (true) {
            String line = br.readLine();
            if (line == null) {
               break;
            }
            int sep = line.indexOf('#');
            if (sep >= 0) {
               line = line.substring(0, sep);
            }
            line = line.trim();
            if (line.length() == 0) {
               continue;
            }
            Matcher m = p.matcher(line);
            if (!m.matches()) {
               continue;
            }
            int r = Integer.parseInt(m.group(1));
            int g = Integer.parseInt(m.group(2));
            int b = Integer.parseInt(m.group(3));
            l.add(new Color(r, g, b));
         }
      } catch (Exception e) {
         System.err.println("Unable to read " + s + ": " + e);
      }

      // handle empty case
      int n = l.size();
      if (n < 2) {
         System.err.println("Pallete " + s + " contains " + n + " colors");
         l.clear();
         l.add(Color.BLACK);
         l.add(Color.WHITE);
         n = l.size();
      }

      // resize to match max-length, convert to array
      int m = (max < 0 ? l.size() - 1 : max);
      Color[] ret = new Color[m + 1];
      if ((m + 1) == n) {
         ret = l.toArray(ret);
      } else {
         double d = ((double) n / (m + 1));
         for (int j = 0; j <= m; j++) {
            int k = (int) (j * d);
            ret[j] = l.get(k);
         }
      }

      return ret;
   }

   private static final class CacheLinkedHashMap
         extends LinkedHashMap<String, Color[]> {
      private static final long serialVersionUID = 1L;

      private CacheLinkedHashMap(int initialCapacity) {
         super(initialCapacity);
      }

      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Color[]> eldest) {
         return (size() > CACHE_SIZE);
      }
   }
}

package org.cougaar.demo.mandelbrot.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A map of key=value pairs with helper methods.
 * <p>
 * TODO merge this back into "org.cougaar.util.Arguments", add support for
 * 
 * <pre>
 *   public List getParameterValues(String key) {...}
 *   public Map getParameterMap() {...}
 * </pre>
 * 
 * as seen in "org.cougaar.core.qos.metrics.ParameterizedPlugin".
 */
public final class Arguments
      extends AbstractMap<String,Object>
      implements Serializable {

   private static final long serialVersionUID = 1L;

   // @see toString(String,String)
   private static final Pattern PATTERN = Pattern.compile("\\$(key|value|vals|vlist)");

   // a non-null, *modifiable* map of String keys to a mix of String and
   // String[] values.
   private final Map<String,Object> m;

   /** @see Arguments(Object,Set,Object) Same as Arguments(null, null, null) */
   public Arguments() {
      this(null, null, null);
   }

   /** @see Arguments(Object,Set,Object) Same as Arguments(o, null, null) */
   public Arguments(Object o) {
      this(o, null, null);
   }

   /** @see Arguments(Object,Set,Map) Same as Arguments(o, null, deflt) */
   public Arguments(Object o, Object deflt) {
      this(o, null, deflt);
   }

   /**
    * @param o the entry data. This can be either<br>
    *        a Map of Strings to Strings/String[]s,<br>
    *        a List of "name=value" Strings,<br>
    *        a String[] of "name=value" Strings,<br>
    *        or null
    * 
    * @param keys an optional Set of Strings to act as filter on the keySet
    * 
    * @param deflt the optional default values. The supported object types are
    *        the same as in the "o" parameter.
    */
   public Arguments(Object o, Set<String> keys, Object deflt) {
      Map<String, Object> m2 = toMap(o);
      Map<String, Object> def = toMap(deflt);
      this.m = parse(m2, keys, def);
   }

   public String getString(String key) {
      return getString(key, null);
   }

   public String getString(String key, String deflt) {
      Object o = m.get(key);
      if (o == null) {
         return deflt;
      }
      if (o instanceof String) {
         return (String) o;
      }
      if (o instanceof String[]) {
         String[] sa = (String[]) o;
         if (sa.length <= 0) {
            return deflt;
         }
         return sa[0];
      }
      return deflt;
   }

   public void setString(String key, String value) {
      m.put(key, value);
   }

   public void swap(String k1, String k2) {
      Object v1 = m.get(k1);
      Object v2 = m.get(k2);
      if (v2 == null) {
         if (v1 != null) {
            m.remove(k1);
         }
      } else {
         m.put(k1, v2);
      }
      if (v1 == null) {
         if (v2 != null) {
            m.remove(k2);
         }
      } else {
         m.put(k2, v1);
      }
   }

   public boolean getBoolean(String key) {
      return getBoolean(key, false);
   }

   public boolean getBoolean(String key, boolean deflt) {
      String value = getString(key);
      return (value == null ? deflt : "true".equals(value));
   }

   public void setBoolean(String key, boolean value) {
      setString(key, Boolean.toString(value));
   }

   public int getInt(String key) {
      return getInt(key, -1);
   }

   public int getInt(String key, int deflt) {
      String value = getString(key);
      if (value == null) {
         return deflt;
      }
      try {
         return Integer.parseInt(value);
      } catch (NumberFormatException nfe) {
         return deflt;
      }
   }

   public void setInt(String key, int value) {
      setString(key, Integer.toString(value));
   }

   public long getLong(String key) {
      return getLong(key, -1);
   }

   public long getLong(String key, long deflt) {
      String value = getString(key);
      if (value == null) {
         return deflt;
      }
      try {
         return Long.parseLong(value);
      } catch (NumberFormatException nfe) {
         return deflt;
      }
   }

   public void setLong(String key, long value) {
      setString(key, Long.toString(value));
   }

   public double getDouble(String key) {
      return getDouble(key, -1.0);
   }

   public double getDouble(String key, double deflt) {
      String value = getString(key);
      if (value == null) {
         return deflt;
      }
      try {
         return Double.parseDouble(value);
      } catch (NumberFormatException nfe) {
         return deflt;
      }
   }

   public void setDouble(String key, double value) {
      setString(key, Double.toString(value));
   }

   /**
    * @return a modifiable set of Map.Entry elements.
    */
   @Override
   public Set<Map.Entry<String, Object>> entrySet() {
      // RFE wrap to ensure that modifications are String->String/String[].
      return m.entrySet();
   }

   /**
    * Create a string representation of this map using the given format.
    * 
    * <pre>
    * For example, if our map contains:
    *   A=B
    *   X=V0,V1,V2
    * then:
    *   toString("the_$key is the_$value\n");
    * would return:
    *   the_A is the_B
    *   the_X is the_V0
    * </pre>
    * 
    * The supported variables are:
    * <ul>
    * <li>"$key" is the string key name (e.g. "X")</li>
    * <li>"$value" is the first value (e.g. "V0"), as defined in
    * {@link #getString(String)}</li>
    * <li>"$vals" is the first value if there is only one value (e.g. "B"),
    * otherwise the list of values prefixed with "[" and "]" if there are
    * multiple values (e.g. "[V0,V1,V2]").</li>
    * <li>"$vlist" is the "[]" wrapped list (e.g. "[B]" or "[V0,V1,V2]")</li>
    * </ul>
    * 
    * @return a string with each entry in the given format, where every "$key"
    *         is replaced with the map key and every "$value" is replaced with
    *         the map value.
    */
   public String toString(String format) {
      return toString(format, null);
   }

   /**
    * @param format optional format, defaults to "$key=$vals"
    * @param separator optional separator, e.g. ", "
    * @see #toString(String) similar string formatter
    */
   public String toString(String format, String separator) {
      String f = format;
      if (f == null) {
         f = "$key=$vals";
      }

      boolean firstTime = false;
      StringBuffer buf = new StringBuffer();
      for (Map.Entry<String, Object> me : m.entrySet()) {
         if (firstTime) {
            if (separator != null) {
               buf.append(separator);
            }
         } else {
            firstTime = true;
         }

         String k = me.getKey();
         Object o = me.getValue();
         String v0;
         String va;
         int n;
         if (o instanceof String) {
            v0 = (String) o;
            va = v0;
            n = 1;
         } else if (o instanceof String[]) {
            String[] sa = (String[]) o;
            n = sa.length;
            if (n <= 0) {
               continue;
            }
            v0 = sa[0];
            if (n == 1) {
               va = v0;
            } else {
               StringBuffer x = new StringBuffer();
               for (int i = 0; i < n; i++) {
                  if (i > 0) {
                     x.append(",");
                  }
                  x.append(sa[i]);
               }
               va = x.toString();
            }
         } else {
            continue;
         }

         Matcher x = PATTERN.matcher(format);
         while (x.find()) {
            String tag = x.group(1);
            String value =
                  ("key".equals(tag) ? k : "value".equals(tag) ? v0 : "vals".equals(tag) ? (n == 1 ? v0 : ("[" + va + "]"))
                        : "vlist".equals(tag) ? va : "InternalError!");
            x.appendReplacement(buf, value);
         }
         x.appendTail(buf);
      }
      return buf.toString();
   }

   @Override
   public String toString() {
      return "[" + toString("$key=$vals", ", ") + "]";
   }

   //
   // constructor helper methods:
   //

   /**
    * @return null or a non-empty, modifiable, ordered map
    */
   private static final Map<String,Object> toMap(Object object) {
      Object o = object;
      if (o == null) {
         return null;
      }
      if (o instanceof Map) {
         @SuppressWarnings("unchecked") // unavoidable?
         Map<String,Object> m2 = (Map<String,Object>) o;
         if (m2.isEmpty()) {
            return null;
         }
         // copy
         Map<String,Object> m = new LinkedHashMap<String,Object>(m2);
         // validate
         for (Map.Entry<String, Object> me : m.entrySet()) {
            Object k = me.getKey();
            if (!(k instanceof String)) {
               throw new IllegalArgumentException("Expecting a Map with String keys, not "
                     + (k == null ? "null" : (k.getClass().getName() + " " + k)));
            }
            Object v = me.getValue();
            if (v instanceof String) {
               continue;
            }
            if (v instanceof String[]) {
               String[] sa = (String[]) v;
               for (int i = 0; i < sa.length; i++) {
                  if (sa[i] != null) {
                     continue;
                  }
                  throw new IllegalArgumentException("Map contains String[] value with null element for key " + k);
               }
               continue;
            }
            throw new IllegalArgumentException("Expecting a Map with String or String[] values, not "
                  + (v == null ? "null" : (v.getClass().getName()) + " " + v));
         }
         return m;
      }
      if (o instanceof String[]) {
         o = Arrays.asList((String[]) o);
      }
      if (!(o instanceof List)) {
         throw new IllegalArgumentException("Expecting a Map, String[], or List, not "
               + (o == null ? "null" : o.getClass().getName()));
      }
      @SuppressWarnings("unchecked") // unavoidable
      List<String> l = (List<String>) o;
      Map<String,Object> m = null;
      for (int i = 0, n = l.size(); i < n; i++) {
         Object oi = l.get(i);
         if (!(oi instanceof String)) {
            throw new IllegalArgumentException("Expecting a List of Strings, not "
                  + (oi == null ? "null" : (oi.getClass().getName() + " " + oi)));
         }
         String s = (String) oi;
         int sep = s.indexOf('=');
         if (sep <= 0) {
            continue;
         }
         String key = s.substring(0, sep).trim();
         String value = s.substring(sep + 1).trim();
         if (key.length() <= 0) {
            continue;
         }
         if (m == null) {
            m = new LinkedHashMap<String,Object>();
         }
         m.put(key, value);
      }
      return m;
   }

   /**
    * @param m a map created by "toMap()"
    * @param deflt a map created by "toMap()"
    * @return a non-null, modifiable, ordered map
    */
   private static final Map<String, Object> parse(Map<String,Object> m, Set<String> keys, Map<String,Object> deflt) {
      Map<String,Object> m2 = new LinkedHashMap<String,Object>();
      if (m != null) {
         if (keys == null) {
            m2.putAll(m);
         } else {
            for (String key : keys) {
               if (!m.containsKey(key)) {
                  continue;
               }
               m2.put(key, m.get(key));
            }
         }
      }
      if (deflt != null) {
         for (Map.Entry<String, Object> me : deflt.entrySet()) {
            String key = me.getKey();
            if (m2.containsKey(key)) {
               continue;
            }
            if (keys != null && !keys.contains(key)) {
               continue;
            }
            m2.put(key, me.getValue());
         }
      }
      return m2;
   }
}

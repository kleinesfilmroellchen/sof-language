package klfr.sof.lang;

@StackableName("String")
public class StringPrimitive extends Primitive {
   private static final long serialVersionUID = 1L;

   private final String s;
   public final long length;

   private StringPrimitive(String s) {
      this.s = s;
      length = s.length();
   }

   @Override
   public Object v() {
      return s;
   }

   public String value() {
      return s;
   }

   public static StringPrimitive createStringPrimitive(String s) {
      return new StringPrimitive(s);
   }

   @Override
   public String toDebugString(DebugStringExtensiveness e) {
      switch (e) {
         case Full:
            return String.format("s\"%s\"(%2d)", this.s, this.length);
         case Compact:
            return '"' + s + '"';
         default:
            return super.toDebugString(e);
      }
   }

   @Override
   public String print() {
      return s;
   }

}

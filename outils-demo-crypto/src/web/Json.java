package web;

import java.util.List;

public class Json {

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String str(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }

    public static String num(Object n) {
        return n == null ? "null" : n.toString();
    }

    public static String bool(boolean b) {
        return Boolean.toString(b);
    }

    public static String arrInt(int[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        return sb.append("]").toString();
    }

    public static String arrRaw(List<String> rawJsonValues) {
        return "[" + String.join(",", rawJsonValues) + "]";
    }

    public static class ObjectBuilder {
        private final StringBuilder sb = new StringBuilder("{");
        private boolean first = true;

        private void sep() {
            if (!first) sb.append(",");
            first = false;
        }

        public ObjectBuilder put(String key, String rawJsonValue) {
            sep();
            sb.append(str(key)).append(":").append(rawJsonValue);
            return this;
        }

        public ObjectBuilder putStr(String key, String value) {
            return put(key, str(value));
        }

        public ObjectBuilder putNum(String key, Object value) {
            return put(key, num(value));
        }

        public ObjectBuilder putBool(String key, boolean value) {
            return put(key, bool(value));
        }

        public String build() {
            return sb.append("}").toString();
        }
    }

    public static ObjectBuilder obj() {
        return new ObjectBuilder();
    }
}

package com.hrms.util;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small JSON writer for payroll DTOs; all control characters are escaped. */
public final class PayrollJson {
    private PayrollJson() { }
    public static Map<String,Object> object(Object... pairs) {
        Map<String,Object> map=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2) map.put((String)pairs[i],pairs[i+1]);
        return map;
    }
    public static String encode(Object value) {
        if(value==null) return "null";
        // Keep DECIMAL(18,2) exact in JavaScript, including values above 2^53.
        if(value instanceof java.math.BigDecimal decimal) return encode(decimal.toPlainString());
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        if(value instanceof Map<?,?> map) {
            java.util.StringJoiner join=new java.util.StringJoiner(",","{","}");
            map.forEach((k,v)->join.add(encode(k.toString())+":"+encode(v)));return join.toString();
        }
        if(value instanceof Iterable<?> values) {
            java.util.StringJoiner join=new java.util.StringJoiner(",","[","]");
            for(Object v:values) join.add(encode(v));return join.toString();
        }
        if(value.getClass().isRecord()) {
            Map<String,Object> map=new LinkedHashMap<>();
            try { for(RecordComponent c:value.getClass().getRecordComponents()) map.put(c.getName(),c.getAccessor().invoke(value)); }
            catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
            return encode(map);
        }
        StringBuilder result=new StringBuilder("\"");
        for(char c:value.toString().toCharArray()) {
            switch(c) {
                case '"' -> result.append("\\\"");case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");case '\r' -> result.append("\\r");case '\t' -> result.append("\\t");
                default -> { if(c<0x20) result.append(String.format("\\u%04x",(int)c));else result.append(c); }
            }
        }
        return result.append('"').toString();
    }
}

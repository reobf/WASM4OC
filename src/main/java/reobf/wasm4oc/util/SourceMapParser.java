package reobf.wasm4oc.util;
import com.google.gson.*;
import java.util.*;

public class SourceMapParser {
    
    public record Triplet(int sourceIndex, int sourceLine, int sourceColumn) {}
    
    public static class SourceMap {
        public List<String> sources;
        public TreeMap<Integer, Triplet> mappings; // wasm offset -> triplet
    }
    
    public static SourceMap parse(String json) {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        
        SourceMap result = new SourceMap();
        

        result.sources = new ArrayList<>();
        JsonArray sources = root.getAsJsonArray("sources");
        for (JsonElement e : sources) {
            result.sources.add(e.getAsString());
        }
        
        result.mappings = new TreeMap<>();
        String mappingsStr = root.get("mappings").getAsString();

        // [0] generated column (wasm offset delta)
        // [1] source file index delta
        // [2] source line delta
        // [3] source column delta
        // [4] names index
        
        int generatedColumn = 0;
        int sourceIndex = 0;
        int sourceLine = 0;
        int sourceColumn = 0;
        
        String[] groups = mappingsStr.split(";");
        int wasmOffset = 0;
        
        for (String group : groups) {
            if (group.isEmpty()) {
                continue;
            }
            String[] segments = group.split(",");
            int segmentColumn = 0;
            
            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                
                int[] fields = decodeVlqSegment(segment);
                
                if (fields.length == 0) continue;
                
                // field[0]: generated column delta 
                generatedColumn += fields[0];
                
                if (fields.length >= 4) {
                    sourceIndex  += fields[1];
                    sourceLine   += fields[2];
                    sourceColumn += fields[3];
                    
                    result.mappings.put(generatedColumn, 
                        new Triplet(sourceIndex, sourceLine, sourceColumn));
                }
            }

        }
        
        return result;
    }
    

    static int[] decodeVlqSegment(String segment) {
        List<Integer> values = new ArrayList<>();
        int i = 0;
        while (i < segment.length()) {
            int[] result = decodeVlq(segment, i);
            values.add(result[0]);
            i = result[1];
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    static int[] decodeVlq(String s, int index) {
        int result = 0;
        int shift = 0;
        int digit;
        do {
            char c = s.charAt(index++);
            digit = BASE64_TABLE.indexOf(c);
            if (digit < 0) throw new RuntimeException("Invalid base64 char: " + c);
            result |= (digit & 0x1F) << shift;
            shift += 5;
        } while ((digit & 0x20) != 0); 
        

        int value = (result & 1) != 0 ? -(result >> 1) : (result >> 1);
        return new int[]{ value, index };
    }
    
    static final String BASE64_TABLE = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
}
package reobf.wasm4oc.util;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.StackFrame;
import com.google.gson.*;
import java.util.*;

public class SourceMapParser {
    
    public record Triplet(int sourceIndex, int sourceLine, int sourceColumn) {}
    
    public static class SourceMap {
        public List<String> sources;
        public TreeMap<Integer, Triplet> mappings; // wasm offset -> triplet
        public List<String> getSource(ArrayList<StackFrame> stack, Instance instance){
        	try {
        	ArrayList<String> ret=new ArrayList<String>();
   
        	for(var s:stack.reversed()) {
        	var get=s.getCurrentInstruction();
        	Integer floorKey;
        	if(get==null) {
        		floorKey=null;//ret.add("No code info for funcid:"+s.funcId);continue;
        	}else
        	{
        	 floorKey = mappings.floorKey(get.address());}
        	if(floorKey!=null) {
        	var val=mappings.get(floorKey);
        	var gg=sources.get(val.sourceIndex);
        	ret.add(gg+" Line:"+val.sourceLine+" Column:"+val.sourceColumn);
        	}else{
        		
        		var fun=instance.imports().function(s.funcId);
        		if(fun instanceof HostFunction host) {
        			
        			host.name();
        			ret.add(host.module()+":"+host.name()+" HostFunction");	
        		}else
        		ret.add("No source for funcid:"+s.funcId);	
        	}
        	
        	}

        	return ret;
        	}catch(Exception e) {e.printStackTrace();
        		return  Arrays.asList("Cannot get StackTrace!");
        	}
        }
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
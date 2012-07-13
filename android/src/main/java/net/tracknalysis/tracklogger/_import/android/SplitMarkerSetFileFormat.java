package net.tracknalysis.tracklogger._import.android;

import java.util.HashMap;
import java.util.Map;

public enum SplitMarkerSetFileFormat {
    
    /**
     * TrackLogger CSV format 1.0.
     */
    CSV_1_0(1);
    
    private final int id;
    
    private SplitMarkerSetFileFormat(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
     
    private static final Map<Integer, SplitMarkerSetFileFormat> intToTypeMap = 
            new HashMap<Integer, SplitMarkerSetFileFormat>();
    
    static {
        for (SplitMarkerSetFileFormat type : SplitMarkerSetFileFormat.values()) {
            intToTypeMap.put(type.getId(), type);
        }
    }

    public static SplitMarkerSetFileFormat fromId(int i) {
        SplitMarkerSetFileFormat type = intToTypeMap.get(Integer.valueOf(i));
        if (type == null) {
            throw new IllegalArgumentException(
                    "No enum const " + i);
        }
        return type;
    }
}

package net.codeoasis.sce_jetbrain.models;

import java.math.BigDecimal;

public class Heartbeat {
    public String entity;
    public Integer lineCount;
    public Integer lineNumber;
    public Integer cursorPosition;
    public BigDecimal timestamp;
    public Boolean isWrite;
    public Boolean isUnsavedFile;
    public String project;
    public String language;
    public Boolean isBuilding;
    public String logTime;

    @Override
    public String toString() {
        return "Heartbeat{"+"project:"+project+" entity='" + entity + "', lineCount=" + lineCount + "', lineNumber="+lineNumber+"', timeStamp="+timestamp+"', logTime="+logTime+"}";
    }

}

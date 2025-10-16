package com.gras_code.sce_jetbrain.models;

public class LineStats {
    public Integer lineCount;
    public Integer lineNumber;
    public Integer cursorPosition;

    public boolean isOK() {
        return lineCount != null && lineNumber != null && cursorPosition != null;
    }
}

package net.codeoasis.sce_jetbrain.models;

import java.math.BigDecimal;

public class SendHeartbeat {
    public String project;
    public String entity;
    public String language;
    public Integer lineCount;
    public Integer lineChange;
    public Integer codeTime;
    public Integer activeCodeTime;
    public BigDecimal timestamp;
}

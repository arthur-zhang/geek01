package me.geek01.javaagent;


import com.alibaba.fastjson.annotation.JSONField;

public abstract class Record {

    @JSONField(name = "appId")
    public String appId = "";
    
    @JSONField(name = "agentId")
    public String agentId = AgentMain.agentId;
    
    public Record() {
    }
}

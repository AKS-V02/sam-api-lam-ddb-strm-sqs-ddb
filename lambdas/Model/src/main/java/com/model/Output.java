package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "OutputDataTable")
public class Output {
    private String user_id;
    private String full_name;
    private String profession;
    private String from_source;
    private String to_source;

    

    public Output() {
    }



    public Output(String user_id, String full_name, String profession, String from_source, String to_source) {
        this.user_id = user_id;
        this.full_name = full_name;
        this.profession = profession;
        this.from_source = from_source;
        this.to_source = to_source;
    }



    @DynamoDBRangeKey(attributeName="user_id")
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    @DynamoDBHashKey(attributeName="full_name")
    public String getFull_name() {
        return full_name;
    }
    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }
    @DynamoDBAttribute(attributeName="profession")
    public String getProfession() {
        return profession;
    }
    public void setProfession(String profession) {
        this.profession = profession;
    }
    @DynamoDBAttribute(attributeName="from_source")
    public String getFrom_source() {
        return from_source;
    }
    public void setFrom_source(String from_source) {
        this.from_source = from_source;
    }
    @DynamoDBAttribute(attributeName="to_source")
    public String getTo_source() {
        return to_source;
    }
    public void setTo_source(String to_source) {
        this.to_source = to_source;
    }

    @Override
    public String toString() {
        return "Output [user_id=" + user_id + ", full_name=" + full_name + ", profession=" + profession
                + ", from_source=" + from_source + ", to_source=" + to_source + "]";
    }
    
    
}

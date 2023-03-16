package com.functions;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
//import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.bazaarvoice.jolt.Chainr;
//import com.bazaarvoice.jolt.JsonUtil;
import com.bazaarvoice.jolt.JsonUtils;
import com.google.gson.Gson;
import com.model.Input;

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
//import java.util.Map;

public class PushData implements RequestHandler<DynamodbEvent, Serializable> {
    
    private final SqsClient sqsClient = SqsClient.builder()
            .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
            .build();
    private AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private DynamoDBMapper mapper = new DynamoDBMapper(client);
    // private static final String tableName= System.getenv("TABLE_NAME");

    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent input, Context context) {
        Gson gson = new Gson();
        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = new ArrayList<>();
        String curRecordSequenceNumber = "";
        String queUrl= System.getenv("QUEUE_URL");
        // String partitionKey= System.getenv("PRIMARY_KEY");
        // String sortKey= System.getenv("SORT_KEY");
        for (DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord : input.getRecords()) {
          try {
                StreamRecord dynamodbRecord = dynamodbStreamRecord.getDynamodb();
                curRecordSequenceNumber = dynamodbRecord.getSequenceNumber();   
                Input inputdata = new Input(dynamodbRecord.getNewImage().get("first_name").getS(), 
                dynamodbRecord.getNewImage().get("id").getS(), 
                dynamodbRecord.getNewImage().get("last_name").getS(), 
                dynamodbRecord.getNewImage().get("job_title").getS(), 
                dynamodbRecord.getNewImage().get("from_source").getS(), 
                dynamodbRecord.getNewImage().get("to_source").getS());

                JoltItem responseInput = mapper.load(JoltItem.class, inputdata.getFrom_source(), 
                inputdata.getTo_source());

                List<Object> specs = JsonUtils.jsonToList(responseInput.getJoltSpec());
                Chainr chainr = Chainr.fromSpec(specs);
                String json = JsonUtils.toJsonString(inputdata);
                Object inputJSON = JsonUtils.jsonToObject(json);
                Object transformedOutput = chainr.transform(inputJSON);

                // String value = dynamodbStreamRecord.getEventSourceARN().split("/")[1];
                // JsonUtils.jsonToList(curRecordSequenceNumber);
                sendSQSMessage(gson.toJson(transformedOutput),queUrl);
                
            } catch (Exception e) {
                batchItemFailures.add(new StreamsEventResponse.BatchItemFailure(curRecordSequenceNumber));
                return new StreamsEventResponse(batchItemFailures);
            }
        }
       
       return new StreamsEventResponse();   
    }


    private String sendSQSMessage(String body, String queueUrl) {

        SendMessageResponse response = sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .delaySeconds(10)
                .build());
        return response.messageId();
    }

    @DynamoDBTable(tableName = "JoltTable")
    public static class JoltItem{
        private String from_source;
        private String to_source;
        private String JoltSpec;

        @DynamoDBHashKey(attributeName="from_source")
        public String getFromSource() {
            return from_source;
        }
        public void setFromSource(String from_source) {
            this.from_source = from_source;
        }

        @DynamoDBRangeKey(attributeName = "to_source")
        public String getToSource() {
            return to_source;
        }
        public void setToSource(String to_source) {
            this.to_source = to_source;
        }

        @DynamoDBAttribute(attributeName = "JoltSpec")
        public String getJoltSpec() {
            return JoltSpec;
        }
        public void setJoltSpec(String JoltSpec) {
            this.JoltSpec = JoltSpec;
        }

        
    }
}
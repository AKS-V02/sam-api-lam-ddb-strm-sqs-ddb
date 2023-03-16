package com.input;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.model.Input;

public class Create implements RequestHandler<APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent>{
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent result = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "*");
        LambdaLogger logger = context.getLogger();

        try {
            String body = input.getBody();
            logger.log("Body is:"+body);
            // String tableName= System.getenv("TABLE_NAME");
            // String primaryKey = System.getenv("PRIMARY_KEY");
            Map<String,String> queryParameter = input.getQueryStringParameters();
            Gson gson = new Gson();
            if(queryParameter!=null && queryParameter.get("isList").equalsIgnoreCase("Y")){
                JsonElement jsonObj = JsonParser.parseString(input.getBody());
                List<Input> inputList = new ArrayList<>();

                // Iterator<?> itr = jsonObj.getAsJsonArray().iterator();

                for(JsonElement obj : jsonObj.getAsJsonArray()){
                    Input inputVal = new Input(obj.getAsJsonObject().get("first_name").getAsString(), 
                    obj.getAsJsonObject().get("last_name").getAsString(), 
                    obj.getAsJsonObject().get("job_title").getAsString(), 
                    obj.getAsJsonObject().get("from_source").getAsString(), 
                    obj.getAsJsonObject().get("to_source").getAsString());
                    logger.log("inputVal is:"+inputVal.toString());
                    inputList.add(inputVal);
                }
                List<FailedBatch> faildItem = mapper.batchSave(inputList);
                // if(faildItem.size()!=0){
                //     faildItem = mapper.batchSave(faildItem);
                // }
                return result.withStatusCode(200).withBody(gson.toJson(faildItem)).withHeaders(headers);
            } else {
                Input inputVal = gson.fromJson(input.getBody(), Input.class);
                logger.log("Body is:"+inputVal.toString());
                mapper.save(inputVal);
                return result.withStatusCode(200).withBody(gson.toJson(inputVal)).withHeaders(headers);
            }
            //logger.log("Body is:"+response);
            //return result.withStatusCode(200).withBody(gson.toJson(response.attributes().get(primaryKey))).withHeaders(headers);
        } catch (Exception e) {
            return result.withStatusCode(400).withBody("Error "+e.getMessage()).withHeaders(headers);
        }
    }
}

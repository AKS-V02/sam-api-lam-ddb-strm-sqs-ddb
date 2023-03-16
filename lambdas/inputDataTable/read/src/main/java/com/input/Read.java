package com.input;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.model.Input;

// import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
// import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
// import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
// import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
// import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

public class Read implements RequestHandler<APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent>{
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        
        APIGatewayProxyResponseEvent result = new APIGatewayProxyResponseEvent();
        // LambdaLogger logger = context.getLogger();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "*");
        
        try {
            Map<String,String> queryParameter = input.getQueryStringParameters();
            Gson gson = new Gson();
            // DynamoDbClient ddb = DynamoDbClient.create();
            // String tableName= System.getenv("TABLE_NAME");
            String primaryKey = System.getenv("PRIMARY_KEY");
            String sortKey = System.getenv("SORT_KEY");
            String lsiKey = System.getenv("LSI_KEY");
            String lsiName = System.getenv("INDEX_NAME");
            if(queryParameter!=null && queryParameter.keySet().contains(primaryKey)){
                if(queryParameter.keySet().contains(sortKey)){
                    Input responseInput = mapper.load(Input.class, queryParameter.get(primaryKey), 
                    queryParameter.get(sortKey));
                    return result.withStatusCode(200).withBody(gson.toJson(responseInput)).withHeaders(headers);
                }else if(queryParameter.keySet().contains(lsiKey)) {
                    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
                    eav.put(":val1", new AttributeValue().withS(queryParameter.get(primaryKey)));
                    eav.put(":val2", new AttributeValue().withS(queryParameter.get(lsiKey)));
                    DynamoDBQueryExpression<Input> quiryExpression = new DynamoDBQueryExpression<Input>().withLimit(3)
                    .withKeyConditionExpression(primaryKey+" = :val1 and "+lsiKey+" = :val2")
                    .withIndexName(lsiName)
                    .withExpressionAttributeValues(eav);
                    if(queryParameter.containsKey("lastId") && queryParameter.containsKey("lastFirstName")){
                        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
                        exclusiveStartKey.put(sortKey,new AttributeValue().withS(queryParameter.get("lastId")));
                        exclusiveStartKey.put(primaryKey,new AttributeValue().withS(queryParameter.get("lastFirstName")));
                        quiryExpression.setExclusiveStartKey(exclusiveStartKey);
                    }
                    QueryResultPage<Input> quiryResponse = mapper.queryPage(Input.class, quiryExpression);
                    return result.withStatusCode(200).withBody(gson.toJson(quiryResponse)).withHeaders(headers);
                } else {
                    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
                    eav.put(":val1", new AttributeValue().withS(queryParameter.get(primaryKey)));
                    DynamoDBQueryExpression<Input> quiryExpression = new DynamoDBQueryExpression<Input>().withLimit(3)
                    .withKeyConditionExpression(primaryKey+" = :val1")
                    .withExpressionAttributeValues(eav);
                    if(queryParameter.containsKey("lastId") && queryParameter.containsKey("lastFirstName")){
                        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
                        exclusiveStartKey.put(sortKey,new AttributeValue().withS(queryParameter.get("lastId")));
                        exclusiveStartKey.put(primaryKey,new AttributeValue().withS(queryParameter.get("lastFirstName")));
                        quiryExpression.setExclusiveStartKey(exclusiveStartKey);
                    }
                    QueryResultPage<Input> quiryResponse = mapper.queryPage(Input.class, quiryExpression);
                    return result.withStatusCode(200).withBody(gson.toJson(quiryResponse)).withHeaders(headers);
                }
                // return result.withStatusCode(200).withBody("gson.toJson(quiryResponse)").withHeaders(headers);
            } else {
                DynamoDBScanExpression scanExpression = new DynamoDBScanExpression().withLimit(3);
                if(queryParameter!=null && queryParameter.containsKey("lastId") && queryParameter.containsKey("lastFirstName")){
                    Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
                    exclusiveStartKey.put(sortKey,new AttributeValue().withS(queryParameter.get("lastId")));
                    exclusiveStartKey.put(primaryKey,new AttributeValue().withS(queryParameter.get("lastFirstName")));
                    scanExpression.setExclusiveStartKey(exclusiveStartKey);
                }
                ScanResultPage<Input> scanResponse = mapper.scanPage(Input.class, scanExpression);
                return result.withStatusCode(200).withBody(gson.toJson(scanResponse)).withHeaders(headers);
            }
           
            // return result.withStatusCode(200).withBody(gson.toJson(scanResponse)).withHeaders(headers);
        } catch (Exception e) {
            return result.withStatusCode(400).withBody(e.getMessage()).withHeaders(headers);
        }
    }
}

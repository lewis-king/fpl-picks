import { DynamoDB } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocument } from '@aws-sdk/lib-dynamodb';

const dynamo = DynamoDBDocument.from(new DynamoDB());


export const handler = async (event) => {
    //console.log('Received event:', JSON.stringify(event, null, 2));

    let body;
    let statusCode = '200';
    const headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': "https://fpl-picks.com",
        'Access-Control-Allow-Headers': "Content-Type",
        'Access-Control-Allow-Methods': "OPTIONS,GET"
    };

    try {
        switch (event.httpMethod) {
            case 'GET':
                const result = await dynamo.scan({ TableName: 'fpl-totw' });
                const rawTotw = result["Items"][0]
                const totwDTO = {
                    gameweek: rawTotw["gameweek"],
                    startingPlayers: rawTotw["startingPlayers"],
                    benchPlayers: rawTotw["benchPlayers"],
                    totalCost: rawTotw["totalCost"],
                    totalPredictedPoints: rawTotw["weightedTotalPredictedPoints"]
                }
                body = totwDTO
                break;
            default:
                throw new Error(`Unsupported method "${event.httpMethod}"`);
        }
    } catch (err) {
        statusCode = '400';
        body = err.message;
    } finally {
        body = JSON.stringify(body);
    }

    return {
        statusCode,
        body,
        headers,
    };
};

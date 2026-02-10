package io.ballerina.asyncapi.core.model;

import java.util.Map;

public record AsyncApiBindings(
        Map<String, Object> http,
        Map<String, Object> ws,
        Map<String, Object> kafka,
        Map<String, Object> amqp,
        Map<String, Object> amqp1,
        Map<String, Object> mqtt,
        Map<String, Object> mqtt5,
        Map<String, Object> nats,
        Map<String, Object> jms,
        Map<String, Object> sns,
        Map<String, Object> sqs,
        Map<String, Object> stomp,
        Map<String, Object> redis,
        Map<String, Object> anypointmq,
        Map<String, Object> solace,
        Map<String, Object> mercure,
        Map<String, Object> ibmmq,
        Map<String, Object> googlepubsub,
        Map<String, Object> pulsar,
        Map<String, String> extensions
) {
}

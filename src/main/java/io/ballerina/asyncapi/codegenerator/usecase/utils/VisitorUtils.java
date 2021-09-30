package io.ballerina.asyncapi.codegenerator.usecase.utils;

import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.models.AaiInfo;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.apicurio.datamodels.core.models.Node;

import java.util.*;

public class VisitorUtils {

    /**
     * ListChannels
     *
     * @param aaiDocument input function name, record name or operation Id
     * @return string with new generated name
     */
    public Map<String, Object> listChannels(AaiDocument aaiDocument, boolean isSchema) {
        Map<String, Object> eventMap = new HashMap<String, Object>();
        for (Iterator<AaiChannelItem> it = aaiDocument.getChannels().iterator(); it.hasNext();) {
            AaiChannelItem channelItem = it.next();
            if(channelItem.subscribe != null && channelItem.subscribe.message.getExtension("x-event-type") != null) {
                String eventName = channelItem.subscribe.message.getExtension("x-event-type").value.toString();
                System.out.println(eventName);
                eventMap.put(eventName, channelItem.subscribe.message);
            }
        }

        return eventMap;
    }

    public Map<String, Object> listChannelEvents(AaiDocument aaiDocument, boolean isSchema) {
        Map<String, Object> eventMap = new HashMap<String, Object>();
        for (Iterator<AaiChannelItem> it = aaiDocument.getChannels().iterator(); it.hasNext();) {
            AaiChannelItem channelItem = it.next();
            if(channelItem.subscribe != null && channelItem.subscribe.message.getExtension("x-event-type") != null) {
                String eventName = channelItem.subscribe.message.getExtension("x-event-type").value.toString();
                System.out.println(eventName);
                eventMap.put(eventName, channelItem.subscribe.message);
            }
        }

        return eventMap;
    }

    public String getEventNamePathComponents(AaiDocument aaiDocument) {
        List<String> eventPath = new ArrayList<>();
        StringBuilder eventPathString = new StringBuilder("genericEvent");
        String fieldType = aaiDocument.getExtension("x-http-event-field-type").value.toString();
        if ("BODY".equals(fieldType)) {
            String eventFieldPath = aaiDocument.getExtension("x-http-event-field").value.toString();
            String [] yamlPathComponents = eventFieldPath.split("/");
            for (String pathComponent: yamlPathComponents) {
                if (!(pathComponent.equals("#") || pathComponent.equals("components") || pathComponent.equals("schemas"))) {
                    eventPath.add(pathComponent);
                }
            }
        }
        for (int i = 1; i < eventPath.size(); i++) { //Omit first element since we've already created records by that name
            String eventPathPart = eventPath.get(i);
            if(i != eventPath.size()) {
                eventPathString.append(".");
            }
            if(Constants.BAL_KEYWORDS.stream()
                    .anyMatch(eventPathPart::equals)) {
                eventPathString.append("`" + eventPathPart);
            } else {
                eventPathString.append(eventPathPart);
            }
        }
        return eventPathString.toString();
    }


    /**
     * ListChannels
     *
     * @param aaiDocument input function name, record name or operation Id
     * @return string with new generated name
     */
    public Node resolveSchemaRef(AaiDocument aaiDocument, String path) {
        String [] pathComponents = path.split("/");
        //Ignore 0 th path component.
        AaiSchema schema = aaiDocument.components.getSchemaDefinition(pathComponents[3]);
        return schema;
    }

    /**
     * ListChannels
     *
     * @param eventName input function name, record name or operation Id
     * @return string with new generated name
     */
    public String formatEventName(String eventName) {
        String sanitizedEventName = eventName.replaceAll("[^a-zA-Z0-9]", "");
        return sanitizedEventName;
    }
}

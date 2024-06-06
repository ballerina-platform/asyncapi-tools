///*
// *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// *
// *  WSO2 Inc. licenses this file to you under the Apache License,
// *  Version 2.0 (the "License"); you may not use this file except
// *  in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing,
// *  software distributed under the License is distributed on an
// *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// *  KIND, either express or implied.  See the License for the
// *  specific language governing permissions and limitations
// *  under the License.
// */
//
//package io.ballerina.asyncapi.codegenerator.entity;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Schema;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * Decorator which implements the interface Schema and purpose of this is to hide the ApiCurio Library entities from
// * the other layers.
// */
//public class SchemaDecorator implements Schema {
//    private final AsyncApi25Schema aaiSchema;
//
//    @Override
//    public Object getExtension(String name) {
//        return aaiSchema.getExtraProperty(name);
//    }
//
//    public SchemaDecorator(AsyncApi25Schema aaiSchema) {
//        this.aaiSchema = aaiSchema;
//    }
//
//    @Override
//    public String getRef() {
//        return aaiSchema.get$ref();
//    }
//
//    @Override
//    public void setRef(String ref) {
//        aaiSchema.set$ref(ref);
//    }
//
//    @Override
//    public String getFormat() {
//        return aaiSchema.getFormat();
//    }
//
//    @Override
//    public void setFormat(String format) {
//        aaiSchema.setFormat(format);
//    }
//
//    @Override
//    public String getTitle() {
//        return aaiSchema.getTitle();
//    }
//
//    @Override
//    public void setTitle(String title) {
//        aaiSchema.setTitle(title);
//    }
//
//    @Override
//    public String getDescription() {
//        return aaiSchema.getDescription();
//    }
//
//    @Override
//    public void setDescription(String description) {
//        aaiSchema.setDescription(description);
//    }
//
//    @Override
//    public Object getDefault() {
//        return aaiSchema.getDefault();
//    }
//
//    @Override
//    public void setDefault(Object defaultValue) {
//        aaiSchema.setDefault((JsonNode) defaultValue);
//    }
//
//    @Override
//    public Number getMultipleOf() {
//        return aaiSchema.getMultipleOf();
//    }
//
//    @Override
//    public void setMultipleOf(Number multipleOf) {
//        aaiSchema.setMultipleOf(multipleOf);
//    }
//
//    @Override
//    public Number getMaximum() {
//        return aaiSchema.getMaximum();
//    }
//
//    @Override
//    public void setMaximum(Number maximum) {
//        aaiSchema.setMaximum(maximum);
//    }
//
//    @Override
//    public Boolean getExclusiveMaximum() {
//        return aaiSchema.getExclusiveMaximum();
//    }
//
//    @Override
//    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
//        aaiSchema.setExclusiveMaximum((Number) exclusiveMaximum);
//    }
//
//    @Override
//    public Number getMinimum() {
//        return aaiSchema.minimum;
//    }
//
//    @Override
//    public void setMinimum(Number minimum) {
//        aaiSchema.minimum = minimum;
//    }
//
//    @Override
//    public Boolean getExclusiveMinimum() {
//        return aaiSchema.exclusiveMinimum;
//    }
//
//    @Override
//    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
//        aaiSchema.exclusiveMinimum = exclusiveMinimum;
//    }
//
//    @Override
//    public Number getMaxLength() {
//        return aaiSchema.maxLength;
//    }
//
//    @Override
//    public void setMaxLength(Number maxLength) {
//        aaiSchema.maxLength = maxLength;
//    }
//
//    @Override
//    public Number getMinLength() {
//        return aaiSchema.minLength;
//    }
//
//    @Override
//    public void setMinLength(Number minLength) {
//        aaiSchema.minLength = minLength;
//    }
//
//    @Override
//    public String getPattern() {
//        return aaiSchema.pattern;
//    }
//
//    @Override
//    public void setPattern(String pattern) {
//        aaiSchema.pattern = pattern;
//    }
//
//    @Override
//    public Number getMaxItems() {
//        return aaiSchema.maxItems;
//    }
//
//    @Override
//    public void setMaxItems(Number maxItems) {
//        aaiSchema.maxItems = maxItems;
//    }
//
//    @Override
//    public Number getMinItems() {
//        return aaiSchema.minItems;
//    }
//
//    @Override
//    public void setMinItems(Number minItems) {
//        aaiSchema.minItems = minItems;
//    }
//
//    @Override
//    public Boolean getUniqueItems() {
//        return aaiSchema.uniqueItems;
//    }
//
//    @Override
//    public void setUniqueItems(Boolean uniqueItems) {
//        aaiSchema.uniqueItems = uniqueItems;
//    }
//
//    @Override
//    public Number getMaxProperties() {
//        return aaiSchema.maxProperties;
//    }
//
//    @Override
//    public void setMaxProperties(Number maxProperties) {
//        aaiSchema.maxProperties = maxProperties;
//    }
//
//    @Override
//    public Number getMinProperties() {
//        return aaiSchema.minProperties;
//    }
//
//    @Override
//    public void setMinProperties(Number minProperties) {
//        aaiSchema.minProperties = minProperties;
//    }
//
//    @Override
//    public List<String> getRequired() {
//        return aaiSchema.required;
//    }
//
//    @Override
//    public void setRequired(List<String> required) {
//        aaiSchema.required = required;
//    }
//
//    @Override
//    public List<Object> getEnum() {
//        return aaiSchema.enum_;
//    }
//
//    @Override
//    public void setEnum(List<Object> enumValue) {
//        aaiSchema.enum_ = enumValue;
//    }
//
//    @Override
//    public String getType() {
//        return aaiSchema.type;
//    }
//
//    @Override
//    public void setType(String type) {
//        aaiSchema.type = type;
//    }
//
//    @Override
//    public Object getItems() {
//        if (aaiSchema.items instanceof AaiSchema) {
//            return new SchemaDecorator((AaiSchema) aaiSchema.items);
//        } else if (aaiSchema.items instanceof List) {
//            return ((List<AaiSchema>) aaiSchema.items).stream().map(SchemaDecorator::new).collect(Collectors
//            .toList());
//        }
//        return null;
//    }
//
//    @Override
//    public void setItems(Object items) {
//        aaiSchema.items = items;
//    }
//
//    @Override
//    public List<Schema> getAllOf() {
//        return aaiSchema.allOf.stream().map(SchemaDecorator::new).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Schema> getOneOf() {
//        return aaiSchema.oneOf.stream().map(SchemaDecorator::new).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Schema> getAnyOf() {
//        return aaiSchema.anyOf.stream().map(SchemaDecorator::new).collect(Collectors.toList());
//    }
//
//    @Override
//    public Schema getNot() {
//        return new SchemaDecorator(aaiSchema.not);
//    }
//
//    @Override
//    public Map<String, Schema> getSchemaProperties() {
//        if (aaiSchema.properties == null) {
//            return null;
//        }
//        return aaiSchema.properties.entrySet()
//                .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new SchemaDecorator(e.getValue())));
//    }
//
//    @Override
//    public Object getAdditionalProperties() {
//        return aaiSchema.additionalProperties;
//    }
//
//    @Override
//    public void setAdditionalProperties(Object additionalProperties) {
//        aaiSchema.additionalProperties = additionalProperties;
//    }
//
//    @Override
//    public Boolean getReadOnly() {
//        return aaiSchema.readOnly;
//    }
//
//    @Override
//    public void setReadOnly(Boolean readOnly) {
//        aaiSchema.readOnly = readOnly;
//    }
//
//    @Override
//    public Boolean getWriteOnly() {
//        return aaiSchema.writeOnly;
//    }
//
//    @Override
//    public void setWriteOnly(Boolean writeOnly) {
//        aaiSchema.writeOnly = writeOnly;
//    }
//
//    @Override
//    public String getDiscriminator() {
//        return aaiSchema.discriminator;
//    }
//
//    @Override
//    public void setDiscriminator(String discriminator) {
//        aaiSchema.discriminator = discriminator;
//    }
//
//    @Override
//    public Boolean getDeprecated() {
//        return aaiSchema.deprecated;
//    }
//
//    @Override
//    public void setDeprecated(Boolean deprecated) {
//        aaiSchema.deprecated = deprecated;
//    }
//
//    @Override
//    public Object getExample() {
//        return aaiSchema.example;
//    }
//
//    @Override
//    public void setExample(Object example) {
//        aaiSchema.example = example;
//    }
//
//    @Override
//    public boolean hasExtraProperties() {
//        return aaiSchema.hasExtraProperties();
//    }
//
//    @Override
//    public List<String> getExtraPropertyNamesList() {
//        return aaiSchema.getExtraPropertyNames();
//    }
//
//    @Override
//    public Object getExtraProperty(String name) {
//        return aaiSchema.getExtraProperty(name);
//    }
//}

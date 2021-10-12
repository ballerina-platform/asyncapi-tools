/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.entity;

import java.util.List;
import java.util.Map;

/**
 * Entity used for schema definition.
 */
public interface Schema {
    String getRef();

    void setRef(String ref);

    String getFormat();

    void setFormat(String format);

    String getTitle();

    void setTitle(String title);

    String getDescription();

    void setDescription(String description);

    Object getDefault();

    void setDefault(Object defaultValue);

    Number getMultipleOf();

    void setMultipleOf(Number multipleOf);

    Number getMaximum();

    void setMaximum(Number maximum);

    Boolean getExclusiveMaximum();

    void setExclusiveMaximum(Boolean exclusiveMaximum);

    Number getMinimum();

    void setMinimum(Number minimum);

    Boolean getExclusiveMinimum();

    void setExclusiveMinimum(Boolean exclusiveMinimum);

    Number getMaxLength();

    void setMaxLength(Number maxLength);

    Number getMinLength();

    void setMinLength(Number minLength);

    String getPattern();

    void setPattern(String pattern);

    Number getMaxItems();

    void setMaxItems(Number maxItems);

    Number getMinItems();

    void setMinItems(Number minItems);

    Boolean getUniqueItems();

    void setUniqueItems(Boolean uniqueItems);

    Number getMaxProperties();

    void setMaxProperties(Number maxProperties);

    Number getMinProperties();

    void setMinProperties(Number minProperties);

    List<String> getRequired();

    void setRequired(List<String> required);

    List<Object> getEnum();

    void setEnum(List<Object> enumValue);

    String getType();

    void setType(String type);

    Object getItems();

    void setItems(Object items);

    List<Schema> getAllOf();

    List<Schema> getOneOf();

    List<Schema> getAnyOf();

    Schema getNot();

    Map<String, Schema> getSchemaProperties();

    Object getAdditionalProperties();

    void setAdditionalProperties(Object additionalProperties);

    Boolean getReadOnly();

    void setReadOnly(Boolean readOnly);

    Boolean getWriteOnly();

    void setWriteOnly(Boolean writeOnly);

    String getDiscriminator();

    void setDiscriminator(String discriminator);

    Boolean getDeprecated();

    void setDeprecated(Boolean deprecated);

    Object getExample();

    void setExample(Object example);

    Object getExtension(String name);
}

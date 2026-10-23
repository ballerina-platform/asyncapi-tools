/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.generator.ws.client.model;

/**
 * Represents a generated source file with a name, content, and overwrite policy.
 */
public class GenSrcFile {

    /**
     * Indicates whether a generated file should be overwritten on subsequent runs.
     */
    public enum GenFileType {
        /** Overwritable generated source (regenerated on each run). */
        GEN_SRC(true),
        /** Overwritable utility source (regenerated on each run). */
        UTIL_SRC(true),
        /** Overwritable model source (regenerated on each run). */
        MODEL_SRC(true),
        /** Overwritable test boilerplate (regenerated on each run). */
        TEST_SRC(true),
        /** Overwritable config file (regenerated on each run). */
        CONFIG_SRC(true);

        private final boolean overwritable;

        GenFileType(boolean overwritable) {
            this.overwritable = overwritable;
        }

        /**
         * Returns whether this file type allows overwriting on subsequent runs.
         *
         * @return {@code true} if the file should be overwritten
         */
        public boolean isOverwritable() {
            return overwritable;
        }
    }

    private final GenFileType type;
    private final String fileName;
    private final String content;

    /**
     * Creates a new generated source file.
     *
     * @param type     the file type (governs overwrite policy)
     * @param fileName the output file name
     * @param content  the generated source content
     */
    public GenSrcFile(GenFileType type, String fileName, String content) {
        this.type = type;
        this.fileName = fileName;
        this.content = content;
    }

    /**
     * Returns the file type.
     *
     * @return file type
     */
    public GenFileType getType() {
        return type;
    }

    /**
     * Returns the output file name.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the generated source content.
     *
     * @return content
     */
    public String getContent() {
        return content;
    }
}

/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.cmd.websockets;

import io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.asyncapi.websocketscore.model.GenSrcFile;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.io.File;
import java.util.List;

/**
 * Contains all the util functions used for asyncapi commands.
 *
 */
public class CmdUtils {

    /**
     * This util method is used to generate {@code Diagnostic} for asyncapi command errors.
     */
    public static AsyncApiDiagnostic constructAsyncAPIDiagnostic(String code, String message,
                                                                 DiagnosticSeverity severity, Location location,
                                                                 Object... args) {

        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(code, message, severity);
        if (location == null) {
            location = new ConverterCommonUtils.NullLocation();
        }
        return new AsyncApiDiagnostic(diagnosticInfo, location, args);
    }

    /**
     * This method for setting the file name for generated file.
     *
     * @param listFiles      generated files
     * @param gFile          GenSrcFile object
     * @param duplicateCount add the tag with duplicate number if file already exist
     */
    public static void setGeneratedFileName(List<File> listFiles, GenSrcFile gFile, int duplicateCount) {
        for (File listFile : listFiles) {
            String listFileName = listFile.getName();
            if (listFileName.contains(".") && ((listFileName.split("\\.")).length >= 2) &&
                    (listFileName.split("\\.")[0].equals(gFile.getFileName().split("\\.")[0]))) {
                duplicateCount = 1 + duplicateCount;
            }
        }
        gFile.setFileName(gFile.getFileName().split("\\.")[0] + "." + (duplicateCount) + "." +
                gFile.getFileName().split("\\.")[1]);
    }
}

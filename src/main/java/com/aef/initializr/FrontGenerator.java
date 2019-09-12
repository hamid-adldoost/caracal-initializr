package com.aef.initializr;

import java.io.File;

public class FrontGenerator {

    public String generateStructureOfProject(String projectName, String targetPath) {

        String rootPath = targetPath + "/" + projectName;
        File filePath = new File(rootPath.toString());
        filePath.mkdirs();
        return rootPath;
    }



}

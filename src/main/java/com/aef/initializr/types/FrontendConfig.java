package com.aef.initializr.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FrontendConfig implements Serializable {

    private String projectName;
    private String projectFarsiName;
    private String targetPath;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectFarsiName() {
        return projectFarsiName;
    }

    public void setProjectFarsiName(String projectFarsiName) {
        this.projectFarsiName = projectFarsiName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
}

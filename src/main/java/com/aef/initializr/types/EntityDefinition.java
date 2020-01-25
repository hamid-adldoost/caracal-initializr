package com.aef.initializr.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityDefinition implements Serializable {

    private String header;
    private String name;
    private String farsiName;
    private String label;
    private List<EntityFieldDefinition> entityFieldDefinitionList;
    private Boolean hasForm;
    private boolean enableValidation;
    private boolean hasAttachment;
    private int gridColumns;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFarsiName() {
        return farsiName;
    }

    public void setFarsiName(String farsiName) {
        this.farsiName = farsiName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<EntityFieldDefinition> getEntityFieldDefinitionList() {
        return entityFieldDefinitionList;
    }

    public void setEntityFieldDefinitionList(List<EntityFieldDefinition> entityFieldDefinitionList) {
        this.entityFieldDefinitionList = entityFieldDefinitionList;
    }

    public Boolean getHasForm() {
        return hasForm;
    }

    public void setHasForm(Boolean hasForm) {
        this.hasForm = hasForm;
    }

    public boolean isEnableValidation() {
        return enableValidation;
    }

    public void setEnableValidation(boolean enableValidation) {
        this.enableValidation = enableValidation;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    public int getGridColumns() {
        return gridColumns;
    }

    public void setGridColumns(int gridColumns) {
        this.gridColumns = gridColumns;
    }
}

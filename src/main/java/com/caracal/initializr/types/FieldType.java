package com.caracal.initializr.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldType implements Serializable {

    private Choice type; //DropDown
    private String referenceUrl;//http://localhost:9090/project-name/reasons
    private String defaultValue;//2
    private List<Choice> options;//[{"label": "عدم توانایی فنی", "value": "1"},{"label": "مشکلات اخلاقی", "value": "2"}]
    private String optionLabel;//label
    private String optionValue;//value
    private Integer colspan;
    private Boolean password = false;
    private Choice metaType;

    public Choice getType() {
        return type;
    }

    public void setType(Choice type) {
        this.type = type;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<Choice> getOptions() {
        return options;
    }

    public void setOptions(List<Choice> options) {
        this.options = options;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public Integer getColspan() {
        return colspan;
    }

    public void setColspan(Integer colspan) {
        this.colspan = colspan;
    }

    public Map<String, String> getOptionMap() {
        if (options == null || options.isEmpty())
            return null;
        Map<String, String> optionMap = new HashMap<>();
        options.forEach(i -> {
            optionMap.put(i.getLabel(), i.getValue());
        });
        return optionMap;

    }

    public Boolean getPassword() {
        return password;
    }

    public void setPassword(Boolean password) {
        this.password = password;
    }

    public Choice getMetaType() {
        return metaType;
    }

    public void setMetaType(Choice metaType) {
        this.metaType = metaType;
    }
}

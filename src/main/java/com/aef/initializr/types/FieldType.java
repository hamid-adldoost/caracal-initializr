package com.aef.initializr.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldType implements Serializable {

    private String type; //DropDown
    private String referenceUrl;//http://localhost:9090/project-name/reasons
    private String defaultValue;//2
    private String options;//[{"label": "عدم توانایی فنی", "value": "1"},{"label": "مشکلات اخلاقی", "value": "2"}]
    private String optionLabel;//label
    private String optionValue;//value
    private Integer colspan;
    private Boolean password = false;

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
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

    @JsonIgnore
    public List<String> getOptionLabels() {
        if(options == null || options.isEmpty())
            return null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DropDownType[] list = objectMapper.readValue(options, DropDownType[].class);
            return Arrays.stream(list).map(DropDownType::getLabel).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json is not parsable for option : " + options);
        }
    }

    @JsonIgnore
    public List<String> getOptionValues() {
        if(options == null || options.isEmpty())
            return null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DropDownType[] list = objectMapper.readValue(options, DropDownType[].class);
            return Arrays.stream(list).map(DropDownType::getValue).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json is not parsable for option : " + options);
        }
    }

    public Map<String, String> getOptionMap() {
        if(options == null || options.isEmpty())
            return null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> optionMap = new HashMap<>();
            options = options.replace("'", "\"");
            DropDownType[] list = objectMapper.readValue(options, DropDownType[].class);
            Arrays.stream(list).forEach(i -> {
                optionMap.put(i.getLabel(), i.getValue());
            });
            return optionMap;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json is not parsable for option : " + options);
        }
    }

    public Boolean getPassword() {
        return password;
    }

    public void setPassword(Boolean password) {
        this.password = password;
    }
}

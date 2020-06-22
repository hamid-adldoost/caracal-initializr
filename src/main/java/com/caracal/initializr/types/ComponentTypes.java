package com.caracal.initializr.types;

public enum ComponentTypes {

    INPUT("Input"),
    DROP_DOWN("DropDown"),
    RADIO_BUTTON("RadioButton"),
    ;

    private String value;

    ComponentTypes(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }


}

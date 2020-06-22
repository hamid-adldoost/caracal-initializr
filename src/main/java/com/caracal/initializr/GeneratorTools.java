package com.caracal.initializr;

public class GeneratorTools {

    public static String camelToSnake(String phrase)
    {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        String snake = phrase
                .replaceAll(regex, replacement)
                .toLowerCase();
        System.out.println(snake);
        return snake;
    }

    public static String camelToDashedSnake(String phrase)
    {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1-$2";
        String snake = phrase
                .replaceAll(regex, replacement)
                .toLowerCase();
        System.out.println(snake);
        return snake;
    }

    public static String pascalCaseToCamelCase(String str) {
        if(str == null || str.isEmpty())
            return "";
        String firstChar = str.substring(0, 1);
        return str.replaceFirst(firstChar, firstChar.toLowerCase());
    }

    public static boolean isInteger(String type) {
        switch (type) {
            case "Integer":
            case "int":
            case "Long":
            case "long":
                return true;
        }
        return false;
    }
}

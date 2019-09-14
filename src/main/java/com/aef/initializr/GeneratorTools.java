package com.aef.initializr;

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
}

package com.example.adharpanscanner;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OtherFunctionalities {

     static boolean isValidPANNumber(@NotNull String input) {
        return input.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}");

    }

    static boolean isValidAadharNumber(String str) {
        String regex = "^[2-9]{1}[0-9]{3}\\s[0-9]{4}\\s[0-9]{4}$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        return m.matches();
    }


}

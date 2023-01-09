package com.example.general.logger;

import java.time.LocalDateTime;

public class SimpleLogger {
    private final String FORMAT = "[%s] - [%s]: [%s]";

    public enum LOG_LEVEL {
        DEBUG,
        INFO,
        WARN,
        ERROR
    };

    public void printLog(LOG_LEVEL level, String message) {
        String levelText = "UNKNOWN";

        switch (level) {
            case DEBUG:
                levelText = "DEBUG";
                break;
            case INFO:
                levelText = "INFO";
                break;
            case WARN:
                levelText = "WARN";
                break;
            case ERROR:
                levelText = "ERROR";
                break;
            default:
                break;
        }

        String now = LocalDateTime.now().toString();
        String logText = "[" + levelText + "] - " + now + " - " + message;

        System.out.println(logText);
    }
}

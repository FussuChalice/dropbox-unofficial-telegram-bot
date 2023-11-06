package com.fussuchalice.bot;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The Telemetry class offers functionality to log and display telemetry data for a chatbot.
 */
public class Telemetry {

    /**
     * Outputs the header for the telemetry data table.
     */
    public void outHeader() {
        String header = "CHAT ID | COMMAND | TIME";
        System.out.println(header);
    }

    /**
     * Outputs a row of telemetry data to the table.
     *
     * @param chatId  The chat ID associated with the telemetry data.
     * @param command The command issued in the telemetry data.
     * @param time    The timestamp of the telemetry data.
     */
    public void outTableValue(String chatId, String command, String time) {
        String output = chatId + " | " + command + " | " + time;
        System.out.println(output);
    }

    /**
     * Retrieves the current time and formats it according to the specified format.
     *
     * @return The current time in the format "dd.HH.mm ss:mm:ss".
     */
    public String getCurrentTimeFormatted() {
        // Define the desired date and time format
        SimpleDateFormat sdf = new SimpleDateFormat("dd.HH.mm ss:mm:ss");

        // Get the current date and time
        Date currentTime = new Date();

        // Format the current time using the specified format
        return sdf.format(currentTime);
    }
}
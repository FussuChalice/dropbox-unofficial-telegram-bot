package com.fussuchalice.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * The `Main` class is the entry point for the DropboxUnofficialBot application. It registers and initializes the bot.
 */
public class Main {
    /**
     * The main method that starts the application and registers the DropboxUnofficialBot.
     *
     * @param args The command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new DropboxUnofficialBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

/**
 * The StartController class is responsible for handling the start command in a Telegram bot.
 * It sends a welcome message with a start image to the user.
 */
package com.fussuchalice.bot.controllers;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.fussuchalice.bot.DropboxUnofficialBot;

public class StartController {

    /**
     * Handles the /start command by sending a welcome message and an image to the user.
     *
     * @param bot    The Telegram bot instance.
     * @param chatId The chat identifier to which the message should be sent.
     */
    public static void handleStartCommand(DropboxUnofficialBot bot, long chatId) {
        SendPhoto sendStartPhoto = new SendPhoto();
        sendStartPhoto.setChatId(chatId);

        // Load the start image from the classpath
        InputFile photo = new InputFile(StartController.class.getClassLoader().getResourceAsStream(
                "images/start-image.png"),
                "images/start-image.png"
        );

        sendStartPhoto.setPhoto(photo);
        sendStartPhoto.setCaption("Welcome to the Dropbox Unofficial Bot \uD83D\uDE80\n" +
                "Get started by typing /help to see the list of available commands or simply send your inquiries. I'm here to assist you!");

        try {
            bot.execute(sendStartPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
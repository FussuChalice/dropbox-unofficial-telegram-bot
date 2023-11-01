package com.fussuchalice.bot.controllers;

import com.fussuchalice.bot.DropboxUnofficialBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The LanguageSelectionController class is responsible for managing language selection and messages in a Telegram bot.
 */
public class LanguageSelectionController {
    /**
     * Sends a language selection message with inline keyboard buttons for language options.
     *
     * @param bot     The Telegram bot instance.
     * @param languagePack The language pack containing text messages.
     * @param chatId  The chat identifier where the message will be sent.
     */
    public static void sendLanguageSelectionMessage(DropboxUnofficialBot bot, long chatId, Properties languagePack) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(languagePack.getProperty("select_language"));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton englishButton = new InlineKeyboardButton();
        englishButton.setText("\uD83C\uDDEC\uD83C\uDDE7 English");
        englishButton.setCallbackData("language_en");

        InlineKeyboardButton russianButton = new InlineKeyboardButton();
        russianButton.setText("\uD83C\uDDF7\uD83C\uDDFA Русский");
        russianButton.setCallbackData("language_ru");

        InlineKeyboardButton ukrainianButton = new InlineKeyboardButton();
        ukrainianButton.setText("\uD83C\uDDFA\uD83C\uDDE6 Українська");
        ukrainianButton.setCallbackData("language_uk");

        // Create buttons for language selection
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(englishButton);
        row.add(russianButton);
        row.add(ukrainianButton);

        // Add the row of language buttons to the keyboard
        keyboard.add(row);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the user to indicate a change in language or provide language-specific content.
     *
     * @param bot          The Telegram bot instance.
     * @param chatId       The chat identifier where the message will be sent.
     * @param messageText  The text message to be sent.
     */
    public static void sendLanguageChangedMessage(DropboxUnofficialBot bot, long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a language-specific properties file based on the specified language code.
     *
     * @param code  The language code to determine the language-specific properties file.
     * @return      The loaded language-specific properties.
     */
    public static Properties getLanguageFileByCode(String code) {
        try (InputStream input = DropboxUnofficialBot.class.getClassLoader().getResourceAsStream("i18n/" + code + ".properties")) {

            Properties prop = new Properties();
            assert input != null;
            prop.load(new InputStreamReader(input, StandardCharsets.UTF_8));

            return prop;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Handles callback data and returns the language-specific properties based on the selected language.
     *
     * @param data    The callback data representing the selected language.
     * @param chatId  The chat identifier associated with the user.
     * @return        The language-specific properties for the selected language.
     */
    public static Properties handleCallbackData(String data, long chatId) {
        return switch (data) {
            case "language_ru" -> getLanguageFileByCode("ru");
            case "language_uk" -> getLanguageFileByCode("uk");
            default -> getLanguageFileByCode("en");
        };
    }
}

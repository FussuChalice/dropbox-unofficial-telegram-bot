package com.fussuchalice.bot;

import com.fussuchalice.bot.controllers.DropboxController;
import com.fussuchalice.bot.controllers.LanguageSelectionController;
import com.fussuchalice.bot.controllers.StartController;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;


/**
 * The `DropboxUnofficialBot` class is the main class for the Telegram bot application. It extends TelegramLongPollingBot
 * and handles user interactions and Dropbox integration.
 */
public class DropboxUnofficialBot extends TelegramLongPollingBot {
    private Properties languagePack = LanguageSelectionController.getLanguageFileByCode("en");
    private String USER_ACCESS_TOKEN;

    private final String UPLOAD_FILE_DIR_PATH = "/uploaded_via_bot";

    @Override
    public void onUpdateReceived(Update update) {


        // Handle commands
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            if (messageText.equals("/start")) {
                StartController.handleStartCommand(this, chatId);
                LanguageSelectionController.sendLanguageSelectionMessage(this, chatId, this.languagePack);
                DropboxController.handleAuthCommand(this, chatId, languagePack);

            } else if (messageText.equals("/change_language")) {
                LanguageSelectionController.sendLanguageSelectionMessage(this, chatId, this.languagePack);
            } else if (messageText.equals("/auth")) {
                DropboxController.handleAuthCommand(this, chatId, languagePack);
            } else if (messageText.equals("/help")) {
                sendMessage(chatId, languagePack.getProperty("help_message"));
            }

            // Handle dropbox commands
            else if (messageText.equals("/get_account_info")) {
                DropboxController.handleGetAccountInfo(this, chatId, languagePack, this.USER_ACCESS_TOKEN);
            } else if (messageText.startsWith("/ls ")) {
                DropboxController.handleListDirCommand(this, chatId, languagePack, this.USER_ACCESS_TOKEN, getPathByMessageText(messageText));
            } else if (messageText.startsWith("/share ")) {
                DropboxController.handleShareCommand(this, chatId, languagePack, this.USER_ACCESS_TOKEN, getPathByMessageText(messageText));
            } else if (messageText.startsWith("/move ")) {
                String[] args = messageText.split(" ");
                if (args.length == 3) {
                    String srcPath = args[1].replace("\"", "");
                    String dstPath = args[2].replace("\"", "");

                    System.out.println(srcPath);

                    DropboxController.handleMoveCommand(this, chatId, languagePack, this.USER_ACCESS_TOKEN, srcPath, dstPath);
                } else {
                    sendMessage(chatId, languagePack.getProperty("help_move_command"));
                }
            }

            // Handle user data
            else {
                if (messageText.contains("key:")) {
                    this.USER_ACCESS_TOKEN = messageText.replace("key:", "");

                    this.sendMessage(chatId, languagePack.getProperty("key_saved"));
                }
            }

        }

        else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            long chatId = callbackQuery.getMessage().getChat().getId();

            if (data.contains("language")) {
                this.languagePack = LanguageSelectionController.handleCallbackData(data, chatId);
                LanguageSelectionController.sendLanguageChangedMessage(this, chatId, this.languagePack.getProperty("language_changed"));
            }
        }

        else if (update.hasMessage() && update.getMessage().hasDocument()) {
            long chatId = update.getMessage().getChatId();
            Document document = update.getMessage().getDocument();
            String fileId = document.getFileId();

            if (fileId != null) {
                GetFile getFile = new GetFile();
                getFile.setFileId(fileId);

                try {
                    File file = execute(getFile);

                    String fileUniqueId = document.getFileUniqueId();
                    String fileExtension = getFileExtension(document.getFileName());

                    String currentDir = System.getProperty("user.dir");
                    String localFilePath = currentDir + "\\uploads\\" + fileUniqueId + fileExtension;
                    System.out.println(localFilePath);


                    // Download the file to the local directory
                    try (InputStream inputStream = downloadFileAsStream(file.getFilePath())) {
                        try (OutputStream outputStream = new FileOutputStream(localFilePath)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    DropboxController.handleUploadFiles(this, chatId, languagePack, this.USER_ACCESS_TOKEN, localFilePath, this.UPLOAD_FILE_DIR_PATH);

                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Gets the file extension from a given file path.
     *
     * @param filePath The file path from which to extract the extension.
     * @return The file extension.
     */
    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return filePath.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * Extracts the path from a message text enclosed in double quotes.
     *
     * @param messageText The message text that may contain a path enclosed in double quotes.
     * @return The extracted path or null if not found.
     */
    public static String getPathByMessageText(String messageText) {
        int startQuote = messageText.indexOf("\"");
        int endQuote = messageText.lastIndexOf("\"");

        if (startQuote != -1 && endQuote != -1 && startQuote < endQuote) {
            return messageText.substring(startQuote + 1, endQuote);
        }

        return null;
    }

    /**
     * Sends a text message to the specified chat.
     *
     * @param chatId      The chat ID where the message will be sent.
     * @param messageText The text message to send.
     */
    public void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "dropbox_unofficial_bot";
    }

    @Override
    public String getBotToken() {
        try (InputStream input = DropboxUnofficialBot.class.getClassLoader().getResourceAsStream("config.properties")) {

            Properties prop = new Properties();

            prop.load(input);

            return prop.getProperty("bot.token").toString();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}

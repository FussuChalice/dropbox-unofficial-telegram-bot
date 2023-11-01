package com.fussuchalice.bot.controllers;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.fussuchalice.bot.DropboxUnofficialBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The `DropboxController` class provides methods for interacting with Dropbox through the Dropbox API.
 */
public class DropboxController {
    /**
     * Handles the authorization command by sending an instruction message to the user.
     *
     * @param bot          The bot instance.
     * @param chatId       The chat ID where the message will be sent.
     * @param languagePack The language pack containing text messages.
     */
    public static void handleAuthCommand(DropboxUnofficialBot bot, long chatId, Properties languagePack) {
        SendMessage messageWithAuthInstruction = new SendMessage();
        messageWithAuthInstruction.setChatId(chatId);
        messageWithAuthInstruction.setParseMode("Markdown");
        messageWithAuthInstruction.setText(languagePack.getProperty("dbx_auth_instruction"));

        try {
            bot.execute(messageWithAuthInstruction);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static DbxClientV2 getClientByAccessKey(String accessKey, DropboxUnofficialBot bot) {
        // Create Dropbox client
        DbxRequestConfig config = DbxRequestConfig.newBuilder(bot.getBotUsername()).build();
        return new DbxClientV2(config, accessKey);
    }

    /**
     * Retrieves the Dropbox client using the provided access key.
     *
     * @param accessKey The Dropbox access key.
     * @param bot       The bot instance.
     */
    public static void handleGetAccountInfo(DropboxUnofficialBot bot, long chatId, Properties languagePack, String accessKey) {
        DbxClientV2 clientV2 = getClientByAccessKey(accessKey, bot);
        FullAccount currentAccount;

        try {
            currentAccount = clientV2.users().getCurrentAccount();
        } catch (DbxException e) {
            throw new RuntimeException(e);
        }

        String accountInfo = languagePack.getProperty("dbx_account_info") +
                languagePack.getProperty("dbx_account_info_email") + ": " + currentAccount.getEmail() + "\n" +
                languagePack.getProperty("dbx_account_info_displayed_name") + ": " + currentAccount.getName().getDisplayName() + "\n" +
                languagePack.getProperty("dbx_account_info_country") + ": " + currentAccount.getCountry() + "\n" +
                languagePack.getProperty("dbx_account_info_profile_image_url") + ": " + currentAccount.getProfilePhotoUrl() + "\n";

        bot.sendMessage(chatId, accountInfo);
    }

    /**
     * Handles the command to list files and folders in a given path on Dropbox.
     *
     * @param bot          The bot instance.
     * @param chatId       The chat ID where the messages will be sent.
     * @param languagePack The language pack containing text messages.
     * @param accessKey    The Dropbox access key.
     * @param path         The path to list files and folders from.
     */
    public static void handleListDirCommand(DropboxUnofficialBot bot, long chatId, Properties languagePack, String accessKey, String path) {
        DbxClientV2 clientV2 = getClientByAccessKey(accessKey, bot);
        ListFolderResult result;

        try {
            result = clientV2.files().listFolder(path);
        } catch (DbxException e) {
            throw new RuntimeException(e);
        }

        StringBuilder paths = new StringBuilder();
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                paths.append(metadata.getPathDisplay()).append("\n");
            }

            if (!result.getHasMore()) {
                break;
            }

            try {
                result = clientV2.files().listFolderContinue(result.getCursor());
            } catch (DbxException e) {
                throw new RuntimeException(e);
            }
        }

        String longMessage = paths.toString();

        int chunkSize = 2048;  // Adjust the chunk size as needed

        for (int i = 0; i < longMessage.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, longMessage.length());
            String chunk = longMessage.substring(i, end);

            bot.sendMessage(chatId, chunk);
        }
    }

    /**
     * Handles the command to share a file or folder in Dropbox.
     *
     * @param bot          The bot instance.
     * @param chatId       The chat ID where the message will be sent.
     * @param languagePack The language pack containing text messages.
     * @param accessKey    The Dropbox access key.
     * @param path         The path of the file or folder to share.
     */
    public static void handleShareCommand(DropboxUnofficialBot bot, long chatId, Properties languagePack, String accessKey, String path) {
        DbxClientV2 clientV2 = getClientByAccessKey(accessKey, bot);
        try {
            // Check if a shared link already exists for the file
            SharedLinkMetadata sharedLinkMetadata = getExistingSharedLink(clientV2, path);

            if (sharedLinkMetadata != null) {
                // If a shared link exists, provide the existing link
                bot.sendMessage(chatId, languagePack.getProperty("dbx_share_file_link") + " " + sharedLinkMetadata.getUrl());
            } else {
                // If no shared link exists, create a new one
                sharedLinkMetadata = clientV2.sharing().createSharedLinkWithSettings(path);
                bot.sendMessage(chatId, languagePack.getProperty("dbx_share_file_link") + " " + sharedLinkMetadata.getUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, languagePack.getProperty("dbx_share_file_error"));
        }
    }

    /**
     * Retrieves an existing shared link for a file or folder if it exists.
     *
     * @param client The Dropbox client.
     * @param path   The path of the file or folder.
     * @return The shared link metadata or null if no shared link exists.
     */
    private static SharedLinkMetadata getExistingSharedLink(DbxClientV2 client, String path) {
        try {
            // Attempt to get an existing shared link for the file
            return client.sharing().listSharedLinksBuilder()
                    .withPath(path)
                    .start()
                    .getLinks()
                    .stream()
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handles the command to move a file or folder in Dropbox to a specified destination.
     *
     * @param bot       The bot instance.
     * @param chatId    The chat ID where the message will be sent.
     * @param languagePack The language pack containing text messages.
     * @param accessKey The Dropbox access key.
     * @param srcPath   The source path of the file or folder to move.
     * @param dstPath   The destination path for the move operation.
     */
    public static void handleMoveCommand(DropboxUnofficialBot bot, long chatId, Properties languagePack, String accessKey, String srcPath, String dstPath) {
        DbxClientV2 clientV2 = getClientByAccessKey(accessKey, bot);

        try {
            // Check if the destination path already exists as a folder
            try {
                clientV2.files().getMetadata(dstPath);
                int dotIndex = srcPath.lastIndexOf('.');
                String fileName = srcPath.substring(0, dotIndex);
                String fileExtension = srcPath.substring(dotIndex);
                String uniqueFileName = fileName + "_" + System.currentTimeMillis() + fileExtension;
                dstPath = dstPath + "/" + uniqueFileName;
            } catch (GetMetadataErrorException e) {
                // The destination path doesn't exist, which is expected
            }

            // Perform the move operation
            Metadata result = clientV2.files().move(srcPath, dstPath);

            if (result != null) {
                bot.sendMessage(chatId, languagePack.getProperty("dbx_move_success"));
            } else {
                bot.sendMessage(chatId, languagePack.getProperty("dbx_move_failure"));
            }
        } catch (DbxException e) {
            // Handle other exceptions if necessary
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the command to upload a local file to Dropbox.
     *
     * @param bot          The bot instance.
     * @param chatId       The chat ID where the message will be sent.
     * @param languagePack The language pack containing text messages.
     * @param accessKey    The Dropbox access key.
     * @param localFilePath The local file path to upload.
     * @param remotePath    The destination path in Dropbox for the uploaded file.
     */
    public static void handleUploadFiles(DropboxUnofficialBot bot, long chatId, Properties languagePack, String accessKey, String localFilePath, String remotePath) {
        DbxClientV2 clientV2 = getClientByAccessKey(accessKey, bot);

        try {
            String remoteFilePath = remotePath + "/" + new File(localFilePath).getName();

            // Perform the file upload
            FileMetadata metadata = clientV2.files().uploadBuilder(remoteFilePath)
                    .withMode(WriteMode.ADD)
                    .uploadAndFinish(new FileInputStream(localFilePath));

            // Delete file from storage
            new File(localFilePath).delete();

            bot.sendMessage(chatId, languagePack.getProperty("dbx_upload_success"));

        } catch (DbxException | IOException e) {
            bot.sendMessage(chatId, languagePack.getProperty("dbx_upload_failure"));
        }
    }
}

package com.elytron.api.fs;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Path;

public class FileUtils {
    private static final Logger LOGGER = Logger.getLogger("cs-api");
    private final Path aliasesFilePath;
    private final String prefix;
    private final String resolverName;


    public FileUtils(String aliasesFilePath, String prefix, String resolverName) {
        this.aliasesFilePath = java.nio.file.Paths.get(aliasesFilePath);
        this.prefix = prefix;
        this.resolverName = resolverName;
    }

    /**
     * Utility method which aims to insert a new entry in the external properties file
     * @param newAlias the alias to be inserted
     * @param encryptedCredential the new encrypted credential
     */
    public void addEntry(String newAlias, String encryptedCredential) {
        LOGGER.trace("Inserting new alias \"" + newAlias + "\" in the external properties file");
        StringBuilder sb = new StringBuilder();
        sb.append(newAlias).append("=${").append(this.prefix).append("::").append(this.resolverName).append(":").append(encryptedCredential).append("}").append(System.lineSeparator());
        LOGGER.trace("with content: " + sb);

        try (FileOutputStream fos = new FileOutputStream(this.aliasesFilePath.toFile(), true)) {
            fos.write((sb.toString()).getBytes());
            LOGGER.trace("Aliases external properties file updated with content: " + sb);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Utility method which aims to update a credential inside the external properties file
     * @param myAlias the alias to be updated
     * @param oldEncryptedCredential the encrypted credential to be replaced
     * @param newEncryptedCredential the new encrypted credential
     */
    public void updateEntry(String myAlias, String oldEncryptedCredential, String newEncryptedCredential) {

        LOGGER.trace("Updating alias \"" + myAlias + "\" in the external properties file");
        LOGGER.trace("Old Encrypted Credential: " + oldEncryptedCredential);
        LOGGER.trace("New Encrypted Credential: " + newEncryptedCredential);

        try (Stream<String> lines = Files.lines(this.aliasesFilePath)) {
            List<String> replaced = lines
                    .map(line-> {
                        if (line.startsWith(myAlias)) {
                            LOGGER.trace("Replacing old credential " + oldEncryptedCredential);
                            LOGGER.trace("With new credential: " + newEncryptedCredential);
                            line = line.replace(oldEncryptedCredential, newEncryptedCredential);
                        }
                        LOGGER.trace("New line to store: " + line);
                        LOGGER.trace("**********************************************");
                        return line;

                    })
                    .collect(Collectors.toList());
            Files.write(this.aliasesFilePath, replaced);
            LOGGER.trace("Aliases external properties file updated with content: " + replaced);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Utility method which aims to delete a credential inside the external properties file
     * @param myAlias the alias to be deleted
     */
    public void deleteEntry(String myAlias) {

        Path newAliasesFilePath = Paths.get(this.aliasesFilePath + ".tmp");

        LOGGER.trace("Deleting alias \"" + myAlias + "\" from the external properties file");

        try (Stream<String> lines = Files.lines(this.aliasesFilePath)) {
            List<String> toKeep = lines
                    .filter(line-> !line.startsWith(myAlias))
                    .collect(Collectors.toList());
            LOGGER.trace("Writing temporary Aliases external properties file: " + newAliasesFilePath);
            Files.write(newAliasesFilePath, toKeep);
            LOGGER.trace("Deleting pre-existing Aliases external properties file: " + this.aliasesFilePath);
            Files.delete(this.aliasesFilePath);
            LOGGER.trace("Renaming temporary Aliases external properties file...");
            newAliasesFilePath.toFile().renameTo(this.aliasesFilePath.toFile());
            LOGGER.trace("Aliases external properties file updated removing alias: \"" + myAlias + "\"");

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

}

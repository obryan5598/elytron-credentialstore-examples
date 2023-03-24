package com.elytron.example.fs;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

    private static final String resolverName = System.getProperty("encresolver-name");

    /**
     * Utility method which aims to insert a new entry in the external properties file
     * @param propertiesFilePath he Path of the external properties file containing aliases and encrypted expressions
     * @param newAlias the alias to be inserted
     * @param encryptedCredential the new encrypted credential
     */
    public static void addEntry(java.nio.file.Path propertiesFilePath, String newAlias, String encryptedCredential) {
        StringBuilder sb = new StringBuilder();
        sb.append(newAlias).append("=${ENC::").append(resolverName).append(":").append(encryptedCredential).append("}").append(System.lineSeparator());

        try (FileOutputStream fos = new FileOutputStream(propertiesFilePath.toFile(), true)) {
            fos.write((sb.toString()).getBytes());

        } catch (Exception e) {
            System.err.print(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Utility method which aims to update a credential inside the external properties file
     * @param propertiesFilePath The Path of the external properties file containing aliases and encrypted expressions
     * @param myAlias the alias to be updated
     * @param oldEncryptedCredential the encrypted credential to be replaced
     * @param newEncryptedCredential the new encrypted credential
     */
    public static void updateEntry(java.nio.file.Path propertiesFilePath, String myAlias, String oldEncryptedCredential, String newEncryptedCredential) {

        System.out.println("**********************************************");
        System.out.println("Old Encrypted Credential: " + oldEncryptedCredential);
        System.out.println("New Encrypted Credential: " + newEncryptedCredential);

        try (Stream<String> lines = Files.lines(propertiesFilePath)) {
            List<String> replaced = lines
                    .map(line-> {
                        if (line.startsWith(myAlias)) {
                            System.out.println("Replacing old credential " + oldEncryptedCredential);
                            System.out.println("With new credential: " + newEncryptedCredential);
                            line = line.replace(oldEncryptedCredential, newEncryptedCredential);
                        }
                        System.out.println("New line to store: " + line);
                        System.out.println("**********************************************");
                        return line;

                    })
                    .collect(Collectors.toList());
            Files.write(propertiesFilePath, replaced);
        } catch (Exception e) {
            System.err.print(e.getMessage());
            e.printStackTrace();
        }
    }


    // TODO Develop delete operation
    /**
     * Utility method which aims to delete a credential inside the external properties file
     * @param propertiesFilePath The Path of the external properties file containing aliases and encrypted expressions
     * @param myAlias the alias to be updated
     */
    /*
    public static void deleteEntry(java.nio.file.Path propertiesFilePath, String myAlias) {

        System.out.println("**********************************************");
        System.out.println("Alias to delete: " + myAlias);

        try (Stream<String> lines = Files.lines(propertiesFilePath)) {
            List<String> toDelete = lines
                    .map(line-> {
                        if (line.startsWith(myAlias)) {
                            System.out.println("Line to delete: " + line);
                        }
                        System.out.println("Line to delete: " + line);
                        System.out.println("**********************************************");
                        return line;

                    })
                    .collect(Collectors.toList());
            Files.write(propertiesFilePath, toDelete);
        } catch (Exception e) {
            System.err.print(e.getMessage());
            e.printStackTrace();
        }
    }


     */

}

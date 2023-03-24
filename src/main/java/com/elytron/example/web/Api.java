package com.elytron.example.web;

import com.elytron.example.fs.FileUtils;
import com.elytron.example.utils.Utils;
import org.wildfly.security.credential.SecretKeyCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.encryption.CipherUtil;

import javax.crypto.SecretKey;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@ApplicationPath("/credentialstore")
@Path("/api")
public class Api extends Application {

    private static final String aliasesFile = System.getProperty("aliases-file");
    private static final String eapInstanceConfigurationPath = System.getProperty("configuration-path");
    private static final java.nio.file.Path propertiesFilePath = Paths.get(eapInstanceConfigurationPath + "/" + aliasesFile);

    /**
     * REST API able to resolve the encrypted credential directly from external properties file
     * using its corresponding alias
     *
     * @param myAlias The alias used to retrieve related credential
     * @return
     */
    @GET
    @Path("/decryptExternalCredentialFromAlias/{myAlias}")
    @Produces("application/text")
    public Response decryptExternalCredentialFromAlias(@PathParam("myAlias") String myAlias) {
        SecretKey secretKey;
        List<String> aliasLines;
        String decryptedString;
        String encryptedString;

        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************").append(System.lineSeparator());

        try {
            CredentialStore credentialStore = Utils.provideCredentialStore();
            secretKey = credentialStore.retrieve("mydecryptkey", SecretKeyCredential.class).getSecretKey();

            Stream<String> lines = Files.lines(propertiesFilePath);
            aliasLines = lines
                    .filter(line -> line.startsWith(myAlias))
                    .peek(aliasLine -> System.out.println("Line with given alias: " + aliasLine))
                    .toList();

            if (aliasLines.size() > 0) {
                encryptedString = Utils.retrieveEncryptedCredentialFromPropertiesLine(aliasLines.get(0));
                decryptedString = CipherUtil.decrypt(encryptedString, secretKey);

                sb.append("Alias: ").append(myAlias).append(System.lineSeparator());
                sb.append("Password: ").append(decryptedString).append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                System.out.print(sb);

                return Response.status(Response.Status.OK)
                        .entity(sb.toString())
                        .build();

            } else {
                sb.append("ERROR:").append(System.lineSeparator());
                sb.append("No alias \"").append(myAlias).append(" found.").append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                System.err.print(sb);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(sb.toString())
                        .build();

            }

        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            System.err.print(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();

        }

    }


    /**
     * REST API which inserts a new credential in an external properties file
     *
     * @param myNewAlias      The alias to be inserted
     * @param myNewCredential The credential to be encrypted and stored
     * @return
     */
    @GET
    @Path("/insertPassword/{myNewAlias}/{myNewCredential}")
    @Produces("application/text")
    public Response insertPassword(@PathParam("myNewAlias") String myNewAlias, @PathParam("myNewCredential") String myNewCredential) {
        String newEncryptedCredential;
        String[] oldLine = new String[1];
        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************").append(System.lineSeparator());

        try {
            Stream<String> stream = Files.lines(propertiesFilePath);
            System.out.println("File Content:");
            stream.forEach(line -> {
                System.out.println("Line: " + line);
                if (line.startsWith(myNewAlias)) {
                    oldLine[0] = line;
                }
            });
            stream.close();

            if (oldLine[0] != null) { // if entry already exists
                sb.append("ERROR:").append(System.lineSeparator());
                sb.append("Alias \"").append(myNewAlias).append("\" already exists.").append(System.lineSeparator());
                sb.append("No operation committed. ").append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                System.err.print(sb);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(sb.toString())
                        .build();

            } else { // if entry is new
                newEncryptedCredential = Utils.encryptCredential(myNewCredential);
                FileUtils.addEntry(propertiesFilePath, myNewAlias, newEncryptedCredential);

                sb.append("Inserted alias: ").append(myNewAlias).append(System.lineSeparator());
                sb.append("New Credential: ").append(myNewCredential).append(System.lineSeparator());
                sb.append("New Ciphered Credential: ").append(newEncryptedCredential).append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                System.out.print(sb);

                return Response.status(Response.Status.OK)
                        .entity(sb.toString())
                        .build();

            }

        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            System.err.print(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();

        }

    }


    /**
     * REST API which updates an existing credential in an external properties file
     *
     * @param myAlias         The alias referring to the credential that needs to be updated
     * @param myNewCredential The new credential to be stored
     * @return
     */
    @GET
    @Path("/updatePassword/{myAlias}/{myNewCredential}")
    @Produces("application/text")
    public Response updatePassword(@PathParam("myAlias") String myAlias, @PathParam("myNewCredential") String myNewCredential) {
        String newEncryptedCredential;
        String[] oldLine = new String[1];
        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************").append(System.lineSeparator());

        try {
            Stream<String> stream = Files.lines(propertiesFilePath);
            System.out.println("File Content:");
            stream.forEach(line -> {
                System.out.println("Line: " + line);
                if (line.startsWith(myAlias)) {
                    oldLine[0] = line;
                }
            });
            stream.close();

            if (oldLine[0] != null) { // if entry already exists
                newEncryptedCredential = Utils.encryptCredential(myNewCredential);
                FileUtils.updateEntry(propertiesFilePath, myAlias, Utils.retrieveEncryptedCredentialFromPropertiesLine(oldLine[0]), newEncryptedCredential);

                sb.append("Updated alias: ").append(myAlias).append(System.lineSeparator());
                sb.append("New Credential: ").append(myNewCredential).append(System.lineSeparator());
                sb.append("New Ciphered Credential: ").append(newEncryptedCredential).append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                System.out.print(sb);

                return Response.status(Response.Status.OK)
                        .entity(sb.toString())
                        .build();

            } else { // if entry is new
                sb.append("ERROR:").append(System.lineSeparator());
                sb.append("Alias \"").append(myAlias).append("\" does not exist.").append(System.lineSeparator());
                sb.append("No operation committed. ").append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                System.err.print(sb);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(sb.toString())
                        .build();

            }

        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            System.err.print(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();

        }

    }



    // TODO Develop delete operation
    /**
     * REST API which deletes an existing credential from an external properties file
     *
     * @param myAlias         The alias referring to the credential that needs to be deleted
     * @return
     */
    /*
    @GET
    @Path("/deletePassword/{myAlias}")
    @Produces("application/text")
    public Response deletePassword(@PathParam("myAlias") String myAlias) {

    }
    */
}
package com.elytron.api.web;

import com.elytron.api.fs.FileUtils;
import com.elytron.api.utils.CSPopulator;
import com.elytron.api.utils.CredentialStoreInfo;
import com.elytron.api.utils.Utils;
import org.jboss.logging.Logger;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationPath("/credentialstore")
@Path("/api")
public class Api extends Application {

    private static final Logger LOGGER = Logger.getLogger("cs-api");
    private Utils utils;
    private FileUtils fileUtils;
    private CredentialStoreInfo credentialStoreInfo;

    private void initialize() {
        credentialStoreInfo = CSPopulator.retrieveInformationFromConfiguration();
        fileUtils = new FileUtils(credentialStoreInfo.getAliasesFile(), credentialStoreInfo.getPrefix(), credentialStoreInfo.getResolverName());
        utils = new Utils(credentialStoreInfo);
    }

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

        initialize();

        try {
            CredentialStore credentialStore = utils.provideCredentialStore();
            secretKey = credentialStore.retrieve(credentialStoreInfo.getDecryptKeyAlias(), SecretKeyCredential.class).getSecretKey();

            Stream<String> lines = Files.lines(Paths.get(credentialStoreInfo.getAliasesFile()));
            aliasLines = lines
                    .filter(line -> line.startsWith(myAlias))
                    .peek(aliasLine -> LOGGER.trace("Line with given alias: " + aliasLine))
                    .collect(Collectors.toList());

            if (aliasLines.size() > 0) {
                encryptedString = utils.retrieveEncryptedCredentialFromPropertiesLine(aliasLines.get(0));
                decryptedString = CipherUtil.decrypt(encryptedString, secretKey);

                sb.append("Alias: ").append(myAlias).append(System.lineSeparator());
                sb.append("Password: ").append(decryptedString).append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                LOGGER.trace(sb);

                return Response.status(Response.Status.OK)
                        .entity(sb.toString())
                        .build();

            } else {
                sb.append("ERROR:").append(System.lineSeparator());
                sb.append("No alias \"").append(myAlias).append("\" found.").append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                LOGGER.error(sb);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(sb.toString())
                        .build();

            }

        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.error(sb);
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

        initialize();

        try {
            Stream<String> stream = Files.lines(Paths.get(credentialStoreInfo.getAliasesFile()));
            LOGGER.trace("File Content:");
            stream.forEach(line -> {
                LOGGER.trace("Line: " + line);
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

                LOGGER.error(sb);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(sb.toString())
                        .build();

            } else { // if entry is new
                newEncryptedCredential = utils.encryptCredential(myNewCredential);
                fileUtils.addEntry(myNewAlias, newEncryptedCredential);

                sb.append("Inserted alias: ").append(myNewAlias).append(System.lineSeparator());
                //sb.append("New Credential: ").append(myNewCredential).append(System.lineSeparator());
                sb.append("New Ciphered Credential: ").append(newEncryptedCredential).append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                LOGGER.info(sb);

                return Response.status(Response.Status.OK)
                        .entity(sb.toString())
                        .build();

            }

        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.error(sb);
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

        initialize();

        try {
            Stream<String> stream = Files.lines(Paths.get(credentialStoreInfo.getAliasesFile()));
            LOGGER.trace("File Content:");
            stream.forEach(line -> {
                LOGGER.trace("Line: " + line);
                if (line.startsWith(myAlias)) {
                    oldLine[0] = line;
                }
            });
            stream.close();

            if (oldLine[0] != null) { // if entry already exists
                newEncryptedCredential = utils.encryptCredential(myNewCredential);
                fileUtils.updateEntry(myAlias, utils.retrieveEncryptedCredentialFromPropertiesLine(oldLine[0]), newEncryptedCredential);

                sb.append("Updated alias: ").append(myAlias).append(System.lineSeparator());
                //sb.append("New Credential: ").append(myNewCredential).append(System.lineSeparator());
                sb.append("New Ciphered Credential: ").append(newEncryptedCredential).append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                LOGGER.info(sb);

                return Response.status(Response.Status.OK)
                        .entity(sb.toString())
                        .build();

            } else { // if entry is new
                sb.append("ERROR:").append(System.lineSeparator());
                sb.append("Alias \"").append(myAlias).append("\" does not exist.").append(System.lineSeparator());
                sb.append("No operation committed. ").append(System.lineSeparator());
                sb.append("**********************************************").append(System.lineSeparator());

                LOGGER.error(sb);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(sb.toString())
                        .build();

            }

        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.error(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();

        }

    }



    /**
     * REST API which deletes a credential from an external properties file
     *
     * @param myAlias      The alias to be deleted
     * @return
     */
    @GET
    @Path("/deletePassword/{myAlias}")
    @Produces("application/text")
    public Response deletePassword(@PathParam("myAlias") String myAlias) {
        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************").append(System.lineSeparator());

        initialize();

        try {
            Stream<String> stream = Files.lines(Paths.get(credentialStoreInfo.getAliasesFile()));
            LOGGER.trace("File Content:");
            stream.forEach(line -> LOGGER.trace("Line: " + line));
            stream.close();

            fileUtils.deleteEntry(myAlias);

            sb.append("Deleted alias: ").append(myAlias).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.info(sb);

            return Response.status(Response.Status.OK)
                    .entity(sb.toString())
                    .build();


        } catch (Exception e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.error(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();

        }

    }

}
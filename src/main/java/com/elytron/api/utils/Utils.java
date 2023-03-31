package com.elytron.api.utils;

import com.elytron.api.exception.DecryptMaskedException;
import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.IdentityCredentials;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.WildFlyElytronCredentialStoreProvider;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.util.PasswordBasedEncryptionUtil;

import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger("cs-api");
    private static final String WRONG_MASKED_PASSWORD = "Wrong masked password format. Expected format is \"MASK-<encoded payload>;<salt>;<iteration>\"";
    private static final Provider CREDENTIAL_STORE_PROVIDER = new WildFlyElytronCredentialStoreProvider();
    private static CredentialStoreInfo credentialStoreInfo = null;

    public Utils(CredentialStoreInfo credentialStoreInfo) {
        this.credentialStoreInfo = credentialStoreInfo;
    }

    /**
     * Utility method which encrypts a credential
     * Encryption is guaranteed running elytron-tool.sh script
     * @param myNewCredential The credential which needs to be encrypted
     * @return
     */
    public static String encryptCredential(String myNewCredential) throws IOException {

        Process proc = null;
        String commandOutput = "";
        String newEncryptedCredential = "";
        StringBuilder elytronToolCommand = new StringBuilder();
        Pattern pattern = Pattern.compile("'(.*?)'");

        elytronToolCommand.append(credentialStoreInfo.getElytronToolPath())
                .append(" credential-store --location ")
                .append(credentialStoreInfo.getCsPath())
                .append(" --encrypt ")
                .append(credentialStoreInfo.getDecryptKeyAlias())
                .append(" -p ")
                .append(credentialStoreInfo.getMaskedPassword())
                .append(" --clear-text ")
                .append(myNewCredential);

        LOGGER.trace("elytronToolCommand: " + elytronToolCommand.toString());

        //proc = Runtime.getRuntime().exec("java -jar /path/to/jboss-eap-7.4/jboss-modules.jar -mp /path/to/jboss-eap-7.4/modules/ org.wildfly.security.elytron-tool credential-store --location " + csPath + " --encrypt " + decryptKeyAlias + " -p " + maskedPassword + " --clear-text " + myNewCredential);
        proc = Runtime.getRuntime().exec(elytronToolCommand.toString());

        // Then retreive the process output
        InputStream in = proc.getInputStream();
        InputStream err = proc.getErrorStream();

        if (err != null) {
            commandOutput = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
            Matcher matcher = pattern.matcher(commandOutput);
            if (matcher.find())
            {
                newEncryptedCredential = matcher.group(1);
            }

        }
        LOGGER.trace("New encrypted credential: " + newEncryptedCredential);

        return newEncryptedCredential;

    }


    /**
     * Utility method which retrieves, via regexp, the encrypted credential
     * from the whole line stored in the external properties file
     * @param currentLine The line containing the whole expression such as ${END::encresolver:xxencrypted-credentialxx}
     * @return The encrypted credential
     */
    public static String retrieveEncryptedCredentialFromPropertiesLine(String currentLine) {
        Pattern pattern = Pattern.compile("::" + credentialStoreInfo.getResolverName() + ":(.*)[}]");
        String encryptedCredential = "";

        Matcher matcher = pattern.matcher(currentLine);
        if (matcher.find())
        {
            encryptedCredential = matcher.group(1);
        }

        LOGGER.trace("Encrypted credential retrieved: " + encryptedCredential);

        return encryptedCredential;

    }



    /**
     * Utility method which decrypts a Masked Password, useful to access credential stores
     * @param masked Masked password in form of MASK-xxxxx
     * @return The decrypted Password object
     * @throws Exception
     */
    public static Password decryptMasked(String masked) throws Exception {
        LOGGER.trace("Masked password to decrypt: " + masked);
        int maskLength = "MASK-".length();
        if (masked == null || masked.length() <= maskLength) {
            throw new DecryptMaskedException(WRONG_MASKED_PASSWORD);
        }
        String[] parsed = masked.substring(maskLength).split(";");
        if (parsed.length != 3) {
            throw new DecryptMaskedException(WRONG_MASKED_PASSWORD);
        }
        String encoded = parsed[0];
        String salt = parsed[1];
        int iteration = Integer.parseInt(parsed[2]);

        PasswordBasedEncryptionUtil encryptUtil = new PasswordBasedEncryptionUtil.Builder()
                .picketBoxCompatibility()
                .salt(salt)
                .iteration(iteration)
                .decryptMode()
                .build();

        Password clearPassword = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, encryptUtil.decodeAndDecrypt(encoded));
        LOGGER.trace("Masked password decrypted");
        return clearPassword;

    }


    public static CredentialStore provideCredentialStore() throws Exception {
        Password storePassword = decryptMasked(credentialStoreInfo.getMaskedPassword());
        CredentialStore.ProtectionParameter protectionParameter = new CredentialStore.CredentialSourceProtectionParameter(IdentityCredentials.NONE.withCredential(new PasswordCredential(storePassword)));
        CredentialStore credentialStore = CredentialStore.getInstance("KeyStoreCredentialStore", CREDENTIAL_STORE_PROVIDER);

        Map<String, String> configuration = new HashMap<>();
        configuration.put("location", credentialStoreInfo.getCsPath());
        configuration.put("keyStoreType", credentialStoreInfo.getCsKeyStoreType());
        configuration.put("modifiable", "true");

        credentialStore.initialize(configuration, protectionParameter);
        LOGGER.trace("Providing credential store...");

        return credentialStore;

    }

}

package com.elytron.example.utils;

import com.elytron.example.exception.DecryptMaskedException;
import org.wildfly.security.auth.server.IdentityCredentials;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.WildFlyElytronCredentialStoreProvider;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.util.PasswordBasedEncryptionUtil;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final String WRONG_MASKED_PASSWORD = "Wrong masked password format. Expected format is \"MASK-<encoded payload>;<salt>;<iteration>\"";
    private static final Provider CREDENTIAL_STORE_PROVIDER = new WildFlyElytronCredentialStoreProvider();
    private static final String resolverName = System.getProperty("encresolver-name");
    private static final String csPath = System.getProperty("cs-path");
    private static final String maskedPassword = System.getProperty("cs-mp");
    private static final String elytronToolPath = System.getProperty("elytrontool-path");
    private static final String decryptKeyAlias = System.getProperty("decryptkey-alias");


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
        Pattern pattern = Pattern.compile("'(.*?)'");

        //proc = Runtime.getRuntime().exec("java -jar /path/to/jboss-eap-7.4/jboss-modules.jar -mp /path/to/jboss-eap-7.4/modules/ org.wildfly.security.elytron-tool credential-store --location " + csPath + " --encrypt " + decryptKeyAlias + " -p " + maskedPassword + " --clear-text " + myNewCredential);
        proc = Runtime.getRuntime().exec(elytronToolPath + " credential-store --location " + csPath + " --encrypt " + decryptKeyAlias + " -p " + maskedPassword + " --clear-text " + myNewCredential);

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

        return newEncryptedCredential;

    }


    /**
     * Utility method which filters the CN on the given InitialDirContext
     * from the context "dc=superheroes,dc=com"
     * @param dirContext The InitialDirContext to use
     * @param cnValue The CN value to be searched
     * @return
     * @throws NamingException
     */
    public static String searchByCNOnLDAP(InitialDirContext dirContext, String cnValue) throws NamingException {
        StringBuilder sb = new StringBuilder();
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        Hashtable<String, String> environment = (Hashtable<String, String>) dirContext.getEnvironment();
        Enumeration<String> keys = environment.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            System.out.println("Key : " + key
                    + "\t\t Value : "
                    + environment.get(key));

        }

        NamingEnumeration<SearchResult> answer = dirContext.search("dc=superheroes,dc=com", "cn=" + cnValue, sc);
        sb.append("Results:").append(System.lineSeparator());

        while (answer.hasMore()) {
            SearchResult sr = answer.next();
            sb.append("Value: CN: ").append(sr.getAttributes().get("cn")).append(" SN: ").append(sr.getAttributes().get("sn")).append(System.lineSeparator());
            System.out.println("Value: CN: " + sr.getAttributes().get("cn") + " SN: " + sr.getAttributes().get("sn"));

        }

        return sb.toString();

    }


    /**
     * Utility method which retrieves, via regexp, the encrypted credential
     * from the whole line stored in the external properties file
     * @param currentLine The line containing the whole expression such as ${END::encresolver:xxencrypted-credentialxx}
     * @return The encrypted credential
     */
    public static String retrieveEncryptedCredentialFromPropertiesLine(String currentLine) {
        Pattern pattern = Pattern.compile("::" + resolverName + ":(.*)[}]");
        String encryptedCredential = "";

        Matcher matcher = pattern.matcher(currentLine);
        if (matcher.find())
        {
            encryptedCredential = matcher.group(1);
        }

        System.out.println("Encrypted Credential: " + encryptedCredential);

        return encryptedCredential;

    }



    /**
     * Utility method which decrypts a Masked Password, useful to access credential stores
     * @param masked Masked password in form of MASK-xxxxx
     * @return The decrypted Password object
     * @throws Exception
     */
    public static Password decryptMasked(String masked) throws Exception {

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

        return ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, encryptUtil.decodeAndDecrypt(encoded));

    }


    public static CredentialStore provideCredentialStore() throws Exception {
        Password storePassword = decryptMasked(maskedPassword);
        CredentialStore.ProtectionParameter protectionParameter = new CredentialStore.CredentialSourceProtectionParameter(IdentityCredentials.NONE.withCredential(new PasswordCredential(storePassword)));
        CredentialStore credentialStore = CredentialStore.getInstance("KeyStoreCredentialStore", CREDENTIAL_STORE_PROVIDER);

        Map<String, String> configuration = new HashMap<>();
        configuration.put("location", csPath);
        configuration.put("keyStoreType", "JCEKS");
        configuration.put("modifiable", "true");

        credentialStore.initialize(configuration, protectionParameter);

        return credentialStore;

    }

}

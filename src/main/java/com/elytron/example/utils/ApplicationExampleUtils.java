package com.elytron.example.utils;

import org.jboss.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Enumeration;
import java.util.Hashtable;

public class ApplicationExampleUtils {

    private static final Logger LOGGER = Logger.getLogger("cs-example");

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
            LOGGER.trace("Key : " + key
                    + "\t\t Value : "
                    + environment.get(key));

        }

        NamingEnumeration<SearchResult> answer = dirContext.search("dc=superheroes,dc=com", "cn=" + cnValue, sc);
        sb.append("Results:").append(System.lineSeparator());

        while (answer.hasMore()) {
            SearchResult sr = answer.next();
            sb.append("Value: CN: ").append(sr.getAttributes().get("cn")).append(" SN: ").append(sr.getAttributes().get("sn")).append(System.lineSeparator());
            LOGGER.info("Value: CN: " + sr.getAttributes().get("cn") + " SN: " + sr.getAttributes().get("sn"));

        }

        return sb.toString();

    }
}

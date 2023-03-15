package com.elytron.example;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;

import com.elytron.credentialstore.utils.CredentialsUtilitiesImpl;
import com.elytron.credentialstore.exception.CredentialStoreApiException;

@ApplicationPath("/credentialstore")
@Path("/api")
public class Api extends Application {


    @GET
    @Path("/lookupCustomContextByCN/{cnValue}")
    @Produces("application/text")
    public Response invokeCustom(@PathParam("cnValue") String cnValue) {

        InitialDirContext initialDirContext;
        StringBuilder sb = new StringBuilder();

        try {
            String ldappassword = CredentialsUtilitiesImpl.retrieveCredentials("ldappwd");

            Hashtable<String, String> environment = new Hashtable<String, String>();

            environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            environment.put(Context.PROVIDER_URL, "ldap://localhost:1389");
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, "cn=admin,dc=superheroes,dc=com");
            environment.put(Context.SECURITY_CREDENTIALS, ldappassword);

            initialDirContext = new InitialDirContext(environment);
            Enumeration<String> keys = environment.keys();

            while (keys.hasMoreElements()) {

                String key = keys.nextElement();

                System.out.println("Key : " + key
                        + "\t\t Value : "
                        + environment.get(key));
            }

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration answer = initialDirContext.search("dc=superheroes,dc=com", "cn=" + cnValue, sc);

            System.out.println("Results:");
            while (answer.hasMore()) {
                SearchResult sr = (SearchResult) answer.next();
                sb.append("Value: CN: " + sr.getAttributes().get("cn") + " SN: " + sr.getAttributes().get("sn"));
                sb.append(System.lineSeparator());
                System.out.println("Value: CN: " + sr.getAttributes().get("cn") + " SN: " + sr.getAttributes().get("sn"));
            }
        } catch (CredentialStoreApiException csae) {
            System.err.println(csae.getMessage());
            csae.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(csae.getMessage())
                    .build();

        } catch (NamingException e) {
            System.err.println("Cannot create Dir Context");
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: Cannot create Dir Context")
                    .build();
        }

        return Response.status(Response.Status.OK)
                .entity(sb.toString())
                .build();

    }

    @GET
    @Path("/retievePasswordOnly/{mySysProp}")
    @Produces("application/text")
    public Response retrieveCredentials(@PathParam("mySysProp") String mySysProp) {

        String alias = "";
        String password = "";

        try {
            alias = System.getProperty(mySysProp);
            password = CredentialsUtilitiesImpl.retrieveCredentials(alias);
            System.out.println("Alias: " + alias + " ********** Password: " + password);

        } catch (CredentialStoreApiException csae) {
            System.err.println(csae.getMessage());
            csae.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(csae.getMessage())
                    .build();
        }

        return Response.status(Response.Status.OK)
                .entity("Alias: " + alias + " ********** Password: " + password)
                .build();

    }

}

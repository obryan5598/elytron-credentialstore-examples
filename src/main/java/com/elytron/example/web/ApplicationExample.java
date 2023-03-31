package com.elytron.example.web;

import com.elytron.example.utils.ApplicationExampleUtils;
import org.jboss.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

@ApplicationPath("/credentialstore")
@Path("/example")
public class ApplicationExample extends Application {

    private static final Logger LOGGER = Logger.getLogger("cs-example");

    /**
     * REST API which retrieves JNDI LDAP Context from underlying EAP configuration
     * which references an encrypted credential on external properties file
     *
     * @param cnValue the CN to look for in the LDAP directory tree
     * @return The response containing the found CN and SN
     */
    @GET
    @Path("/lookupInConfiguredContextByCN/{cnValue}")
    @Produces("application/text")
    public Response lookupInConfiguredContextByCN(@PathParam("cnValue") String cnValue) {
        StringBuilder sb = new StringBuilder();
        String output;
        sb.append("**********************************************").append(System.lineSeparator());

        try {
            InitialContext initialContext = new InitialContext();
            InitialDirContext initialDirContext = (InitialDirContext) initialContext.lookup("java:global/ldap");

            output = ApplicationExampleUtils.searchByCNOnLDAP(initialDirContext, cnValue);
            sb.append(output);
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.info(sb);

        } catch (NamingException e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append("Cannot create Dir Context").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.error(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();
        }

        return Response.status(Response.Status.OK)
                .entity(sb.toString())
                .build();

    }

    /**
     * REST API which programmatically creates a JNDI LDAP Context
     * and retrieves an encrypted credential from a system property configured on EAP,
     * which in turn references an alias in an external properties file
     *
     * @param cnValue the CN to look for in the LDAP directory tree
     * @return The response containing the found CN and SN
     */
    @GET
    @Path("/lookupInCustomContextByCN/{cnValue}")
    @Produces("application/text")
    public Response lookupInCustomContextByCN(@PathParam("cnValue") String cnValue) {
        InitialDirContext initialDirContext;
        StringBuilder sb = new StringBuilder();
        String output;
        sb.append("**********************************************").append(System.lineSeparator());

        try {
            String ldappassword = System.getProperty("ldappwd-system-property");

            Hashtable<String, String> environment = new Hashtable<String, String>();

            environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            environment.put(Context.PROVIDER_URL, "ldap://localhost:1389");
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, "cn=admin,dc=superheroes,dc=com");
            environment.put(Context.SECURITY_CREDENTIALS, ldappassword);

            initialDirContext = new InitialDirContext(environment);

            output = ApplicationExampleUtils.searchByCNOnLDAP(initialDirContext, cnValue);
            sb.append(output);
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.info(sb);

        } catch (NamingException e) {
            sb.append("ERROR:").append(System.lineSeparator());
            sb.append("Cannot create Dir Context").append(System.lineSeparator());
            sb.append(e.getMessage()).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.error(sb);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(sb.toString())
                    .build();
        }

        return Response.status(Response.Status.OK)
                .entity(sb.toString())
                .build();

    }

    /**
     * REST API able to resolve the encrypted credential from external properties file, using
     * a bridge system property inside EAP configuration
     *
     * @param mySystemProperty The "bridge" system property inside EAP configuration
     * @return
     */
    @GET
    @Path("/resolveExternalCredentialFromSystemProperty/{mySystemProperty}")
    @Produces("application/text")
    public Response resolveExternalCredentialFromSystemProperty(@PathParam("mySystemProperty") String mySystemProperty) {
        String password;
        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************").append(System.lineSeparator());

        try {
            password = System.getProperty(mySystemProperty);
            sb.append("System Property: " + mySystemProperty).append(System.lineSeparator());
            sb.append("Password: " + password).append(System.lineSeparator());
            sb.append("**********************************************").append(System.lineSeparator());

            LOGGER.info(sb);

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

        return Response.status(Response.Status.OK)
                .entity(sb.toString())
                .build();

    }

}
# CREDENTIAL STORE API EXAMPLES

The current project aims to show how access and use a credential store which uses an external properties containing Elytron encrypted expressions.
The example has been compiled using OpenJDK 17.0.5 and tested on EAP 7.4.7 G.A which runs on the same JDK.

## COMPILATION AND PROVISIONING

To compile the current project please run:

```
mvn clean package
```

## OpenLDAP CONTAINER PROVISIONING

To test a LDAP invocation it is required to run a docker container with a LDAP server.
To run the corresponding container please update the *OpenLDAP/run.sh* file adding the absolute path on your local machine, then execute:

```
cd OpenLDAP
sh run.sh
```


## EAP CONFIGURATION

Before launching EAP via *jboss-eap-rhel.sh* (assuming a previous configuration of the *jboss-eap-instance* unit), it is necessary to update the file *jboss-eap.conf* adding the line:
```
JBOSS_OPTS="-P /path/to/your/EAP/instance/configuration/aliases.properties"
```
The given path references the external properties file which contains Elytron encrypted expressions (i.e. aliases and related credentials).
It is now possible to run EAP as root: 
```
systemctl start jboss-eap-instance.service 
```


To configure the EAP instance it is necessary to create the masked password which will be used by the credential store:
```
sh /path/to/your/EAP/installation/bin/elytron-tool.sh mask -s 20192018 -i 983  -d -x 123456

# OUTPUT
MASK-2ldsPZW4MPr;20192018;983
```

And then apply the following EAP CLI configurations:
```
# SETTING UP CONSOLE HANDLER WHETHER MISSING
/subsystem=logging/console-handler=CONSOLE:add(enabled=true, level=INFO)
/subsystem=logging/root-logger=ROOT:add-handler(name=CONSOLE)

# SETTING UP CREDENTIAL STORE
/subsystem=elytron/credential-store=decryptkeycs:add(create=true, modifiable=true, relative-to="jboss.server.config.dir", location="decryptkeycs.cs", implementation-properties={"keyStoreType"=>"JCEKS"}, credential-reference={clear-text="MASK-2ldsPZW4MPr;20192018;983"})
/subsystem=elytron/credential-store=decryptkeycs:generate-secret-key(alias=mydecryptkey)
/subsystem=elytron/expression=encryption:add(resolvers=[{credential-store=decryptkeycs,name=encresolver, secret-key=mydecryptkey}])

# SETTING UP SYSTEM PROPERTIES USEFUL FOR DEMO
/system-property=aliases-file:add(value="aliases.properties")
/system-property=configuration-path:add(value="/path/to/your/EAP/instance/configuration")
/system-property=cs-mp:add(value="MASK-2ldsPZW4MPr;20192018;983")
/system-property=cs-path:add(value="/path/to/your/EAP/instance/configuration/decryptkeycs.cs")
/system-property=elytrontool-path:add(value="/path/to/your/EAP/installation/bin/elytron-tool.sh")
/system-property=decryptkey-alias:add(value="mydecryptkey")
/system-property=encresolver-name:add(value=encresolver)
shutdown --restart
```

Once completed, it is possible to deploy the artifact:
```
cp /path/to/your/repo/target/ldap-invocation.war /path/to/your/EAP/instance/deployments/
```

## API EXECUTION

It is possible to invoke exposed REST APIs to manage credentials in external properties file

### INSERT NEW CREDENTIAL IN EXTERNAL PROPERTIES FILE
To insert the new credential *password* mapped by the *myAlias* key, please run httpie in the following way:
```
http :8080/ldap-invocation/credentialstore/api/insertPassword/myAlias/password
```

### UPDATE EXISTING CREDENTIAL IN EXTERNAL PROPERTIES FILE
To update an existing credential mapped by the *myAlias* key, please run httpie in the following way:
```
http :8080/ldap-invocation/credentialstore/api/updatePassword/myAlias/newPassword
```

### TODO
### DELETE EXISTING CREDENTIAL FROM EXTERNAL PROPERTIES FILE
To delete an existing credential mapped by the *myAlias* key, please run httpie in the following way:
```
http :8080/ldap-invocation/credentialstore/api/deletePassword/myAlias
```


### DECRYPT CREDENTIAL FROM EXTERNAL PROPERTIES FILE
To decrypt an existing credential mapped by the *myAlias* key, please run httpie in the following way:
```
http :8080/ldap-invocation/credentialstore/api/decryptExternalCredentialFromAlias/myAlias
```


## EXAMPLES EXECUTION (USE CASES)

It is possible to run some examples invoking the exposed REST APIs


### RETRIEVE CREDENTIAL FROM MAPPED SYSTEM PROPERTY

To retrieve a specific credential mapped by the *my-system-property* system property (which in turn references an alias from an external file), please run the followings:
```
# INSERT A NEW CREDENTIAL WHICH WILL BE REFERENCED BY AN EAP SYSTEM PROPERTY
http :8080/ldap-invocation/credentialstore/api/insertPassword/mysysprop.password/myPassword

# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 234
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:26:36 GMT

**********************************************
Inserted alias: mysysprop.password
New Credential: myPassword
New Ciphered Credential: RUxZAUMQUz2JU0HDTqryUt114P3EL/mP+rN0MyuyuQzqp22MFrw=
**********************************************


# RUN THE FOLLOWING EAP CLI
shutdown --restart
/system-property=my-system-property:add(value=${mysysprop.password})

# RETRIEVE THE PASSWORD FROM A SYSTEM PROPERTY REFERENCING AN ALIAS IN EXTERNAL PROPERTIES FILE
http :8080/ldap-invocation/credentialstore/example/resolveExternalCredentialFromSystemProperty/my-system-property


# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 151
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:27:17 GMT

**********************************************
System Property: my-system-property
Password: myPassword
**********************************************

# UPDATE THE CREDENTIAL WHICH WILL BE REFERENCED BY AN EAP SYSTEM PROPERTY
http :8080/ldap-invocation/credentialstore/api/updatePassword/mysysprop.password/myNewPassword


# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 236
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:34:39 GMT

**********************************************
Updated alias: mysysprop.password
New Credential: myNewPassword
New Ciphered Credential: RUxZAUMQtlyblhcKI/YtVYQtUEw67vy+m+GP5QBgJHr2DjEmbAY=
**********************************************

# RUN THE FOLLOWING EAP CLI
shutdown --restart

# RETRIEVE THE NEW PASSWORD AFTER THE UPDATE
http :8080/ldap-invocation/credentialstore/example/resolveExternalCredentialFromSystemProperty/my-system-property


# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 154
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:35:26 GMT

**********************************************
System Property: my-system-property
Password: myNewPassword
**********************************************
```


### RUN LDAP QUERY USING A CONFIGURED EXTERNAL CONTEXT ON EAP, WHICH REFERENCES AN ALIAS IN EXTERNAL PROPERTIES FILE
To run a query in the LDAP context configured on EAP please run:
```
# INSERT A NEW CREDENTIAL WHICH WILL BE REFERENCED IN THE EAP NAMING SUBSYSTEM
http :8080/ldap-invocation/credentialstore/api/insertPassword/configuredldap.password/admin

# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 234
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:29:22 GMT

**********************************************
Inserted alias: configuredldap.password
New Credential: admin
New Ciphered Credential: RUxZAUMQAyTUIWTG2m0IRszmF0YwMgAl3UVu5CYlFnrKq2Jw6/Q=
**********************************************


# RUN THE FOLLOWING EAP CLI
shutdown --restart
/subsystem=naming/binding=java\:global\/ldap:add(binding-type=external-context, cache=false, class="javax.naming.directory.InitialDirContext", module="org.jboss.as.naming", environment={"java.naming.factory.initial" => "com.sun.jndi.ldap.LdapCtxFactory", "java.naming.provider.url" => "ldap://localhost:1389", "java.naming.security.authentication" => "simple", "java.naming.security.principal" => "cn=admin,dc=superheroes,dc=com", "java.naming.security.credentials" => expression "${configuredldap.password}"})

# INVOKE A QUERY ON LDAP BY GIVEN CN STEVE
http :8080/ldap-invocation/credentialstore/example/lookupInConfiguredContextByCN/Steve


# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 139
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:30:04 GMT

**********************************************
Results:
Value: CN: cn: Steve SN: sn: Rogers
**********************************************
```

### RUN LDAP QUERY USING A INITIALDIRCONTEXT CONFIGURED IN APPLICATION, WHICH REFERENCES THE CREDENTIAL IN A SYSTEM PROPERTY
To run a query in a LDAP context configured programmatically in the application (which uses a password from a system property referencing an alias from an external file) please run via httpie:
```
# INSERT A NEW CREDENTIAL WHICH WILL BE REFERENCED IN THE DEDICATED SYSTEM PROPERTY
http :8080/ldap-invocation/credentialstore/api/insertPassword/programmaticldap.password/admin


# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 236
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:31:03 GMT

**********************************************
Inserted alias: programmaticldap.password
New Credential: admin
New Ciphered Credential: RUxZAUMQMa50zXVIdjhohIVdHPRFton8ddusEmx7PYBjEkatGSg=
**********************************************

# RUN THE FOLLOWING EAP CLI
shutdown --restart
/system-property=ldappwd-system-property:add(value=${programmaticldap.password})

# INVOKE A QUERY ON LDAP BY GIVEN CN STEVE
http :8080/ldap-invocation/credentialstore/example/lookupInCustomContextByCN/Steve


# RESPONSE
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 139
Content-Type: application/text
Date: Fri, 24 Mar 2023 17:31:42 GMT

**********************************************
Results:
Value: CN: cn: Steve SN: sn: Rogers
**********************************************
```


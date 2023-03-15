docker run --volume /absolute/path/to/your/repo/OpenLDAP/initdb:/container/service/slapd/assets/config/bootstrap/ldif/custom -e LDAP_ORGANISATION=superheroes -e LDAP_DOMAIN=superheroes.com -e LDAP_ADMIN_PASSWORD=admin -p 1389:389 -p 1636:636  --detach --name openLDAP_server --rm osixia/openldap:latest --copy-service

# FOR APACHEDS CONNECTION:
# user or BindDN: cn=admin,dc=superheroes,dc=com
# password: admin
# BaseDN: dc=superheroes,dc=com


# FOR LDAPSEARCH
# ldapsearch -x -b "dc=superheroes,dc=com" -H ldap://127.0.0.1:1389 -D "cn=admin,dc=superheroes,dc=com" -W "cn=Steve"

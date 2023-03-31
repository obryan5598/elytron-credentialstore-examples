package com.elytron.api.utils;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

public class CSPopulator {

    private static final Logger LOGGER = Logger.getLogger("cs-api");
    private static final CredentialStoreInfo credentialStoreInfo = CredentialStoreInfo.getInstance();

    public static CredentialStoreInfo retrieveInformationFromConfiguration() {

        LOGGER.trace("Retrieving information from current EAP configuration...");
        try {
            ModelNode productNode = new ModelNode();

            productNode.get(ClientConstants.OP).set("product-info");
            productNode.get(ClientConstants.NAME).set("product-info-resource");

            populateVariables(productNode);

            ModelNode expressionNode = new ModelNode();

            expressionNode.get(ClientConstants.OP).set(ClientConstants.READ_RESOURCE_OPERATION);
            expressionNode.get(ClientConstants.NAME).set("expression-resource");
            expressionNode.get(ClientConstants.OP_ADDR).add("subsystem", "elytron");
            expressionNode.get(ClientConstants.OP_ADDR).add("expression", "encryption");
            expressionNode.get(ClientConstants.RECURSIVE).set(true);

            populateVariables(expressionNode);

            ModelNode csNode = new ModelNode();

            csNode.get(ClientConstants.OP).set(ClientConstants.READ_RESOURCE_OPERATION);
            csNode.get(ClientConstants.NAME).set("cs-resource");
            csNode.get(ClientConstants.OP_ADDR).add("subsystem", "elytron");
            csNode.get(ClientConstants.OP_ADDR).add("credential-store", credentialStoreInfo.getCsName());
            csNode.get(ClientConstants.RECURSIVE).set(true);

            populateVariables(csNode);

            ModelNode pathNode = new ModelNode();

            pathNode.get(ClientConstants.OP).set(ClientConstants.READ_RESOURCE_OPERATION);
            pathNode.get(ClientConstants.NAME).set("aliases-path-resource");
            pathNode.get(ClientConstants.OP_ADDR).add("system-property", "aliases-file-path");
            pathNode.get(ClientConstants.RECURSIVE).set(true);

            populateVariables(pathNode);


        } catch (Exception e) {
            LOGGER.error("No Native Management Interface Found.");
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        } finally {
            LOGGER.trace("Information retrieved");
            return credentialStoreInfo;
        }
    }

    private static void populateVariables(ModelNode operationNode) throws IOException {

        LOGGER.trace("PopulateVariables input ModelNode: " + operationNode);
        ModelNode resolved;
        Property property;

        // TODO IMPROVE NATIVE INTERFACE LOOKUP
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("127.0.0.1"), 9999 + Integer.valueOf(System.getProperty("jboss.socket.binding.port-offset")));

        ModelNode response = client.execute(new OperationBuilder(operationNode).build());
        LOGGER.trace("PopulateVariables response: " + response);

        ModelNode outcome = response.get(ClientConstants.OUTCOME);
        LOGGER.trace("PopulateVariables outcome: " + outcome);

        if (ClientConstants.SUCCESS.equals(outcome.asString())) {
            List<ModelNode> result = response.get(ClientConstants.RESULT).asList();
            for (ModelNode modelNode : result) {
                resolved = modelNode.resolve();
                property = resolved.asProperty();
                switch (property.getName()) {
                    case "summary":
                        StringBuilder sb = new StringBuilder();
                        sb.append(property.getValue().get("product-home").asString()).append("/bin/elytron-tool.sh");
                        credentialStoreInfo.setElytronToolPath(sb.toString());
                        LOGGER.trace("elytronToolPath: " + credentialStoreInfo.getElytronToolPath());
                        break;
                    case "prefix":
                        credentialStoreInfo.setPrefix(property.getValue().asString());
                        LOGGER.trace("prefix: " + credentialStoreInfo.getPrefix());
                        break;
                    case "resolvers":
                        List<ModelNode> resolvers = property.getValue().asList();
                        Optional<ModelNode> optionalFoundResolver = resolvers.stream()
                                .filter(resolver -> resolver.get("name").asString().equals("encresolver"))
                                .findAny();

                        if (optionalFoundResolver.isPresent()) {
                            ModelNode foundResolver = optionalFoundResolver.get();
                            LOGGER.trace("resolver: " + foundResolver.get().asString());
                            credentialStoreInfo.setResolverName(foundResolver.get("name").asString());
                            LOGGER.trace("resolverName: " + credentialStoreInfo.getResolverName());
                            credentialStoreInfo.setDecryptKeyAlias(foundResolver.get("secret-key").asString());
                            LOGGER.trace("decryptKeyAlias: " + credentialStoreInfo.getDecryptKeyAlias());
                            credentialStoreInfo.setCsName(foundResolver.get("credential-store").asString());
                            LOGGER.trace("csName: " + credentialStoreInfo.getCsName());
                        } else {
                            LOGGER.error("No expression resolver found!");
                        }
                        break;
                    case "credential-reference":
                        ModelNode clearText = property.getValue();
                        credentialStoreInfo.setMaskedPassword(clearText.resolve().asProperty().getValue().asString());
                        LOGGER.trace("maskedPassword: " + credentialStoreInfo.getMaskedPassword());
                        break;
                    case "implementation-properties":
                        ModelNode keyStoreType = property.getValue();
                        credentialStoreInfo.setCsKeyStoreType(keyStoreType.resolve().asProperty().getValue().asString());
                        LOGGER.trace("csKeyStoreType: " + credentialStoreInfo.getCsKeyStoreType());
                        break;
                    case "path":
                        credentialStoreInfo.setCsFile(property.getValue().asString());
                        LOGGER.trace("csFile: " + credentialStoreInfo.getCsFile());
                        credentialStoreInfo.setCsPath(System.getProperty("jboss.server.config.dir") + "/" + credentialStoreInfo.getCsFile());
                        LOGGER.trace("csPath: " + credentialStoreInfo.getCsPath());
                        break;
                    case "value":
                        credentialStoreInfo.setAliasesFile(property.getValue().asString());
                        LOGGER.trace("aliasesFilePath: " + credentialStoreInfo.getAliasesFile());
                        break;
                }

            }

        } else {
            LOGGER.error("No Configuration Found.");
        }
    }

}

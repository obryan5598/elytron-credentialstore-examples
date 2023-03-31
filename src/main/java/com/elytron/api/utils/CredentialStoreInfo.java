package com.elytron.api.utils;

public class CredentialStoreInfo {


    private String resolverName = "";
    private String csFile = "";
    private String csName = "";
    private String csPath = "";
    private String aliasesFile = "";
    private String maskedPassword = "";
    private String prefix = "";
    private String csKeyStoreType = "";
    private String elytronToolPath = "";
    private String decryptKeyAlias = "";

    private static CredentialStoreInfo instance;

    public static synchronized CredentialStoreInfo getInstance() {
        if (instance == null) {
            instance = new CredentialStoreInfo();
        }
        return instance;
    }

    private CredentialStoreInfo() {
    }

    private CredentialStoreInfo(String resolverName, String csFile, String csName, String csPath, String aliasesFile, String maskedPassword, String prefix, String csKeyStoreType, String elytronToolPath, String decryptKeyAlias) {
        this.resolverName = resolverName;
        this.csFile = csFile;
        this.csName = csName;
        this.csPath = csPath;
        this.aliasesFile = aliasesFile;
        this.maskedPassword = maskedPassword;
        this.prefix = prefix;
        this.csKeyStoreType = csKeyStoreType;
        this.elytronToolPath = elytronToolPath;
        this.decryptKeyAlias = decryptKeyAlias;
    }

    public String getResolverName() {
        return resolverName;
    }

    public String getCsFile() {
        return csFile;
    }

    public String getCsName() {
        return csName;
    }

    public String getCsPath() {
        return csPath;
    }

    public String getAliasesFile() {
        return aliasesFile;
    }

    public String getMaskedPassword() {
        return maskedPassword;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCsKeyStoreType() {
        return csKeyStoreType;
    }

    public String getElytronToolPath() {
        return elytronToolPath;
    }

    public String getDecryptKeyAlias() {
        return decryptKeyAlias;
    }

    public void setResolverName(String resolverName) {
        this.resolverName = resolverName;
    }

    public void setCsFile(String csFile) {
        this.csFile = csFile;
    }

    public void setCsName(String csName) {
        this.csName = csName;
    }

    public void setCsPath(String csPath) {
        this.csPath = csPath;
    }

    public void setAliasesFile(String aliasesFile) {
        this.aliasesFile = aliasesFile;
    }

    public void setMaskedPassword(String maskedPassword) {
        this.maskedPassword = maskedPassword;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setCsKeyStoreType(String csKeyStoreType) {
        this.csKeyStoreType = csKeyStoreType;
    }

    public void setElytronToolPath(String elytronToolPath) {
        this.elytronToolPath = elytronToolPath;
    }

    public void setDecryptKeyAlias(String decryptKeyAlias) {
        this.decryptKeyAlias = decryptKeyAlias;
    }
}

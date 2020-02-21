package com.example.testhomomorphicencryptionapp;

import java.io.Serializable;
import java.util.UUID;

public class SEALConfig implements Serializable {

    private String serialVersionUID;
    private String params;
    private String privateKey;
    private String publicKey;

    public SEALConfig() {}

    public SEALConfig(String params, String privateKey, String publicKey) {
        this.params = params;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public boolean isEmpty(){
        if (this.params == null && this.privateKey == null && this.publicKey == null) {
            return true;
        }
        return false;
    }

    public String getSerialVersionUID() {
        return this.serialVersionUID;
    }

    public String getParams() {
        return params;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setSerialVersionUID(String serialVersionUID) {
        this.serialVersionUID = serialVersionUID;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}

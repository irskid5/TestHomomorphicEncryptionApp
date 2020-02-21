package com.example.testhomomorphicencryptionapp;

import java.util.ArrayList;

public class PathLocationData {
    String device_id;
    String key;
    ArrayList<LatLngEnc> path;

    public PathLocationData() {
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ArrayList<LatLngEnc> getPath() {
        return path;
    }

    public void setPath(ArrayList<LatLngEnc> path) {
        this.path = path;
    }
}

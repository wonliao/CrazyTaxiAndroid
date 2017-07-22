package com.firebase.sfvehicles.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by User on 2017/7/21.
 */

public class CarPos implements Serializable {

    private String key;
    private String g;
    private List<Float> l;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public List<Float> getL() {
        return l;
    }

    public void setL(List<Float> l) {
        this.l = l;
    }

    @Override
    public boolean equals(Object object) {
        if (this.key != null && object != null && object instanceof CarPos) {
            return  this.key.equals(((CarPos) object).getKey());
        } else {
            return false;
        }
    }
}

package com.firebase.sfvehicles.model;

import java.io.Serializable;

/**
 * Created by User on 2017/7/21.
 */

public class CarInfo implements Serializable {

    private String key;
    private float car_dir;
    private Integer car_no;
    private String car_type;
    private String driver_name;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public float getCar_dir() {
        return car_dir;
    }

    public void setCar_dir(float car_dir) {
        this.car_dir = car_dir;
    }

    public Integer getCar_no() {
        return car_no;
    }

    public void setCar_no(Integer car_no) {
        this.car_no = car_no;
    }

    public String getCar_type() {
        return car_type;
    }

    public void setCar_type(String car_type) {
        this.car_type = car_type;
    }

    public String getDriver_name() {
        return driver_name;
    }

    public void setDriver_name(String driver_name) {
        this.driver_name = driver_name;
    }

    @Override
    public boolean equals(Object object) {

        if (this.key != null && object != null) {
            if (object instanceof CarInfo) {
                this.key.equals(((CarInfo) object).getKey());
            } else if (object instanceof CarPos) {
                this.key.equals(((CarPos) object).getKey());
            } else if (object instanceof String) {
                this.key.equals((String) object);
            }
        }
        return false;
    }
}

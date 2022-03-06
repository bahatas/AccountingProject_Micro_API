package com.cydeo.serviceproduct.enums;

import lombok.Getter;

@Getter
public enum ProductStatus {
    ACTIVE("Active"),PASSIVE("Passive");

    private String value;
    ProductStatus(String productStatus){

        this.value = productStatus;


    }


    public String getValue(){return value;}

}

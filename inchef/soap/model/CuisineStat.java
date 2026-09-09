package com.inchef.soap.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CuisineStat", namespace = "http://inchef.com/soap/reports", propOrder = {
        "cuisine",
        "recipeCount"
})
public class CuisineStat {

    @XmlElement(namespace = "http://inchef.com/soap/reports", required = true)
    private String cuisine;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long recipeCount;

    public CuisineStat() {}

    public CuisineStat(String cuisine, long recipeCount) {
        this.cuisine = cuisine;
        this.recipeCount = recipeCount;
    }

    public String getCuisine() {
        return this.cuisine;
    }
    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public long getRecipeCount() {
        return this.recipeCount;
    }
    public void setRecipeCount(long recipeCount) {
        this.recipeCount = recipeCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cuisine;
        private long recipeCount;

        public Builder cuisine(String cuisine) {
            this.cuisine = cuisine;
            return this;
        }

        public Builder recipeCount(long recipeCount) {
            this.recipeCount = recipeCount;
            return this;
        }

        public CuisineStat build() {
            CuisineStat obj = new CuisineStat();
            obj.setCuisine(this.cuisine);
            obj.setRecipeCount(this.recipeCount);
            return obj;
        }
    }
}

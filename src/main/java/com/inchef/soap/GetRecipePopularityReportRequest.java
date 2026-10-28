package com.inchef.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "recipeId"
})
@XmlRootElement(name = "GetRecipePopularityReportRequest", namespace = "http://inchef.com/soap/reports")
public class GetRecipePopularityReportRequest {

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long recipeId;

    public GetRecipePopularityReportRequest() {}

    public GetRecipePopularityReportRequest(long recipeId) {
        this.recipeId = recipeId;
    }

    public long getRecipeId() {
        return this.recipeId;
    }
    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long recipeId;

        public Builder recipeId(long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public GetRecipePopularityReportRequest build() {
            GetRecipePopularityReportRequest obj = new GetRecipePopularityReportRequest();
            obj.setRecipeId(this.recipeId);
            return obj;
        }
    }
}

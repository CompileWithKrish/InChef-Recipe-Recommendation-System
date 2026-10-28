package com.inchef.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "filterStartDate"
})
@XmlRootElement(name = "GetSystemReportRequest", namespace = "http://inchef.com/soap/reports")
public class GetSystemReportRequest {

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private String filterStartDate;

    public GetSystemReportRequest() {}

    public GetSystemReportRequest(String filterStartDate) {
        this.filterStartDate = filterStartDate;
    }

    public String getFilterStartDate() {
        return this.filterStartDate;
    }
    public void setFilterStartDate(String filterStartDate) {
        this.filterStartDate = filterStartDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String filterStartDate;

        public Builder filterStartDate(String filterStartDate) {
            this.filterStartDate = filterStartDate;
            return this;
        }

        public GetSystemReportRequest build() {
            GetSystemReportRequest obj = new GetSystemReportRequest();
            obj.setFilterStartDate(this.filterStartDate);
            return obj;
        }
    }
}

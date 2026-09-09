package com.inchef.soap;

import com.inchef.soap.model.*;
import com.inchef.soap.service.RecipeReportService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class RecipeReportEndpoint {

    private static final String NAMESPACE_URI = "http://inchef.com/soap/reports";

    private final RecipeReportService recipeReportService;

    public RecipeReportEndpoint(RecipeReportService recipeReportService) {
        this.recipeReportService = recipeReportService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetSystemReportRequest")
    @ResponsePayload
    public GetSystemReportResponse getSystemReport(@RequestPayload GetSystemReportRequest request) {
        return recipeReportService.generateSystemReport(request);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetRecipePopularityReportRequest")
    @ResponsePayload
    public GetRecipePopularityReportResponse getRecipePopularityReport(@RequestPayload GetRecipePopularityReportRequest request) {
        return recipeReportService.generateRecipePopularityReport(request);
    }
}

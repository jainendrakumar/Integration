# CrewAvailabilityApi

All URIs are relative to *https://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**crewvalidation**](CrewAvailabilityApi.md#crewvalidation) | **POST** /cris/dev/in/api/crewavailability | Crew Availability Validation


<a name="crewvalidation"></a>
# **crewvalidation**
> CrewAvailabilityResponse crewvalidation(body)

Crew Availability Validation

Crew Availability Validation for data from Crew Management System from Designated crew lobbies

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CrewAvailabilityApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://localhost:8080");

    CrewAvailabilityApi apiInstance = new CrewAvailabilityApi(defaultClient);
    CrewAvailability body = new CrewAvailability(); // CrewAvailability | Crew Availability data for Validation
    try {
      CrewAvailabilityResponse result = apiInstance.crewvalidation(body);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CrewAvailabilityApi#crewvalidation");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | **CrewAvailability**| Crew Availability data for Validation |

### Return type

[**CrewAvailabilityResponse**](CrewAvailabilityResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful operation |  -  |
**400** | Bad request |  -  |


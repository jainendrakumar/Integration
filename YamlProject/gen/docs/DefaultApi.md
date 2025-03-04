# DefaultApi

All URIs are relative to *https://tobeprovidedlater*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createorupdate**](DefaultApi.md#createorupdate) | **POST** /mammoet/dev/api/volumeactuals | 


<a name="createorupdate"></a>
# **createorupdate**
> Response createorupdate(volumeActuals)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://tobeprovidedlater");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    VolumeActuals volumeActuals = new VolumeActuals(); // VolumeActuals | 
    try {
      Response result = apiInstance.createorupdate(volumeActuals);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#createorupdate");
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
 **volumeActuals** | [**VolumeActuals**](VolumeActuals.md)|  |

### Return type

[**Response**](Response.md)

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


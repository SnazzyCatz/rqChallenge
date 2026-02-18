package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeApiException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeApiClient {

    private final RestTemplate restTemplate;

    @Value("${mock.api.base-url}")
    private String baseUrl;

    @Value("${mock.api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${mock.api.retry.initial-delay-ms:2000}")
    private long initialRetryDelayMs;

    @Value("${mock.api.retry.multiplier:2}")
    private int retryMultiplier;

    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees from mock API");
        return executeWithRetry(() -> {
            ResponseEntity<ApiResponse<List<Employee>>> response = restTemplate.exchange(
                    baseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {});

            ApiResponse<List<Employee>> apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null) {
                log.info(
                        "Successfully fetched {} employees",
                        apiResponse.getData().size());
                return apiResponse.getData();
            }
            throw new EmployeeApiException("No data returned from API");
        });
    }

    public Employee getEmployeeById(String id) {
        log.info("Fetching employee by id: {}", id);
        return executeWithRetry(() -> {
            try {
                ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                        baseUrl + "/" + id,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ApiResponse<Employee>>() {});

                ApiResponse<Employee> apiResponse = response.getBody();
                if (apiResponse != null && apiResponse.getData() != null) {
                    log.info(
                            "Successfully fetched employee: {}",
                            apiResponse.getData().getName());
                    return apiResponse.getData();
                }
                throw new EmployeeNotFoundException("Employee with id " + id + " not found");
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("Employee not found with id: {}", id);
                throw new EmployeeNotFoundException("Employee with id " + id + " not found");
            }
        });
    }

    public Employee createEmployee(EmployeeInput input) {
        log.info("Creating employee: {}", input.getName());
        return executeWithRetry(() -> {
            HttpEntity<EmployeeInput> request = new HttpEntity<>(input);
            ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, request, new ParameterizedTypeReference<ApiResponse<Employee>>() {});

            ApiResponse<Employee> apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null) {
                log.info(
                        "Successfully created employee: {}",
                        apiResponse.getData().getName());
                return apiResponse.getData();
            }
            throw new EmployeeApiException("Failed to create employee");
        });
    }

    public Boolean deleteEmployeeByName(String name) {
        log.info("Deleting employee by name: {}", name);
        return executeWithRetry(() -> {
            // Mock API expects DELETE with body containing name
            DeleteRequest deleteRequest = new DeleteRequest(name);
            HttpEntity<DeleteRequest> request = new HttpEntity<>(deleteRequest);

            ResponseEntity<ApiResponse<Boolean>> response = restTemplate.exchange(
                    baseUrl, HttpMethod.DELETE, request, new ParameterizedTypeReference<ApiResponse<Boolean>>() {});

            ApiResponse<Boolean> apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null) {
                log.info("Successfully deleted employee: {}", name);
                return apiResponse.getData();
            }
            throw new EmployeeApiException("Failed to delete employee");
        });
    }

    private <T> T executeWithRetry(ApiCall<T> apiCall) {
        int attempt = 0;
        long delayMs = initialRetryDelayMs;

        while (true) {
            try {
                return apiCall.execute();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    attempt++;
                    if (attempt >= maxRetryAttempts) {
                        log.error("Max retry attempts ({}) reached for rate limit", maxRetryAttempts);
                        throw new RateLimitException("Rate limit exceeded after " + maxRetryAttempts + " attempts");
                    }
                    log.warn("Rate limit hit, attempt {}/{}, retrying in {}ms", attempt, maxRetryAttempts, delayMs);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EmployeeApiException("Retry interrupted", ie);
                    }
                    delayMs *= retryMultiplier;
                } else {
                    throw new EmployeeApiException("API call failed: " + e.getMessage(), e);
                }
            } catch (EmployeeNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new EmployeeApiException("Unexpected error calling API: " + e.getMessage(), e);
            }
        }
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }

    private record DeleteRequest(String name) {}
}

package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.exception.EmployeeApiException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
        properties = {
            "mock.api.base-url=http://localhost:8112/api/v1/employee",
            "mock.api.retry.max-attempts=3",
            "mock.api.retry.initial-delay-ms=10",
            "mock.api.retry.multiplier=2"
        })
class EmployeeApiClientTest {

    @Autowired
    private EmployeeApiClient employeeApiClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer mockServer;

    private String baseUrl = "http://localhost:8112/api/v1/employee";

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testGetAllEmployees_Success() throws Exception {
        List<Employee> employees = Arrays.asList(Employee.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .salary(100000)
                .age(30)
                .title("Developer")
                .email("john.doe@company.com")
                .build());

        ApiResponse<List<Employee>> response = new ApiResponse<>();
        response.setData(employees);
        response.setStatus("Successfully processed request.");

        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        List<Employee> result = employeeApiClient.getAllEmployees();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        mockServer.verify();
    }

    @Test
    void testGetEmployeeById_Success() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder()
                .id(employeeId)
                .name("John Doe")
                .salary(100000)
                .age(30)
                .title("Developer")
                .email("john.doe@company.com")
                .build();

        ApiResponse<Employee> response = new ApiResponse<>();
        response.setData(employee);
        response.setStatus("Successfully processed request.");

        mockServer
                .expect(requestTo(baseUrl + "/" + employeeId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        Employee result = employeeApiClient.getEmployeeById(employeeId.toString());

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        mockServer.verify();
    }

    @Test
    void testGetEmployeeById_NotFound() {
        UUID employeeId = UUID.randomUUID();

        mockServer
                .expect(requestTo(baseUrl + "/" + employeeId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(EmployeeNotFoundException.class, () -> employeeApiClient.getEmployeeById(employeeId.toString()));

        mockServer.verify();
    }

    @Test
    void testCreateEmployee_Success() throws Exception {
        EmployeeInput input = EmployeeInput.builder()
                .name("New Employee")
                .salary(80000)
                .age(25)
                .title("Junior Developer")
                .build();

        Employee createdEmployee = Employee.builder()
                .id(UUID.randomUUID())
                .name("New Employee")
                .salary(80000)
                .age(25)
                .title("Junior Developer")
                .email("new.employee@company.com")
                .build();

        ApiResponse<Employee> response = new ApiResponse<>();
        response.setData(createdEmployee);
        response.setStatus("Successfully processed request.");

        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        Employee result = employeeApiClient.createEmployee(input);

        assertNotNull(result);
        assertEquals("New Employee", result.getName());
        mockServer.verify();
    }

    @Test
    void testDeleteEmployeeByName_Success() throws Exception {
        String employeeName = "John Doe";

        ApiResponse<Boolean> response = new ApiResponse<>();
        response.setData(true);
        response.setStatus("Successfully processed request.");

        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        Boolean result = employeeApiClient.deleteEmployeeByName(employeeName);

        assertTrue(result);
        mockServer.verify();
    }

    @Test
    void testRateLimitRetry_SuccessAfterRetry() throws Exception {
        List<Employee> employees = Arrays.asList(Employee.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .salary(100000)
                .age(30)
                .title("Developer")
                .email("john.doe@company.com")
                .build());

        ApiResponse<List<Employee>> response = new ApiResponse<>();
        response.setData(employees);
        response.setStatus("Successfully processed request.");

        // First call returns 429
        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // Second call succeeds
        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        List<Employee> result = employeeApiClient.getAllEmployees();

        assertEquals(1, result.size());
        mockServer.verify();
    }

    @Test
    void testRateLimitRetry_MaxAttemptsExceeded() {
        // All attempts return 429
        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(RateLimitException.class, () -> employeeApiClient.getAllEmployees());

        mockServer.verify();
    }

    @Test
    void testApiError_InternalServerError() {
        mockServer
                .expect(requestTo(baseUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(EmployeeApiException.class, () -> employeeApiClient.getAllEmployees());

        mockServer.verify();
    }
}

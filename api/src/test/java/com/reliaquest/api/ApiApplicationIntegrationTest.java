package com.reliaquest.api;

import static org.junit.jupiter.api.Assertions.*;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Integration tests require mock server to be running on port 8112")
class ApiApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void contextLoads() {
        // Verify application context loads successfully
        assertNotNull(restTemplate);
    }

    @Test
    void testGetAllEmployees_ReturnsListOfEmployees() {
        ResponseEntity<List> response = restTemplate.getForEntity(getBaseUrl() + "/", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetHighestSalary_ReturnsInteger() {
        ResponseEntity<Integer> response = restTemplate.getForEntity(getBaseUrl() + "/highestSalary", Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() >= 0);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames_ReturnsList() {
        ResponseEntity<List> response =
                restTemplate.getForEntity(getBaseUrl() + "/topTenHighestEarningEmployeeNames", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() <= 10);
    }

    @Test
    void testSearchEmployees_ReturnsFilteredList() {
        ResponseEntity<List> response = restTemplate.getForEntity(getBaseUrl() + "/search/test", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testCreateAndDeleteEmployee_FullWorkflow() {
        // Create employee
        EmployeeInput input = EmployeeInput.builder()
                .name("Test Employee")
                .salary(75000)
                .age(28)
                .title("Test Engineer")
                .build();

        ResponseEntity<Employee> createResponse = restTemplate.postForEntity(getBaseUrl() + "/", input, Employee.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Employee createdEmployee = createResponse.getBody();
        assertNotNull(createdEmployee.getId());
        assertEquals("Test Employee", createdEmployee.getName());

        // Delete employee
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                getBaseUrl() + "/" + createdEmployee.getId(), HttpMethod.DELETE, null, String.class);

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertEquals("Test Employee", deleteResponse.getBody());
    }

    @Test
    void testGetEmployeeById_NotFound() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        ResponseEntity<String> response = restTemplate.getForEntity(getBaseUrl() + "/" + nonExistentId, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

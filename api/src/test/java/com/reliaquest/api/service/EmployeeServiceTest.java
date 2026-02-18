package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeApiClient employeeApiClient;

    @InjectMocks
    private EmployeeService employeeService;

    private List<Employee> mockEmployees;

    @BeforeEach
    void setUp() {
        mockEmployees = Arrays.asList(
                Employee.builder()
                        .id(UUID.randomUUID())
                        .name("John Doe")
                        .salary(100000)
                        .age(30)
                        .title("Developer")
                        .email("john.doe@company.com")
                        .build(),
                Employee.builder()
                        .id(UUID.randomUUID())
                        .name("Jane Smith")
                        .salary(150000)
                        .age(35)
                        .title("Senior Developer")
                        .email("jane.smith@company.com")
                        .build(),
                Employee.builder()
                        .id(UUID.randomUUID())
                        .name("Bob Johnson")
                        .salary(120000)
                        .age(28)
                        .title("Developer")
                        .email("bob.johnson@company.com")
                        .build());
    }

    @Test
    void testGetAllEmployees() {
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        List<Employee> result = employeeService.getAllEmployees();

        assertEquals(3, result.size());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    void testSearchEmployeesByName_Found() {
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        List<Employee> result = employeeService.searchEmployeesByName("john");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getName().equals("John Doe")));
        assertTrue(result.stream().anyMatch(e -> e.getName().equals("Bob Johnson")));
    }

    @Test
    void testSearchEmployeesByName_NotFound() {
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        List<Employee> result = employeeService.searchEmployeesByName("xyz");

        assertEquals(0, result.size());
    }

    @Test
    void testSearchEmployeesByName_CaseInsensitive() {
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        List<Employee> result = employeeService.searchEmployeesByName("JANE");

        assertEquals(1, result.size());
        assertEquals("Jane Smith", result.get(0).getName());
    }

    @Test
    void testGetEmployeeById() {
        UUID employeeId = UUID.randomUUID();
        Employee mockEmployee = mockEmployees.get(0);
        when(employeeApiClient.getEmployeeById(employeeId.toString())).thenReturn(mockEmployee);

        Employee result = employeeService.getEmployeeById(employeeId.toString());

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        verify(employeeApiClient, times(1)).getEmployeeById(employeeId.toString());
    }

    @Test
    void testGetHighestSalary() {
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        Integer result = employeeService.getHighestSalary();

        assertEquals(150000, result);
    }

    @Test
    void testGetHighestSalary_EmptyList() {
        when(employeeApiClient.getAllEmployees()).thenReturn(Arrays.asList());

        Integer result = employeeService.getHighestSalary();

        assertEquals(0, result);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames() {
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        assertEquals(3, result.size());
        assertEquals("Jane Smith", result.get(0));
        assertEquals("Bob Johnson", result.get(1));
        assertEquals("John Doe", result.get(2));
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames_MoreThanTen() {
        List<Employee> manyEmployees = Arrays.asList(
                createEmployee("Emp1", 100000),
                createEmployee("Emp2", 90000),
                createEmployee("Emp3", 110000),
                createEmployee("Emp4", 95000),
                createEmployee("Emp5", 105000),
                createEmployee("Emp6", 115000),
                createEmployee("Emp7", 85000),
                createEmployee("Emp8", 120000),
                createEmployee("Emp9", 80000),
                createEmployee("Emp10", 125000),
                createEmployee("Emp11", 130000),
                createEmployee("Emp12", 75000));

        when(employeeApiClient.getAllEmployees()).thenReturn(manyEmployees);

        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        assertEquals(10, result.size());
        assertEquals("Emp11", result.get(0));
        assertEquals("Emp10", result.get(1));
    }

    @Test
    void testCreateEmployee() {
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

        when(employeeApiClient.createEmployee(any(EmployeeInput.class))).thenReturn(createdEmployee);

        Employee result = employeeService.createEmployee(input);

        assertNotNull(result);
        assertEquals("New Employee", result.getName());
        assertEquals(80000, result.getSalary());
        verify(employeeApiClient, times(1)).createEmployee(any(EmployeeInput.class));
    }

    @Test
    void testDeleteEmployeeById() {
        UUID employeeId = UUID.randomUUID();
        Employee mockEmployee = mockEmployees.get(0);

        when(employeeApiClient.getEmployeeById(employeeId.toString())).thenReturn(mockEmployee);
        when(employeeApiClient.deleteEmployeeByName("John Doe")).thenReturn(true);

        String result = employeeService.deleteEmployeeById(employeeId.toString());

        assertEquals("John Doe", result);
        verify(employeeApiClient, times(1)).getEmployeeById(employeeId.toString());
        verify(employeeApiClient, times(1)).deleteEmployeeByName("John Doe");
    }

    @Test
    void testDeleteEmployeeById_FailedDelete() {
        UUID employeeId = UUID.randomUUID();
        Employee mockEmployee = mockEmployees.get(0);

        when(employeeApiClient.getEmployeeById(employeeId.toString())).thenReturn(mockEmployee);
        when(employeeApiClient.deleteEmployeeByName("John Doe")).thenReturn(false);

        Exception exception =
                assertThrows(RuntimeException.class, () -> employeeService.deleteEmployeeById(employeeId.toString()));

        assertTrue(exception.getMessage().contains("Failed to delete employee"));
    }

    private Employee createEmployee(String name, int salary) {
        return Employee.builder()
                .id(UUID.randomUUID())
                .name(name)
                .salary(salary)
                .age(30)
                .title("Developer")
                .email(name.toLowerCase() + "@company.com")
                .build();
    }
}

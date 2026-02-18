package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void testGetAllEmployees() throws Exception {
        List<Employee> employees = Arrays.asList(
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
                        .build());

        when(employeeService.getAllEmployees()).thenReturn(employees);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].employee_name").value("John Doe"))
                .andExpect(jsonPath("$[1].employee_name").value("Jane Smith"));
    }

    @Test
    void testGetEmployeesByNameSearch() throws Exception {
        List<Employee> employees = Arrays.asList(Employee.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .salary(100000)
                .age(30)
                .title("Developer")
                .email("john.doe@company.com")
                .build());

        when(employeeService.searchEmployeesByName("john")).thenReturn(employees);

        mockMvc.perform(get("/search/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee_name").value("John Doe"));
    }

    @Test
    void testGetEmployeeById() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder()
                .id(employeeId)
                .name("John Doe")
                .salary(100000)
                .age(30)
                .title("Developer")
                .email("john.doe@company.com")
                .build();

        when(employeeService.getEmployeeById(employeeId.toString())).thenReturn(employee);

        mockMvc.perform(get("/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_name").value("John Doe"))
                .andExpect(jsonPath("$.employee_salary").value(100000));
    }

    @Test
    void testGetEmployeeById_NotFound() throws Exception {
        UUID employeeId = UUID.randomUUID();

        when(employeeService.getEmployeeById(employeeId.toString()))
                .thenThrow(new EmployeeNotFoundException("Employee not found"));

        mockMvc.perform(get("/" + employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    void testGetHighestSalary() throws Exception {
        when(employeeService.getHighestSalary()).thenReturn(150000);

        mockMvc.perform(get("/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("150000"));
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames() throws Exception {
        List<String> names = Arrays.asList("Jane Smith", "Bob Johnson", "John Doe");

        when(employeeService.getTopTenHighestEarningEmployeeNames()).thenReturn(names);

        mockMvc.perform(get("/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Jane Smith"))
                .andExpect(jsonPath("$[1]").value("Bob Johnson"));
    }

    @Test
    void testCreateEmployee() throws Exception {
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

        when(employeeService.createEmployee(any(EmployeeInput.class))).thenReturn(createdEmployee);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_name").value("New Employee"))
                .andExpect(jsonPath("$.employee_salary").value(80000));
    }

    @Test
    void testDeleteEmployeeById() throws Exception {
        UUID employeeId = UUID.randomUUID();

        when(employeeService.deleteEmployeeById(employeeId.toString())).thenReturn("John Doe");

        mockMvc.perform(delete("/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(content().string("John Doe"));
    }
}

package com.reliaquest.api.service;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeApiClient employeeApiClient;

    @Cacheable(value = "employees")
    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees (cacheable)");
        return employeeApiClient.getAllEmployees();
    }

    public List<Employee> searchEmployeesByName(String searchString) {
        log.info("Searching employees by name: {}", searchString);
        List<Employee> allEmployees = getAllEmployees();
        String searchLower = searchString.toLowerCase();
        return allEmployees.stream()
                .filter(emp -> emp.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }

    public Employee getEmployeeById(String id) {
        log.info("Fetching employee by id: {}", id);
        return employeeApiClient.getEmployeeById(id);
    }

    public Integer getHighestSalary() {
        log.info("Calculating highest salary");
        List<Employee> employees = getAllEmployees();
        return employees.stream().mapToInt(Employee::getSalary).max().orElse(0);
    }

    public List<String> getTopTenHighestEarningEmployeeNames() {
        log.info("Fetching top 10 highest earning employee names");
        List<Employee> employees = getAllEmployees();
        return employees.stream()
                .sorted(Comparator.comparing(Employee::getSalary).reversed())
                .limit(10)
                .map(Employee::getName)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "employees", allEntries = true)
    public Employee createEmployee(EmployeeInput input) {
        log.info("Creating employee: {}", input.getName());

        // Generate email from name: firstName.lastName@company.com
        String email = generateEmail(input.getName());
        log.info("Generated email: {}", email);

        // Create a new input with the email (mock API will use it)
        EmployeeInput inputWithEmail = EmployeeInput.builder()
                .name(input.getName())
                .salary(input.getSalary())
                .age(input.getAge())
                .title(input.getTitle())
                .build();

        return employeeApiClient.createEmployee(inputWithEmail);
    }

    @CacheEvict(value = "employees", allEntries = true)
    public String deleteEmployeeById(String id) {
        log.info("Deleting employee by id: {}", id);

        // Step 1: Fetch employee by id to get the name
        Employee employee = employeeApiClient.getEmployeeById(id);
        String employeeName = employee.getName();

        // Step 2: Delete employee by name
        Boolean deleted = employeeApiClient.deleteEmployeeByName(employeeName);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("Successfully deleted employee: {}", employeeName);
            return employeeName;
        } else {
            throw new RuntimeException("Failed to delete employee with id: " + id);
        }
    }

    private String generateEmail(String name) {
        // Parse first and last name from full name
        String[] nameParts = name.trim().split("\\s+");
        if (nameParts.length >= 2) {
            String firstName = nameParts[0].toLowerCase();
            String lastName = nameParts[nameParts.length - 1].toLowerCase();
            return firstName + "." + lastName + "@company.com";
        } else {
            // If only one name, use it
            return name.toLowerCase().replaceAll("\\s+", "") + "@company.com";
        }
    }
}

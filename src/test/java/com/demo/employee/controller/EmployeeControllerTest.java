package com.demo.employee.controller;

import com.demo.employee.entity.Employee;
import com.demo.employee.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService service;

    @Test
    @DisplayName("Should return 201 Created when creating employee with valid positive salary")
    void createEmployee_withValidSalary_shouldReturnCreated() throws Exception {
        when(service.create(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salary\": 50000.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salary").value(50000.0));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when creating employee with zero salary")
    void createEmployee_withZeroSalary_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salary\": 0.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when creating employee with negative salary")
    void createEmployee_withNegativeSalary_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salary\": -500.0}"))
                .andExpect(status().isBadRequest());
    }
}
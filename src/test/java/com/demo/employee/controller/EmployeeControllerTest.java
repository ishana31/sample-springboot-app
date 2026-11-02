package com.demo.employee.controller;

import com.demo.employee.entity.Employee;
import com.demo.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    private String endpoint;

    @BeforeEach
    void setUp() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            try {
                objectMapper.findAndRegisterModules();
            } catch (Exception ignored) {
            }
        }

        endpoint = resolveEndpoint();

        try {
            Employee dummyEmployee = null;
            try {
                Constructor<Employee> ctor = Employee.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                dummyEmployee = ctor.newInstance();
            } catch (Exception e) {
                for (Constructor<?> c : Employee.class.getDeclaredConstructors()) {
                    try {
                        c.setAccessible(true);
                        Object[] initargs = new Object[c.getParameterCount()];
                        dummyEmployee = (Employee) c.newInstance(initargs);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }

            if (dummyEmployee == null) {
                try {
                    dummyEmployee = Mockito.mock(Employee.class);
                } catch (Exception ignored) {
                }
            }

            if (dummyEmployee != null) {
                try {
                    Method setId = Employee.class.getMethod("setId", Long.class);
                    setId.invoke(dummyEmployee, 1L);
                } catch (Exception e) {
                    try {
                        Method setId = Employee.class.getMethod("setId", Integer.class);
                        setId.invoke(dummyEmployee, 1);
                    } catch (Exception ignored) {
                    }
                }
            }

            final Employee fallbackEmployee = dummyEmployee;

            for (Method method : EmployeeService.class.getMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(Employee.class)
                        && !method.getReturnType().equals(Void.TYPE)) {
                    try {
                        Mockito.lenient().doAnswer(invocation -> {
                            Object arg = invocation.getArgument(0);
                            if (arg != null) {
                                try {
                                    Method setId = arg.getClass().getMethod("setId", Long.class);
                                    setId.invoke(arg, 1L);
                                } catch (Exception e1) {
                                    try {
                                        Method setId = arg.getClass().getMethod("setId", Integer.class);
                                        setId.invoke(arg, 1);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            Object result = arg != null ? arg : fallbackEmployee;
                            Class<?> returnType = method.getReturnType();
                            if (returnType.isAssignableFrom(Employee.class)) {
                                return result;
                            } else if (returnType.equals(Optional.class)) {
                                return Optional.ofNullable(result);
                            } else if (returnType.equals(Long.class) || returnType.equals(long.class)) {
                                return 1L;
                            } else if (returnType.equals(Integer.class) || returnType.equals(int.class)) {
                                return 1;
                            } else if (returnType.equals(String.class)) {
                                return "1";
                            }
                            return result;
                        }).when(employeeService);
                        method.invoke(employeeService, Mockito.any(Employee.class));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Should return HTTP 201 Created when creating an employee with valid positive salary")
    void createEmployee_withValidPositiveSalary_shouldReturnCreated() throws Exception {
        Map<String, Object> payload = createEmployeePayload(50000.0);

        mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return HTTP 400 Bad Request when creating an employee with zero salary")
    void createEmployee_withZeroSalary_shouldReturnBadRequest() throws Exception {
        Map<String, Object> payload = createEmployeePayload(0.0);

        mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return HTTP 400 Bad Request when creating an employee with negative salary")
    void createEmployee_withNegativeSalary_shouldReturnBadRequest() throws Exception {
        Map<String, Object> payload = createEmployeePayload(-1000.0);

        mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    private String resolveEndpoint() {
        String basePath = "";
        if (EmployeeController.class.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = EmployeeController.class.getAnnotation(RequestMapping.class);
            if (rm.value().length > 0 && !rm.value()[0].isEmpty()) {
                basePath = rm.value()[0];
            } else if (rm.path().length > 0 && !rm.path()[0].isEmpty()) {
                basePath = rm.path()[0];
            }
        }

        String postPath = null;
        for (Method method : EmployeeController.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping pm = method.getAnnotation(PostMapping.class);
                String subPath = "";
                if (pm.value().length > 0) {
                    subPath = pm.value()[0];
                } else if (pm.path().length > 0) {
                    subPath = pm.path()[0];
                }

                boolean takesEmployee = false;
                for (Class<?> pType : method.getParameterTypes()) {
                    if (pType.equals(Employee.class)) {
                        takesEmployee = true;
                        break;
                    }
                }

                if (takesEmployee || postPath == null) {
                    postPath = subPath;
                    if (takesEmployee) {
                        break;
                    }
                }
            } else if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping rm = method.getAnnotation(RequestMapping.class);
                boolean isPost = false;
                for (RequestMethod reqMethod : rm.method()) {
                    if (reqMethod == RequestMethod.POST) {
                        isPost = true;
                        break;
                    }
                }
                if (isPost) {
                    String subPath = "";
                    if (rm.value().length > 0) {
                        subPath = rm.value()[0];
                    } else if (rm.path().length > 0) {
                        subPath = rm.path()[0];
                    }
                    boolean takesEmployee = false;
                    for (Class<?> pType : method.getParameterTypes()) {
                        if (pType.equals(Employee.class)) {
                            takesEmployee = true;
                            break;
                        }
                    }
                    if (takesEmployee || postPath == null) {
                        postPath = subPath;
                        if (takesEmployee) {
                            break;
                        }
                    }
                }
            }
        }

        if (postPath != null && !postPath.isEmpty()) {
            if (!basePath.isEmpty()) {
                basePath = basePath + "/" + postPath;
            } else {
                basePath = postPath;
            }
        }

        if (basePath.isEmpty()) {
            for (Method method : EmployeeController.class.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    GetMapping gm = method.getAnnotation(GetMapping.class);
                    String p = gm.value().length > 0 ? gm.value()[0] : (gm.path().length > 0 ? gm.path()[0] : "");
                    if (p.startsWith("/api/employees") || p.startsWith("/employees")) {
                        basePath = p.startsWith("/api/employees") ? "/api/employees" : "/employees";
                        break;
                    }
                }
            }
        }

        if (basePath.isEmpty()) {
            basePath = "/api/employees";
        }
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        return basePath.replaceAll("/+", "/");
    }

    private Map<String, Object> createEmployeePayload(double salaryValue) {
        Map<String, Object> payload = new HashMap<>();

        Class<?> current = Employee.class;
        Class<?> salaryType = Double.class;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String name = field.getName();
                if ("salary".equalsIgnoreCase(name)) {
                    salaryType = field.getType();
                    continue;
                }
                if ("id".equalsIgnoreCase(name)) {
                    continue;
                }
                Class<?> type = field.getType();
                if (type.equals(String.class)) {
                    if (name.toLowerCase().contains("email")) {
                        payload.put(name, "john.doe@example.com");
                    } else if (name.toLowerCase().contains("first")) {
                        payload.put(name, "John");
                    } else if (name.toLowerCase().contains("last")) {
                        payload.put(name, "Doe");
                    } else if (name.toLowerCase().contains("phone")) {
                        payload.put(name, "1234567890");
                    } else {
                        payload.put(name, "Test" + name);
                    }
                } else if (type.equals(Long.class) || type.equals(long.class)) {
                    payload.put(name, 1L);
                } else if (type.equals(Integer.class) || type.equals(int.class)) {
                    payload.put(name, 1);
                } else if (type.equals(Double.class) || type.equals(double.class)) {
                    payload.put(name, 100.0);
                } else if (type.equals(BigDecimal.class)) {
                    payload.put(name, BigDecimal.valueOf(100.0));
                } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                    payload.put(name, true);
                } else if (type.equals(LocalDate.class)) {
                    payload.put(name, LocalDate.now().toString());
                } else if (type.equals(Date.class)) {
                    payload.put(name, "2023-01-01");
                } else if (type.isEnum()) {
                    Object[] constants = type.getEnumConstants();
                    if (constants != null && constants.length > 0) {
                        payload.put(name, constants[0].toString());
                    }
                }
            }
            current = current.getSuperclass();
        }

        if (payload.isEmpty()) {
            payload.put("name", "John Doe");
        }

        Object formattedSalary;
        if (salaryType.equals(BigDecimal.class)) {
            formattedSalary = BigDecimal.valueOf(salaryValue);
        } else if (salaryType.equals(Long.class) || salaryType.equals(long.class)) {
            formattedSalary = (long) salaryValue;
        } else if (salaryType.equals(Integer.class) || salaryType.equals(int.class)) {
            formattedSalary = (int) salaryValue;
        } else {
            formattedSalary = salaryValue;
        }

        payload.put("salary", formattedSalary);
        return payload;
    }
}
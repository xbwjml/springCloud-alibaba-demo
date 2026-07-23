package com.demo.inventory.controller;

import com.demo.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class InventoryControllerBindingTest {

    @Test
    void stockPathVariableIsBoundWithoutCompilerParameterMetadata() throws Exception {
        MockMvc mockMvc = standaloneSetup(
                new InventoryController(new InventoryServiceImpl()))
                .build();

        mockMvc.perform(get("/inventory/stock/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.stock").value(100));
    }
}

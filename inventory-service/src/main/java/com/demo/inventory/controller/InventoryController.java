package com.demo.inventory.controller;

import com.demo.common.result.Result;
import com.demo.inventory.service.impl.InventoryServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryServiceImpl inventoryService;

    public InventoryController(InventoryServiceImpl inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/deduct")
    public Result<Map<String, Object>> deduct(@RequestParam("productId") Long productId,
                                              @RequestParam("quantity") int quantity) {
        int remaining = inventoryService.deduct(productId, quantity);
        return Result.success(Map.of("productId", productId, "quantity", quantity, "remaining", remaining));
    }

    @GetMapping("/stock/{productId}")
    public Result<Map<String, Object>> getStock(@PathVariable("productId") Long productId) {
        int stock = inventoryService.getStock(productId);
        return Result.success(Map.of("productId", productId, "stock", stock));
    }
}

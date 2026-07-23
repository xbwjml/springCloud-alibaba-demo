package com.demo.order.service;

import com.demo.common.dto.ProductDTO;
import com.demo.common.exception.BusinessException;
import com.demo.common.service.InventoryService;
import com.demo.order.client.ProductClientFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductClientFacade productClientFacade;

    @Mock
    private InventoryService inventoryService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(productClientFacade);
        ReflectionTestUtils.setField(orderService, "inventoryService", inventoryService);
    }

    @Test
    void productFailureDoesNotDeductInventory() {
        when(productClientFacade.getById(1L))
                .thenThrow(new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                        50311, "商品服务暂时不可用"));

        assertThatThrownBy(() -> orderService.createOrderViaFeign(1L, 1))
                .isInstanceOf(BusinessException.class);
        verify(inventoryService, never()).deduct(1L, 1);
    }

    @Test
    void successfulProductQueryDeductsInventoryOnce() {
        ProductDTO product = ProductDTO.builder()
                .id(1L)
                .name("iPhone")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .build();
        when(productClientFacade.getById(1L)).thenReturn(product);
        when(inventoryService.deduct(1L, 2)).thenReturn(8);

        var order = orderService.createOrderViaFeign(1L, 2);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");
        verify(inventoryService).deduct(1L, 2);
    }
}

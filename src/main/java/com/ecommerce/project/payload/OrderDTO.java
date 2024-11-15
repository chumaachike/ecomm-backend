package com.ecommerce.project.payload;

import com.ecommerce.project.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Double totalPrice;
    private AddressDTO address; // Use a DTO for Address if needed
    private List<OrderItemDTO> orderItems;
}

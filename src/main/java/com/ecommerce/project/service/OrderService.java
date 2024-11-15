package com.ecommerce.project.service;

import com.ecommerce.project.payload.EntityResponse;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemRequestDTO;

import java.util.List;

public interface OrderService {
    OrderDTO placeOrder(Long addressId, List<OrderItemRequestDTO> items);

    EntityResponse<OrderDTO> getUserOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderDTO buySelectedFromCart(Long addressId, List<OrderItemRequestDTO> selectedItems);

    OrderDTO buyNow(Long addressId, OrderItemRequestDTO itemRequest);
}

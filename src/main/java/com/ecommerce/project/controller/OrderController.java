package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.payload.EntityResponse;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.payload.OrderItemRequestDTO;
import com.ecommerce.project.service.OrderService;
import com.ecommerce.project.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthUtil authUtil;

    @PostMapping("/order/cart/{addressId}")
    public ResponseEntity<OrderDTO>buySelectedFromCart(
            @PathVariable Long addressId,
            @RequestBody List <OrderItemRequestDTO> selectedItems){
        OrderDTO order = orderService.buySelectedFromCart(addressId, selectedItems);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
    @GetMapping("/orders/user")
    public ResponseEntity<EntityResponse<OrderDTO>>getUserOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY_ORDER_DATE, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ){
        EntityResponse<OrderDTO> orderDTOEntityResponse = orderService.getUserOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(orderDTOEntityResponse, HttpStatus.OK);
    }

    @PostMapping("/order/buy-now/{addressId}")
    public ResponseEntity<OrderDTO> buyNow(@PathVariable Long addressId, @RequestBody OrderItemRequestDTO itemRequest) {
        OrderDTO order = orderService.buyNow(addressId, itemRequest);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

}

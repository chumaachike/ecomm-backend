package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.*;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuthUtil authUtil;

    @Override
    @Transactional
    public OrderDTO placeOrder(Long addressId, List<OrderItemRequestDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new APIException("Order items cannot be empty");
        }

        Order order = new Order();
        order.setUser(authUtil.loggedInUser());
        order.setOrderStatus(OrderStatus.PENDING);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "AddressId", addressId));
        order.setAddress(address);

        double totalPrice = items.stream()
                .mapToDouble(itemDTO -> processOrderItem(itemDTO, order))
                .sum();

        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        return convertToOrderDTO(savedOrder);
    }

    @Override
    public EntityResponse<OrderDTO> getUserOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        String email = authUtil.loggedInEmail();

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Order> pageOrders = orderRepository.findAllByEmail(email, pageDetails);

        List<Order> orders = pageOrders.getContent();
        if (orders.isEmpty()) throw new APIException("No orders found");

        List<OrderDTO> orderDTOS = orders.stream()
                .map(this::convertToOrderDTO)
                .toList();

        EntityResponse<OrderDTO> orderResponse = new EntityResponse<>();
        orderResponse.setContent(orderDTOS);
        orderResponse.setPageNumber(pageOrders.getNumber());
        orderResponse.setPageSize(pageOrders.getSize());
        orderResponse.setTotalElements(pageOrders.getTotalElements());
        orderResponse.setTotalPages(pageOrders.getTotalPages());
        orderResponse.setLastPage(pageOrders.isLast());

        return orderResponse;
    }

    @Override
    @Transactional
    public OrderDTO purchase(OrderRequestDTO orderRequest) {
        Address address = resolveAddress(orderRequest);

        String email = authUtil.loggedInEmail();
        Optional<Cart> cartOptional = cartRepository.findCartByEmail(email);

        List<CartItem> cartItemsToRemove = new ArrayList<>();
        List<OrderItemRequestDTO> orderItemRequests = orderRequest.getOrderItems().stream().map(orderItem -> {
            CartItem cartItem = cartOptional.map(cart ->
                    cart.getCartItems().stream()
                            .filter(ci -> ci.getProduct().getProductId().equals(orderItem.getProductId()))
                            .findFirst()
                            .orElse(null)
            ).orElse(null);

            if (cartItem != null) {
                if (cartItem.getQuantity() < orderItem.getQuantity()) {
                    throw new APIException("Insufficient quantity in cart for product: "
                            + cartItem.getProduct().getProductName() + ". Available: "
                            + cartItem.getQuantity() + ", Requested: " + orderItem.getQuantity());
                }
                cartItemsToRemove.add(cartItem);
            }

            return new OrderItemRequestDTO(orderItem.getProductId(), orderItem.getQuantity());
        }).toList();

        OrderDTO order = placeOrder(address.getAddressId(), orderItemRequests);

        cartOptional.ifPresent(cart -> {
            cartItemsToRemove.forEach(cart::removeCartItem);
            cartItemRepository.deleteAll(cartItemsToRemove);
        });

        return order;
    }

    private Address resolveAddress(OrderRequestDTO orderRequest) {
        if (orderRequest.getAddress().getAddressId() != null) {
            return addressRepository.findById(orderRequest.getAddress().getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "AddressId", orderRequest.getAddress().getAddressId()));
        }
        Address newAddress = modelMapper.map(orderRequest.getAddress(), Address.class);
        return addressRepository.save(newAddress);
    }

    private double processOrderItem(OrderItemRequestDTO itemDTO, Order order) {
        Product product = productRepository.findById(itemDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ProductId", itemDTO.getProductId()));

        if (product.getQuantity() < itemDTO.getQuantity()) {
            throw new APIException("Insufficient stock for product: " + product.getProductName());
        }

        product.setQuantity(product.getQuantity() - itemDTO.getQuantity());
        productRepository.save(product);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(itemDTO.getQuantity());
        orderItem.setPrice(product.getSpecialPrice() * itemDTO.getQuantity());

        order.getOrderItems().add(orderItem);
        orderItemRepository.save(orderItem);
        return orderItem.getPrice();
    }

    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
        orderDTO.setOrderItems(orderItemDTOs);

        return orderDTO;
    }

    private OrderItemDTO convertToOrderItemDTO(OrderItem orderItem) {
        ProductDTO productDTO = modelMapper.map(orderItem.getProduct(), ProductDTO.class);

        OrderItemDTO orderItemDTO = new OrderItemDTO();
        orderItemDTO.setOrderItemId(orderItem.getOrderItemId());
        orderItemDTO.setProductDTO(productDTO);
        orderItemDTO.setQuantity(orderItem.getQuantity());
        orderItemDTO.setPrice(orderItem.getPrice());

        return orderItemDTO;
    }
}

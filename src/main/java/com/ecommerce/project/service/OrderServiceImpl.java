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

import java.util.List;
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

        return convertToOrderDTO(savedOrder);  // Use this method to ensure enriched DTO
    }

    @Override
    public EntityResponse<OrderDTO> getUserOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        String email = authUtil.loggedInEmail();

        // Create Sort object based on sortOrder
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // Create Pageable object
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        // Fetch paginated orders
        Page<Order> pageOrders = orderRepository.findAllByEmail(email, pageDetails);


        List<Order> orders = pageOrders.getContent();
        if (orders.isEmpty()) throw new APIException("No orders found");
        List<OrderDTO> orderDTOS = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class)).toList();

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
    public OrderDTO buySelectedFromCart(Long addressId, List<OrderItemRequestDTO> selectedItems) {
        String email = authUtil.loggedInEmail();

        // Retrieve user's cart
        Cart cart = cartRepository.findCartByEmail(email);
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty. Add items to cart before purchasing.");
        }

        // Filter cart items based on selected items
        List<CartItem> itemsToPurchase = selectedItems.stream()
                .map(selectedItem -> {
                    CartItem cartItem = cart.getCartItems().stream()
                            .filter(ci -> ci.getProduct().getProductId().equals(selectedItem.getProductId()))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product", "productId", selectedItem.getProductId()
                            ));

                    if (cartItem.getQuantity() < selectedItem.getQuantity()) {
                        throw new APIException("Insufficient quantity in cart for product: "
                                + cartItem.getProduct().getProductName() + ". Available: "
                                + cartItem.getQuantity() + ", Requested: " + selectedItem.getQuantity());
                    }

                    return cartItem;
                })
                .toList();

        // Map selected cart items to OrderItemRequestDTO
        List<OrderItemRequestDTO> orderItemRequests = itemsToPurchase.stream()
                .map(cartItem -> new OrderItemRequestDTO(cartItem.getProduct().getProductId(), cartItem.getQuantity()))
                .toList();

        // Place the order using the filtered items
        OrderDTO order = placeOrder(addressId, orderItemRequests);

        // Remove purchased items from the cart
        itemsToPurchase.forEach(cart::removeCartItem);  // Ensure removal from in-memory collection
        cartItemRepository.deleteAll(itemsToPurchase);  // Ensure database deletion

        return order;
    }

    @Override
    public OrderDTO buyNow(Long addressId, OrderItemRequestDTO itemRequest) {
        return placeOrder(addressId, List.of(itemRequest));
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
        orderItemDTO.setProductDTO(productDTO);  // Use ProductDTO for enriched response
        orderItemDTO.setQuantity(orderItem.getQuantity());
        orderItemDTO.setPrice(orderItem.getPrice());

        return orderItemDTO;
    }
}

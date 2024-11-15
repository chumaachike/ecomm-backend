package com.ecommerce.project.service;

import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;

import java.util.List;

public interface CartService {
    CartItemDTO addOrUpdateCartItem(Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart();


    String deleteProductFromCart(Long productId);

    CartItemDTO updateProductQuantityInCart(Long productId, Integer quantity);
}

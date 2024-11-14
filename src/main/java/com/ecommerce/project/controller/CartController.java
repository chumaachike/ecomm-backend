package com.ecommerce.project.controller;

import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartItemDTO> addProductToCart(@PathVariable Long productId,
                                                        @PathVariable Integer quantity){
        CartItemDTO cartItemDTO = cartService.addOrUpdateCartItem(productId, quantity);
        return new ResponseEntity<CartItemDTO>(cartItemDTO, HttpStatus.CREATED);
    }

    @GetMapping("/carts")
    public ResponseEntity<List<CartDTO>>getCarts(){
        List<CartDTO> cartDTOList = cartService.getAllCarts();
        return new ResponseEntity<List<CartDTO>>(cartDTOList, HttpStatus.OK);
    }

    @PutMapping("/cart/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartItemDTO>updateCartProduct(@PathVariable Long productId,@PathVariable Integer quantity){
        CartItemDTO cartItemDTO = cartService.updateProductQuantityInCart(productId, quantity);
        return new ResponseEntity<CartItemDTO>(cartItemDTO, HttpStatus.OK);
    }


    @GetMapping("/carts/users/cart")
    public ResponseEntity<CartDTO> getCartById(){
        CartDTO cartDTO = cartService.getCart();
        return new ResponseEntity<CartDTO>(cartDTO, HttpStatus.OK);
    }

    @DeleteMapping("/carts/product/{productId}")
    public ResponseEntity<String>deleteProductFromCart( @PathVariable Long productId){
        String message = cartService.deleteProductFromCart(productId);
        return new ResponseEntity<String>(message, HttpStatus.OK);
    }

}

package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private final ModelMapper modelMapper;

    @Autowired
    public CartServiceImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public CartItemDTO addOrUpdateCartItem(Long productId, Integer quantity) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() <= 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            return updateProductQuantityInCart(productId, quantity);
        }

        cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setDiscount(product.getDiscount());

        CartItem savedCartItem = cartItemRepository.save(cartItem);
        recalculateCartTotalPrice(cart);

        return modelMapper.map(savedCartItem, CartItemDTO.class);
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if (carts.isEmpty()) {
            throw new APIException("No cart exists");
        }

        return carts.stream()
                .map(cart -> {
                    CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
                    List<ProductDTO> productDTOS = cart.getCartItems().stream()
                            .map(cartItem -> modelMapper.map(cartItem.getProduct(), ProductDTO.class))
                            .collect(Collectors.toList());
                    cartDTO.setProducts(productDTOS);
                    return cartDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CartDTO getCart() {
        Cart cart = getOrCreateCart();
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> products = cart.getCartItems().stream()
                .map(cartItem -> {
                    ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                    productDTO.setQuantity(cartItem.getQuantity());
                    return productDTO;
                })
                .collect(Collectors.toList());

        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional
    public String deleteProductFromCart(Long productId) {
        Cart cart = getOrCreateCart();
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cartItemRepository.delete(cartItem);
        cart.getCartItems().remove(cartItem);
        recalculateCartTotalPrice(cart);

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart!";
    }

    @Override
    @Transactional
    public CartItemDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem == null) {
            throw new APIException(product.getProductName() + " not available in the cart");
        }

        if (quantity <= 0) {
            deleteProductFromCart(productId);
        } else if (quantity > product.getQuantity()) {
            throw new APIException("Requested quantity exceeds available stock for product: " + product.getProductName());
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        recalculateCartTotalPrice(cart);
        return modelMapper.map(cartItem, CartItemDTO.class);
    }

    private void recalculateCartTotalPrice(Cart cart) {
        double newTotalPrice = cart.getCartItems().stream()
                .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(newTotalPrice);
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart() {
        String emailId = authUtil.loggedInEmail();
        return cartRepository.findCartByEmail(emailId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setTotalPrice(0.00);
                    newCart.setUser(authUtil.loggedInUser());
                    return cartRepository.save(newCart);
                });
    }
}

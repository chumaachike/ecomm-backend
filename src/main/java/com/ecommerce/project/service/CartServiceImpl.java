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
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    private final ModelMapper modelMapper;

    @Autowired
    public CartServiceImpl(ModelMapper modelMapper) {  // Inject the configured ModelMapper
        this.modelMapper = modelMapper;
    }


    @Override
    public CartItemDTO addOrUpdateCartItem(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() <= 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        // If the item already exists, update quantity and return
        if (cartItem != null) {
            return updateProductQuantityInCart(productId, quantity);
        }

        // Else, create a new CartItem
        cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setDiscount(product.getDiscount());

        CartItem savedCartItem = cartItemRepository.save(cartItem);
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);

        return modelMapper.map(savedCartItem, CartItemDTO.class);
    }



    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if(carts.isEmpty()){
            throw new APIException("No cart exists");
        }
        return carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> productDTOS = cart.getCartItems().stream()
                    .map(product -> modelMapper.map(product.getProduct(), ProductDTO.class)).toList();
            cartDTO.setProducts(productDTOS);
            return cartDTO;
        }).toList();
    }

    @Override
    public CartDTO getCart() {
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) cart = createCart();

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cart.getCartItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(product -> modelMapper.map(product.getProduct(), ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional
    public String deleteProductFromCart(Long productId) {
        Cart cart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        // Delete from the database
        cartItemRepository.deleteCartItemByProductIdAndCartId(cart.getCartId(), productId);

        // Remove the cartItem from the cart's in-memory list
        cart.getCartItems().remove(cartItem);

        // Recalculate the total price
        double newTotalPrice = cart.getCartItems().stream()
                .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(newTotalPrice);

        Cart savedCart = cartRepository.save(cart);
        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";

    }



    private Cart createCart() {
        Cart userCart  = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());

        return cartRepository.save(cart);
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        double cartPrice = cart.getTotalPrice()
                - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice
                + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public CartItemDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);

        if (userCart == null) throw new ResourceNotFoundException("Cart", "email", emailId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(userCart.getCartId(), productId);
        if (cartItem == null) throw new APIException(product.getProductName() + " not available in the cart");

        double priceAdjustment = cartItem.getProductPrice() * cartItem.getQuantity();

        if (quantity <= 0) {
            deleteProductFromCart(productId);
        } else {
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());

            // Adjust the total price based on the new quantity
            userCart.setTotalPrice(userCart.getTotalPrice() - priceAdjustment + (product.getSpecialPrice() * quantity));
            cartItemRepository.save(cartItem);
        }

        cartRepository.save(userCart);
        return modelMapper.map(cartItem, CartItemDTO.class);
    }


}

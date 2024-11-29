package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.EntityResponse;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;


@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private  CategoryRepository categoryRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private  CartService cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PixelService pixelService;

    @Autowired
    private AuthUtil authUtil;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        User user = authUtil.loggedInUser();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

        boolean isProductNotPresent = true;

        List<Product> products = category.getProducts();
        for (Product product : products) {
            if (product.getProductName().equals(productDTO.getProductName())) {
                isProductNotPresent = false;
                break;
            }
        }

        if (isProductNotPresent) {
            Product product = modelMapper.map(productDTO, Product.class);
            String imageUrl = pixelService.fetchImage(product.getProductName());
            product.setImageUrl(Objects.requireNonNullElse(imageUrl, "xxx"));
            product.setCategory(category);
            product.setUser(user);
            double specialPrice = product.getPrice() -
                    ((product.getDiscount() * 0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct, ProductDTO.class);
        } else {
            throw new APIException("Product already exist!!");
        }
    }
    @Override
    public EntityResponse<ProductDTO> getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageDetails = buildPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Product> pageProducts = productRepository.findAll(pageDetails);
        return buildEntityResponse(pageProducts);
    }

    @Override
    public EntityResponse<ProductDTO> getUserProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageDetails = buildPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Product> pageProducts = productRepository.findAllByEmail(authUtil.loggedInEmail(), pageDetails);
        return buildEntityResponse(pageProducts);
    }

    @Override
    public EntityResponse<ProductDTO> searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "CategoryId", categoryId));

        Pageable pageDetails = buildPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Product> pageProducts = productRepository.findByCategoryOrderByPriceAsc(category, pageDetails);

        return buildEntityResponse(pageProducts);
    }

    @Override
    public EntityResponse<ProductDTO> searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageDetails = buildPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Product> pageProducts = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageDetails);

        return buildEntityResponse(pageProducts);
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {

        //Fetch the logged-in user
        User loggedInUser = authUtil.loggedInUser();
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        // Check ownership
        if (!existingProduct.getUser().getUserId().equals(loggedInUser.getUserId())) {
            throw new APIException("Unauthorized: You do not own this product.");
        }

        // Map updated fields from ProductDTO to the existing product
        if (productDTO.getProductName() != null)existingProduct.setProductName(productDTO.getProductName());
        if (productDTO.getDescription() != null)existingProduct.setDescription(productDTO.getDescription());
        if (productDTO.getPrice() != 0.0)existingProduct.setPrice(productDTO.getPrice());
        if (productDTO.getDiscount() != 0.0)existingProduct.setDiscount(productDTO.getDiscount());
        if (productDTO.getQuantity() != null) existingProduct.setQuantity(productDTO.getQuantity());

        // Recalculate special price
        double specialPrice = existingProduct.getPrice() -
                (existingProduct.getDiscount() * 0.01) * existingProduct.getPrice();
        existingProduct.setSpecialPrice(specialPrice);

        // Save the updated product
        Product updatedProduct = productRepository.save(existingProduct);

        // Update all carts that contain this product
        List<Cart> carts = cartRepository.findCartsByProductId(productId);

        carts.forEach(cart -> {
            cart.getCartItems().stream()
                    .filter(cartItem -> cartItem.getProduct().getProductId().equals(productId))
                    .forEach(cartItem -> {
                        cartItem.setProductPrice(updatedProduct.getSpecialPrice());
                        cartItem.setDiscount(updatedProduct.getDiscount());
                        cartItemRepository.save(cartItem); // Save updated cart item
                    });

            // Recalculate and save cart's total price
            recalculateCartTotalPrice(cart);
        });

        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    private void recalculateCartTotalPrice(Cart cart) {
        double newTotalPrice = cart.getCartItems().stream()
                .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(newTotalPrice);
        cartRepository.save(cart); // Save updated cart total price
    }


    @Override
    public ProductDTO deleteProduct(Long productId) {
        User user = authUtil.loggedInUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product", "ProductId", productId));

        if (!product.getUser().getUserId().equals(user.getUserId())) {
            throw new APIException("Unauthorized: You do not own this product.");
        }
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(productId));
        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }



    @Override
    public ProductDTO getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product", "ProductId", productId));
        return modelMapper.map(product, ProductDTO.class);
    }



    private EntityResponse<ProductDTO> buildEntityResponse(Page<Product> pageProducts) {
        List<ProductDTO> productDTOS = pageProducts.getContent().stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        if (productDTOS.isEmpty()) {
            throw new APIException("No products found");
        }

        EntityResponse<ProductDTO> productResponse = new EntityResponse<>();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());

        return productResponse;
    }

    private Pageable buildPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}

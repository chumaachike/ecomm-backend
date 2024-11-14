package com.ecommerce.project.service;

import com.ecommerce.project.payload.EntityResponse;
import com.ecommerce.project.payload.ProductDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProductService {
    ProductDTO addProduct(Long categoryId, ProductDTO productDTO);

    EntityResponse<ProductDTO> getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    EntityResponse<ProductDTO> searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    EntityResponse<ProductDTO> searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductDTO updateProduct(Long productId, ProductDTO product);

    ProductDTO deleteProduct(Long productId);

    ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException;

    ProductDTO getProduct(Long productId);
}

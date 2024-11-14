package com.ecommerce.project.service;

import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.EntityResponse;


public interface CategoryService {
    EntityResponse<CategoryDTO> getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder );
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    CategoryDTO deleteCategory(Long categoryId);
    CategoryDTO updateCategory(CategoryDTO categoryDTO , Long categoryDTOId);

}

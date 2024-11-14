package com.ecommerce.project.service;


import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.EntityResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.List;


@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PixelService pixelService;

    @Override
    public EntityResponse<CategoryDTO> getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAnOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAnOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categories =  categoryPage.getContent();
        if (categories.isEmpty()) throw new APIException("No Category created");
        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();
        EntityResponse<CategoryDTO> categoryResponse = new EntityResponse<CategoryDTO>();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());
        return  categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category tempCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (tempCategory != null) throw new APIException("Category: " + category.getCategoryName() + " already exists");

        String imageUrl = pixelService.fetchImage(category.getCategoryName());
        if (imageUrl != null){
            category.setCategoryUrl(imageUrl);
        }else{
            category.setCategoryUrl("xxx");
        }

        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category", "categorID", categoryId));
        categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryDTOId) {
        Category foundCategory = categoryRepository.findById(categoryDTOId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryDTOId));

        foundCategory.setCategoryName(categoryDTO.getCategoryName());

        Category updatedCategory = categoryRepository.save(foundCategory);

        return modelMapper.map(updatedCategory, CategoryDTO.class);
    }
}
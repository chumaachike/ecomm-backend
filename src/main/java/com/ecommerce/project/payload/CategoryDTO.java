package com.ecommerce.project.payload;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long categoryId;

    @Size(min = 3, message = "Category name must contain at least five characters")
    @NotBlank
    private String categoryName;

    private String categoryUrl;
}

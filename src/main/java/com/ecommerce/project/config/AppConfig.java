package com.ecommerce.project.config;

import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.payload.CartItemDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        TypeMap<CartItem, CartItemDTO> typeMap = modelMapper.createTypeMap(CartItem.class, CartItemDTO.class);
        typeMap.addMappings(mapper -> {
            mapper.map(CartItem::getCart, CartItemDTO::setCartDTO); // Explicit mapping
            mapper.map(CartItem::getProduct, CartItemDTO::setProductDTO); // Map nested Product to ProductDTO
        });
        return modelMapper;
    }
}

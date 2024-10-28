package com.ecommerce.project.service;

import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.EntityResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface AddressService {
    AddressDTO addAddress(@Valid AddressDTO addressDTO);

    EntityResponse<AddressDTO> getAddresses();

    AddressDTO getAddress(Long addressId);

    List<AddressDTO> getUserAddresses();

    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);

    String deleteAddress(Long addressId);
}

package com.ecommerce.project.controller;

import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.EntityResponse;
import com.ecommerce.project.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> addAddress(@Valid @RequestBody AddressDTO addressDTO){
        AddressDTO savedAddressDTO = addressService.addAddress(addressDTO);
        return new ResponseEntity<>(savedAddressDTO, HttpStatus.CREATED);
    }

    @GetMapping("/addresses")
    public ResponseEntity<EntityResponse<AddressDTO>> getAddresses(){
        EntityResponse<AddressDTO> addressDTOEntityResponse = addressService.getAddresses();
        return new ResponseEntity<>(addressDTOEntityResponse, HttpStatus.OK);
    }

    @GetMapping("addresses/{addressId}")
    public ResponseEntity<AddressDTO>getAddressById(@PathVariable Long addressId){
        AddressDTO foundAddress = addressService.getAddress(addressId);
        return new ResponseEntity<>(foundAddress, HttpStatus.OK);
    }
    @GetMapping("/users/addresses")
    public ResponseEntity<List<AddressDTO>>getUserAddresses(){
        List<AddressDTO> addressDTOS = addressService.getUserAddresses();
        return new ResponseEntity<>(addressDTOS, HttpStatus.OK);
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO>updateAddress(@PathVariable Long addressId, @RequestBody AddressDTO addressDTO){
        AddressDTO updatedAddress = addressService.updateAddress(addressId, addressDTO);
        return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
    }

    @DeleteMapping("addresses/{addressId}")
    public ResponseEntity<String>deleteAddress(@PathVariable Long addressId){
        String message = addressService.deleteAddress(addressId);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}

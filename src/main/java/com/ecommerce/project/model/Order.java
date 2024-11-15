package com.ecommerce.project.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JsonManagedReference
    private List<OrderItem> orderItems = new ArrayList<>();



    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate() {
        orderDate =  LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private Double totalPrice;


    // Reference to Address
    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;
}

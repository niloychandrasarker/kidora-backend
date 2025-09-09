package com.kidora.repository;

import com.kidora.entity.Address;
import com.kidora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    
    Optional<Address> findByUserAndIsDefaultTrue(User user);
    
    void deleteByUserAndId(User user, Long id);
}

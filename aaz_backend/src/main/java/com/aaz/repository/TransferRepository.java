package com.aaz.repository;

import com.aaz.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByTripGroupIdOrderByTransferDateDesc(Long tripGroupId);

    List<Transfer> findByTripGroupId(Long tripGroupId);
}
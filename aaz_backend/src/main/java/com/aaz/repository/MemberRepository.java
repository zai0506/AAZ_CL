package com.aaz.repository;

import com.aaz.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByTripGroupId(Long tripGroupId);

    Optional<Member> findByTripGroupIdAndUserId(Long tripGroupId, Long userId);

    Boolean existsByTripGroupIdAndUserId(Long tripGroupId, Long userId);

    List<Member> findByUserId(Long userId);
}
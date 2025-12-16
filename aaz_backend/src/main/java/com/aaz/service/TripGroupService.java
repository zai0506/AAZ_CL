package com.aaz.service;

import com.aaz.dto.*;
import com.aaz.entity.*;
import com.aaz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripGroupService {
    
    private final TripGroupRepository tripGroupRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public TripGroupResponse createTripGroup(TripGroupRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new RuntimeException("使用者不存在"));
        
        TripGroup tripGroup = new TripGroup();
        tripGroup.setName(request.getName());
        tripGroup.setTripCode(generateTripCode());
        tripGroup.setStartDate(request.getStartDate());
        tripGroup.setEndDate(request.getEndDate());
        tripGroup.setBaseCurrency(request.getBaseCurrency());
        tripGroup.setAnnouncement(request.getAnnouncement());
        tripGroup.setCreatorId(creatorId);
        
        TripGroup saved = tripGroupRepository.save(tripGroup);
        
        // 新增創建者為成員
        Member creatorMember = new Member();
        creatorMember.setTripGroup(saved);
        creatorMember.setUser(creator);
        creatorMember.setDisplayName(creator.getNickname());
        creatorMember.setIsCreator(true);
        memberRepository.save(creatorMember);
        
        // 新增其他成員
        if (request.getMembers() != null) {
            for (MemberRequest mr : request.getMembers()) {
                Member member = new Member();
                member.setTripGroup(saved);
                if (mr.getUserId() != null) {
                    User user = userRepository.findById(mr.getUserId()).orElse(null);
                    member.setUser(user);
                }
                member.setDisplayName(mr.getDisplayName());
                member.setIsCreator(false);
                memberRepository.save(member);
            }
        }
        
        return convertToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public List<TripGroupResponse> getUserTripGroups(Long userId) {
        return tripGroupRepository.findByUserId(userId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TripGroupResponse getTripGroupById(Long tripId) {
        TripGroup tripGroup = tripGroupRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("群組不存在"));
        return convertToResponse(tripGroup);
    }
    
    @Transactional
    public TripGroupResponse updateTripGroup(Long tripId, TripGroupRequest request) {
        TripGroup tripGroup = tripGroupRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("群組不存在"));

        // 更新群組基本資料
        tripGroup.setName(request.getName());
        tripGroup.setStartDate(request.getStartDate());
        tripGroup.setEndDate(request.getEndDate());
        tripGroup.setBaseCurrency(request.getBaseCurrency());
        tripGroup.setAnnouncement(request.getAnnouncement());

        TripGroup saved = tripGroupRepository.save(tripGroup);

        // 處理成員更新和新增
        if (request.getMembers() != null) {
            for (MemberRequest mr : request.getMembers()) {
                if (mr.getId() != null) {
                    // 更新現有成員的 displayName
                    Member member = memberRepository.findById(mr.getId())
                        .orElseThrow(() -> new RuntimeException("成員不存在"));
                    member.setDisplayName(mr.getDisplayName());
                    memberRepository.save(member);
                } else {
                    // 新增成員
                    Member member = new Member();
                    member.setTripGroup(saved);
                    member.setDisplayName(mr.getDisplayName());
                    member.setIsCreator(false);
                    if (mr.getUserId() != null) {
                        User user = userRepository.findById(mr.getUserId()).orElse(null);
                        member.setUser(user);
                    }
                    memberRepository.save(member);
                }
            }
        }

        return convertToResponse(saved);
    }
    
    private String generateTripCode() {
        return "AAZ" + String.format("%06d", new Random().nextInt(999999));
    }
    
    private TripGroupResponse convertToResponse(TripGroup tripGroup) {
        TripGroupResponse response = new TripGroupResponse();
        response.setId(tripGroup.getId());
        response.setTripCode(tripGroup.getTripCode());
        response.setName(tripGroup.getName());
        response.setStartDate(tripGroup.getStartDate());
        response.setEndDate(tripGroup.getEndDate());
        response.setBaseCurrency(tripGroup.getBaseCurrency());
        response.setAnnouncement(tripGroup.getAnnouncement());
        
        List<Member> members = memberRepository.findByTripGroupId(tripGroup.getId());
        response.setMembers(members.stream()
            .map(m -> {
                MemberResponse mr = new MemberResponse();
                mr.setId(m.getId());
                mr.setDisplayName(m.getDisplayName());
                mr.setIsCreator(m.getIsCreator());
                return mr;
            })
            .collect(Collectors.toList()));
        
        return response;
    }
}
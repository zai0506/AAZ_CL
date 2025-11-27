package com.aaz.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tripgroups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_code", unique = true, nullable = false, length = 10)
    private String tripCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "TWD";

    @Column(columnDefinition = "TEXT")
    private String announcement;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @OneToMany(mappedBy = "tripGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members = new ArrayList<>();
}
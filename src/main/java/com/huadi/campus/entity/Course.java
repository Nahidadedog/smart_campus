package com.huadi.campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String code;

    @Column
    private Double credits;

    @Column
    private Integer hours;

    @Column(length = 50)
    private String type;

    @Column(length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(length = 50)
    private String semester;

    @Column(length = 100)
    private String schedule;

    @Column(length = 100)
    private String classroom;

    @Column
    private Integer maxStudents;

    /** 已选人数。修改必须通过 CourseRepository 的原子更新语句，避免并发超选 */
    @Column
    private Integer currentStudents = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    void onCreate() {
        this.createTime = LocalDateTime.now();
    }
}

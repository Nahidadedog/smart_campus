package com.huadi.campus.entity;

import com.huadi.campus.enums.GradeLevel;
import com.huadi.campus.enums.GradeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "grade")
@Getter
@Setter
@NoArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradeLevel gradeLevel;

    @Column
    private Double credits;

    /** 状态由业务层根据分数计算，不在实体里给默认值 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradeStatus status;

    @Column(length = 50)
    private String semester;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    void onCreate() {
        this.createTime = LocalDateTime.now();
    }
}

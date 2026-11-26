package com.huadi.campus.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeLevelTests {

    @ParameterizedTest
    @CsvSource({
            "100, EXCELLENT",
            "90,  EXCELLENT",
            "89.9, GOOD",
            "80,  GOOD",
            "70,  MEDIUM",
            "60,  PASS",
            "59.9, FAIL",
            "0,   FAIL"
    })
    void mapsScoreToLevel(double score, GradeLevel expected) {
        assertEquals(expected, GradeLevel.fromScore(score));
    }

    @Test
    void labelIsChinese() {
        assertEquals("优", GradeLevel.EXCELLENT.getLabel());
        assertEquals("不及格", GradeLevel.FAIL.getLabel());
    }
}

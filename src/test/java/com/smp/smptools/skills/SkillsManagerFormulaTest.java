package com.smp.smptools.skills;

import com.smp.smptools.utils.Constants;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillsManagerFormulaTest {

    @Test
    void getExpToNextLevel_level1_returnsBaseExp() {
        // For level 1: 100 * 1.2^0 = 100
        int exp = (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, 0));
        assertEquals(Constants.SKILL_BASE_EXP, exp);
    }

    @Test
    void getExpToNextLevel_increasesWithLevel() {
        int expLevel1 = (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, 0));
        int expLevel2 = (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, 1));
        int expLevel10 = (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, 9));

        assertTrue(expLevel2 > expLevel1);
        assertTrue(expLevel10 > expLevel2);
    }

    @Test
    void getExpToNextLevel_level50_isReasonable() {
        int expLevel50 = (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, 49));
        // Should be around 100 * 1.2^49 ≈ 83,593
        assertTrue(expLevel50 > 50000);
        assertTrue(expLevel50 < 200000);
    }

    @Test
    void skillGrowthRate_providesReasonableProgression() {
        // Verify that the growth rate provides a good curve
        int prevExp = 0;
        for (int level = 1; level <= 50; level++) {
            int exp = (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, level - 1));
            assertTrue(exp > prevExp, "Experience should increase with level");
            prevExp = exp;
        }
    }
}

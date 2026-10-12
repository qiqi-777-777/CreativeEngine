package com.mingde.creativeengine.service.agent.tool;

import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.service.OccupationService;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 职业工具测试。
 */
class OccupationToolsTest {

    private static class StubOccupationService implements OccupationService {

        private List<Occupation> matches = Collections.emptyList();
        private RuntimeException failure;
        private List<String> lastMajors;
        private List<String> lastSkills;
        private List<String> lastPersonalities;
        private int matchCallCount;

        @Override
        public List<String> getAllMajors() {
            return List.of("计算机科学与技术", "软件工程");
        }

        @Override
        public List<String> getAllSkills() {
            return List.of("Java", "MySQL");
        }

        @Override
        public List<String> getAllPersonalities() {
            return List.of("研究型", "实践型");
        }

        @Override
        public List<Occupation> matchOccupations(List<String> majors, List<String> skills, List<String> personalities) {
            throw new AssertionError("工具不应调用会触发大模型补全的 matchOccupations");
        }

        @Override
        public List<Occupation> matchFromDatabase(List<String> majors, List<String> skills, List<String> personalities) {
            matchCallCount++;
            lastMajors = majors;
            lastSkills = skills;
            lastPersonalities = personalities;
            if (failure != null) {
                throw failure;
            }
            return matches;
        }
    }

    private static Occupation occupation(String name, String reason) {
        Occupation occupation = new Occupation();
        occupation.setName(name);
        occupation.setReason(reason);
        return occupation;
    }

    @Test
    void tagToolListsLibraryTagsSoModelCanMapFreeTextToStandardTags() {
        String result = new OccupationTools(new StubOccupationService()).getOccupationTags();

        assertTrue(result.contains("专业标签：计算机科学与技术、软件工程"), result);
        assertTrue(result.contains("技能标签：Java、MySQL"), result);
        assertTrue(result.contains("性格标签：研究型、实践型"), result);
    }

    @Test
    void matchToolSplitsMixedDelimitersIntoTags() {
        StubOccupationService service = new StubOccupationService();
        service.matches = List.of(occupation("后端开发工程师", "技能匹配"));

        String result = new OccupationTools(service).matchOccupations("计算机科学与技术、软件工程", "Java，MySQL", "研究型");

        assertEquals(List.of("计算机科学与技术", "软件工程"), service.lastMajors);
        assertEquals(List.of("Java", "MySQL"), service.lastSkills);
        assertEquals(List.of("研究型"), service.lastPersonalities);
        assertTrue(result.contains("后端开发工程师：技能匹配"), result);
    }

    @Test
    void jsonArrayStyleArgumentsAreAlsoAccepted() {
        StubOccupationService service = new StubOccupationService();
        service.matches = List.of(occupation("测试工程师", "细致"));

        new OccupationTools(service).matchOccupations("[\"软件工程\"]", null, null);

        assertEquals(List.of("软件工程"), service.lastMajors);
        assertTrue(service.lastSkills.isEmpty());
    }

    @Test
    void emptyTagsAskModelToConfirmWithUserInsteadOfQuerying() {
        StubOccupationService service = new StubOccupationService();

        String result = new OccupationTools(service).matchOccupations("  ", null, "");

        assertTrue(result.contains("请先向用户确认"), result);
        assertTrue(result.contains("getOccupationTags"), result);
        assertEquals(0, service.matchCallCount, "没有标签时不应查库");
    }

    @Test
    void noMatchTellsModelToRetryWithStandardTags() {
        StubOccupationService service = new StubOccupationService();
        service.matches = Collections.emptyList();

        String result = new OccupationTools(service).matchOccupations("考古学", null, null);

        assertTrue(result.contains("没有匹配到"), result);
        assertTrue(result.contains("getOccupationTags"), result);
    }

    @Test
    void matchLimitKeepsContextSmall() {
        StubOccupationService service = new StubOccupationService();
        List<Occupation> many = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            many.add(occupation("职业" + i, "理由" + i));
        }
        service.matches = many;

        String result = new OccupationTools(service).matchOccupations("计算机科学与技术", null, null);

        assertTrue(result.contains("职业5"), result);
        assertFalse(result.contains("职业6"), "最多只应返回 5 条：" + result);
    }

    @Test
    void failureIsReturnedAsReadableTextNotThrown() {
        StubOccupationService service = new StubOccupationService();
        service.failure = new IllegalStateException("db down");

        String result = assertDoesNotThrow(() -> new OccupationTools(service).matchOccupations("计算机科学与技术", null, null));

        assertTrue(result.contains("暂时不可用"), result);
        assertFalse(result.contains("db down"), "不应把底层异常细节抛给模型：" + result);
    }
}

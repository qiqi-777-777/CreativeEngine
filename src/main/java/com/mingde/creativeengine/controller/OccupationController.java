package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.service.OccupationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 职业匹配控制器
 *
 * @author CreativeEngine Team
 */
@RestController
@RequestMapping("/api/occupation")
public class OccupationController {

    @Autowired
    private OccupationService occupationService;

    /**
     * 获取所有专业列表
     */
    @GetMapping("/majors")
    public Result<List<String>> getAllMajors() {
        List<String> majors = occupationService.getAllMajors();
        return Result.success(majors);
    }

    /**
     * 获取所有技术列表
     */
    @GetMapping("/skills")
    public Result<List<String>> getAllSkills() {
        List<String> skills = occupationService.getAllSkills();
        return Result.success(skills);
    }

    /**
     * 获取所有性格类型列表
     */
    @GetMapping("/personalities")
    public Result<List<String>> getAllPersonalities() {
        List<String> personalities = occupationService.getAllPersonalities();
        return Result.success(personalities);
    }

    /**
     * 职业匹配
     */
    @PostMapping("/match")
    public Result<List<Occupation>> matchOccupations(@RequestBody Map<String, List<String>> request) {
        List<String> majors = request.get("majors");
        List<String> skills = request.get("skills");
        List<String> personalities = request.get("personalities");

        List<Occupation> matchedOccupations = occupationService.matchOccupations(majors, skills, personalities);
        return Result.success(matchedOccupations);
    }

}

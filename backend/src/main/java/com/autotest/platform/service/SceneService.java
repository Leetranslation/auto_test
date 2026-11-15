package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.TestScene;
import com.autotest.platform.entity.TestSceneVersion;
import com.autotest.platform.mapper.TestSceneMapper;
import com.autotest.platform.mapper.TestSceneVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SceneService {

    private final TestSceneMapper sceneMapper;
    private final TestSceneVersionMapper versionMapper;

    public Page<TestScene> page(long page, long pageSize, Long projectId, String keyword) {
        LambdaQueryWrapper<TestScene> qw = new LambdaQueryWrapper<>();
        qw.eq(TestScene::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(TestScene::getSceneName, keyword).or().like(TestScene::getSceneCode, keyword));
        }
        qw.orderByDesc(TestScene::getId);
        return sceneMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public TestScene create(TestScene scene) {
        if (!StringUtils.hasText(scene.getSceneName())) throw new BizException("场景名称不能为空");
        scene.setId(null);
        scene.setSceneCode(StringUtils.hasText(scene.getSceneCode()) ? scene.getSceneCode() : "SCENE_" + System.currentTimeMillis());
        scene.setStatus(StringUtils.hasText(scene.getStatus()) ? scene.getStatus() : "DRAFT");
        scene.setCurrentVersion(1);
        sceneMapper.insert(scene);

        TestSceneVersion v = new TestSceneVersion();
        v.setSceneId(scene.getId());
        v.setVersionNo(1);
        v.setGraphJson("{\"nodes\":[],\"edges\":[]}");
        v.setPublished(0);
        versionMapper.insert(v);
        return scene;
    }

    public TestScene update(TestScene scene) {
        if (sceneMapper.selectById(scene.getId()) == null) throw new BizException(404, "场景不存在");
        sceneMapper.updateById(scene);
        return sceneMapper.selectById(scene.getId());
    }

    public void delete(Long id) {
        sceneMapper.deleteById(id);
    }

    public void changeStatus(Long id, String status) {
        TestScene scene = sceneMapper.selectById(id);
        if (scene == null) throw new BizException(404, "场景不存在");
        scene.setStatus(status);
        sceneMapper.updateById(scene);
    }

    public List<TestSceneVersion> versionList(Long sceneId) {
        return versionMapper.selectList(new LambdaQueryWrapper<TestSceneVersion>()
                .eq(TestSceneVersion::getSceneId, sceneId)
                .orderByDesc(TestSceneVersion::getVersionNo));
    }

    /** 查询指定版本（或当前版本）的图 JSON */
    public String graph(Long sceneId, Integer versionNo) {
        TestScene scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw new BizException(404, "场景不存在");
        Integer v = versionNo == null ? scene.getCurrentVersion() : versionNo;
        TestSceneVersion version = versionMapper.selectOne(new LambdaQueryWrapper<TestSceneVersion>()
                .eq(TestSceneVersion::getSceneId, sceneId)
                .eq(TestSceneVersion::getVersionNo, v));
        if (version == null) throw new BizException(404, "版本不存在");
        return version.getGraphJson();
    }

    @Transactional
    public TestSceneVersion saveVersion(Long sceneId, String graphJson, boolean publish) {
        TestScene scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw new BizException(404, "场景不存在");
        Integer max = versionMapper.selectList(new LambdaQueryWrapper<TestSceneVersion>()
                        .eq(TestSceneVersion::getSceneId, sceneId))
                .stream().map(TestSceneVersion::getVersionNo).max(Integer::compareTo).orElse(0);
        TestSceneVersion v = new TestSceneVersion();
        v.setSceneId(sceneId);
        v.setVersionNo(max + 1);
        v.setGraphJson(graphJson);
        v.setPublished(publish ? 1 : 0);
        versionMapper.insert(v);
        scene.setCurrentVersion(v.getVersionNo());
        sceneMapper.updateById(scene);
        return v;
    }
}

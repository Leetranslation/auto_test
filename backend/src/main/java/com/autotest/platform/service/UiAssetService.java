package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.UiApplication;
import com.autotest.platform.entity.UiLocator;
import com.autotest.platform.entity.UiPageObject;
import com.autotest.platform.mapper.UiApplicationMapper;
import com.autotest.platform.mapper.UiLocatorMapper;
import com.autotest.platform.mapper.UiPageObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UiAssetService {

    private final UiApplicationMapper appMapper;
    private final UiPageObjectMapper pageMapper;
    private final UiLocatorMapper locatorMapper;

    // 应用
    public List<UiApplication> appList(Long projectId) {
        return appMapper.selectList(new LambdaQueryWrapper<UiApplication>()
                .eq(UiApplication::getProjectId, projectId).orderByDesc(UiApplication::getId));
    }

    public UiApplication appCreate(UiApplication app) {
        if (!StringUtils.hasText(app.getAppName())) throw new BizException("应用名称不能为空");
        app.setId(null);
        app.setBrowserType(StringUtils.hasText(app.getBrowserType()) ? app.getBrowserType() : "CHROMIUM");
        app.setDefaultTimeoutMs(app.getDefaultTimeoutMs() == null ? 10000 : app.getDefaultTimeoutMs());
        app.setStatus(app.getStatus() == null ? 1 : app.getStatus());
        appMapper.insert(app);
        return app;
    }

    public UiApplication appUpdate(UiApplication app) {
        if (appMapper.selectById(app.getId()) == null) throw new BizException(404, "应用不存在");
        appMapper.updateById(app);
        return appMapper.selectById(app.getId());
    }

    public void appDelete(Long id) {
        appMapper.deleteById(id);
    }

    // 页面对象
    public List<UiPageObject> pageList(Long projectId, Long applicationId) {
        LambdaQueryWrapper<UiPageObject> qw = new LambdaQueryWrapper<>();
        qw.eq(UiPageObject::getProjectId, projectId);
        if (applicationId != null) qw.eq(UiPageObject::getApplicationId, applicationId);
        qw.orderByDesc(UiPageObject::getId);
        return pageMapper.selectList(qw);
    }

    public UiPageObject pageCreate(UiPageObject page) {
        if (!StringUtils.hasText(page.getPageName())) throw new BizException("页面名称不能为空");
        page.setId(null);
        pageMapper.insert(page);
        return page;
    }

    public UiPageObject pageUpdate(UiPageObject page) {
        if (pageMapper.selectById(page.getId()) == null) throw new BizException(404, "页面对象不存在");
        pageMapper.updateById(page);
        return pageMapper.selectById(page.getId());
    }

    public void pageDelete(Long id) {
        pageMapper.deleteById(id);
    }

    // 定位器
    public List<UiLocator> locatorList(Long pageObjectId) {
        return locatorMapper.selectList(new LambdaQueryWrapper<UiLocator>()
                .eq(UiLocator::getPageObjectId, pageObjectId));
    }

    public UiLocator locatorSave(UiLocator locator) {
        if (!StringUtils.hasText(locator.getLocatorName())) throw new BizException("定位器名称不能为空");
        if (!StringUtils.hasText(locator.getLocatorValue())) throw new BizException("定位器值不能为空");
        UiLocator existing = locatorMapper.selectOne(new LambdaQueryWrapper<UiLocator>()
                .eq(UiLocator::getPageObjectId, locator.getPageObjectId())
                .eq(UiLocator::getLocatorName, locator.getLocatorName()));
        if (existing != null) {
            existing.setStrategy(locator.getStrategy());
            existing.setLocatorValue(locator.getLocatorValue());
            existing.setDescription(locator.getDescription());
            locatorMapper.updateById(existing);
            return existing;
        }
        locator.setId(null);
        locator.setStrategy(StringUtils.hasText(locator.getStrategy()) ? locator.getStrategy() : "DATA_TESTID");
        locatorMapper.insert(locator);
        return locator;
    }

    public void locatorDelete(Long id) {
        locatorMapper.deleteById(id);
    }
}
